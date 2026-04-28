package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

import java.util.*

data class MonthItem(
    val monthKey: String,
    val amount: Double
)

class MonthAdapter(private val items: List<MonthItem>) :
    RecyclerView.Adapter<MonthAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMonth: TextView = view.findViewById(R.id.tvMonth)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val viewColor: View = view.findViewById(R.id.viewColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_month, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        val month = item.monthKey.split("-")
            .getOrNull(1)
            ?.toIntOrNull() ?: 1

        val months = listOf(
            "Enero","Febrero","Marzo","Abril","Mayo","Junio",
            "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
        )

        holder.tvMonth.text = months.getOrNull(month - 1) ?: "Mes"
        holder.tvAmount.text = "$${String.format("%,.2f", item.amount)}"

        // Colores
        val color = when (position) {
            0 -> "#B91C1C"
            1 -> "#9CA3AF"
            else -> "#9CA3AF"
        }

        val drawable = holder.viewColor.background.mutate()
        drawable.setTint(android.graphics.Color.parseColor(color))
    }

    override fun getItemCount() = items.size
}
class AdminDashboardActivity : AppCompatActivity() {
    private val db by lazy { FirebaseFirestore.getInstance() }

    private var storeId: String = ""
    private var baseMonthKey: String = ""
    private var compMonthKey: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val name = intent.getStringExtra("name") ?: "Admin"

        findViewById<TextView>(R.id.tvHello).text = "Hola,\n$name\n(Admin)"

        // El ícono de la esquina ahora lleva al perfil en lugar de cerrar sesión
        findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        val bottom = findViewById<BottomNavigationView>(R.id.bottomNavAdmin)
        bottom.selectedItemId = R.id.nav_home

        val barChart = findViewById<BarChart>(R.id.barChart)

        val recycler = findViewById<RecyclerView>(R.id.rvLastMonths)
        recycler.layoutManager = LinearLayoutManager(this)

        StoreSession.getStoreId(
            onOk = { sid ->
                storeId = sid
                setupDefaultMonths()

                loadTotalsAndShow(barChart)
                loadRecentMonths(recycler)
            },
            onFail = { }
        )

        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_users -> {
                    startActivity(Intent(this, UsersActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_home -> true
                R.id.nav_report -> {
                    startActivity(Intent(this, AdminReportActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_products -> {
                    startActivity(Intent(this, ProductsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        findViewById<ImageButton>(R.id.fabAdmin)
            .setOnClickListener {
                startActivity(Intent(this, CreateUserActivity::class.java))
            }

    }


    private fun setupDefaultMonths() {
        val cal = Calendar.getInstance()

        baseMonthKey = String.format(
            Locale.getDefault(),
            "%04d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1
        )

        cal.add(Calendar.MONTH, -1)

        compMonthKey = String.format(
            Locale.getDefault(),
            "%04d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1
        )
    }

    private fun loadTotalsAndShow(chart: BarChart) {

        val monthsToQuery = listOf(baseMonthKey, compMonthKey)

        db.collection("stores").document(storeId)
            .collection("sales")
            .whereIn("monthKey", monthsToQuery)
            .get()
            .addOnSuccessListener { snap ->

                var totalBase = 0.0
                var totalComp = 0.0

                for (d in snap.documents) {
                    val mk = d.getString("monthKey") ?: continue
                    val amt = d.get("amount")?.toString()?.toDoubleOrNull() ?: 0.0

                    if (mk == baseMonthKey) totalBase += amt
                    if (mk == compMonthKey) totalComp += amt
                }

                setupBarChart(chart, totalBase, totalComp)

            }
            .addOnFailureListener {
                Toast.makeText(this, "Error cargando datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupBarChart(
        chart: BarChart,
        totalBase: Double,
        totalComp: Double
    ) {

        val green = android.graphics.Color.parseColor("#166534")
        val red = android.graphics.Color.parseColor("#B91C1C")
        val gray = android.graphics.Color.parseColor("#9CA3AF")

        val diff = totalBase - totalComp

        val colorActual = if (diff >= 0) green else red

        // 🔹 Cada barra es un dataset distinto
        val compEntry = BarEntry(0f, totalComp.toFloat())
        val baseEntry = BarEntry(1f, totalBase.toFloat())

        val dataSetComp = BarDataSet(listOf(compEntry), "Mes anterior")
        dataSetComp.color = gray

        val dataSetBase = BarDataSet(listOf(baseEntry), "Mes actual")
        dataSetBase.color = colorActual

        val data = BarData(dataSetComp, dataSetBase)
        data.barWidth = 0.4f

        chart.data = data

        chart.xAxis.valueFormatter = IndexAxisValueFormatter(
            listOf(
                monthKeyToShort(compMonthKey),
                monthKeyToShort(baseMonthKey)
            )
        )

        chart.xAxis.granularity = 1f
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false

        chart.legend.isEnabled = true

        chart.animateY(1000)
        chart.invalidate()
    }

    private fun monthKeyToShort(mk: String): String {
        val m = mk.split("-").getOrNull(1)?.toIntOrNull() ?: return mk
        return listOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")[m - 1]
    }

    private fun loadRecentMonths(recycler: RecyclerView) {

        db.collection("stores").document(storeId)
            .collection("sales")
            .get()
            .addOnSuccessListener { snap ->

                val map = mutableMapOf<String, Double>()

                for (d in snap.documents) {
                    val mk = d.getString("monthKey") ?: continue
                    val amt = d.get("amount")?.toString()?.toDoubleOrNull() ?: 0.0

                    map[mk] = (map[mk] ?: 0.0) + amt
                }

                val list = map.map { MonthItem(it.key, it.value) }
                    .sortedByDescending { it.monthKey }
                    .take(2)

                recycler.adapter = MonthAdapter(list)
            }
    }
}