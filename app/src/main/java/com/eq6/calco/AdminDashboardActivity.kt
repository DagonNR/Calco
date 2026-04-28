package com.eq6.calco

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

data class MonthItem(
    val monthKey: String,
    val amount: Double
)

class MonthAdapter(private val items: List<MonthItem>) :
    RecyclerView.Adapter<MonthAdapter.ViewHolder>() {

    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

        val parts = item.monthKey.split("-")
        val year = parts.getOrNull(0) ?: ""
        val monthIdx = parts.getOrNull(1)?.toIntOrNull()?.minus(1) ?: 0

        val monthName = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        ).getOrNull(monthIdx) ?: "Mes"

        holder.tvMonth.text = "$monthName $year"
        holder.tvAmount.text = moneyFmt.format(item.amount)

        val color = if (position == 0) "#7C3AED" else "#9CA3AF"
        holder.viewColor.background.mutate().setTint(android.graphics.Color.parseColor(color))
    }

    override fun getItemCount() = items.size
}

class AdminDashboardActivity : AppCompatActivity() {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    private var storeId: String = ""
    private var baseMonthKey: String = ""
    private var compMonthKey: String = ""

    private lateinit var tvTotalAmount: TextView
    private lateinit var tvMonth: TextView
    private lateinit var tvCompareDiff: TextView
    private lateinit var tvComparePct: TextView
    private lateinit var ivCompareArrow: ImageView
    private lateinit var cardCompareStats: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvMonth = findViewById(R.id.tvMonth)
        tvCompareDiff = findViewById(R.id.tvCompareDiff)
        tvComparePct = findViewById(R.id.tvComparePct)
        ivCompareArrow = findViewById(R.id.ivCompareArrow)
        cardCompareStats = findViewById(R.id.cardCompareStats)

        val name = intent.getStringExtra("name") ?: "Admin"
        findViewById<TextView>(R.id.tvHello).text = "Hola,\n$name\n(Admin)"

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
                loadDashboardData(barChart, recycler)
            },
            onFail = {
                Toast.makeText(this, "Error: Sesión no encontrada", Toast.LENGTH_SHORT).show()
            }
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

        findViewById<ImageButton>(R.id.fabAdmin).setOnClickListener {
            startActivity(Intent(this, CreateUserActivity::class.java))
        }
    }

    private fun setupDefaultMonths() {
        val cal = Calendar.getInstance()
        baseMonthKey = String.format(Locale.getDefault(), "%04d-%02d",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        
        cal.add(Calendar.MONTH, -1)
        compMonthKey = String.format(Locale.getDefault(), "%04d-%02d",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        
        tvMonth.text = monthKeyToFullLabel(baseMonthKey)
    }

    private fun loadDashboardData(chart: BarChart, recycler: RecyclerView) {
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

                val totalBase = map[baseMonthKey] ?: 0.0
                val totalComp = map[compMonthKey] ?: 0.0

                tvTotalAmount.text = moneyFmt.format(totalBase)
                updateComparisonCard(totalBase, totalComp)
                setupBarChart(chart, totalBase, totalComp)

                val recentList = map.map { MonthItem(it.key, it.value) }
                    .sortedByDescending { it.monthKey }
                    .take(5)
                recycler.adapter = MonthAdapter(recentList)
            }
    }

    private fun updateComparisonCard(base: Double, comp: Double) {
        val diff = base - comp
        val percent = if (comp == 0.0) {
            if (base > 0) 100.0 else 0.0
        } else (diff / comp) * 100.0

        tvCompareDiff.text = (if (diff >= 0) "+" else "") + moneyFmt.format(diff)
        tvComparePct.text = String.format(Locale.getDefault(), "%.1f%% vs mes anterior", percent)

        if (diff >= 0) {
            cardCompareStats.setCardBackgroundColor(android.graphics.Color.parseColor("#DCFCE7"))
            tvCompareDiff.setTextColor(android.graphics.Color.parseColor("#166534"))
            tvComparePct.setTextColor(android.graphics.Color.parseColor("#166534"))
            ivCompareArrow.setImageResource(R.drawable.arrow_down)
            ivCompareArrow.rotation = 180f
        } else {
            cardCompareStats.setCardBackgroundColor(android.graphics.Color.parseColor("#FEE2E2"))
            tvCompareDiff.setTextColor(android.graphics.Color.parseColor("#B91C1C"))
            tvComparePct.setTextColor(android.graphics.Color.parseColor("#B91C1C"))
            ivCompareArrow.setImageResource(R.drawable.arrow_down)
            ivCompareArrow.rotation = 0f
        }
    }

    private fun setupBarChart(chart: BarChart, totalBase: Double, totalComp: Double) {
        val gray = android.graphics.Color.parseColor("#9CA3AF")
        val purple = android.graphics.Color.parseColor("#7C3AED")

        val entriesComp = listOf(BarEntry(0f, totalComp.toFloat()))
        val entriesBase = listOf(BarEntry(1f, totalBase.toFloat()))

        val dsComp = BarDataSet(entriesComp, "Mes anterior").apply { color = gray }
        val dsBase = BarDataSet(entriesBase, "Mes actual").apply { color = purple }

        chart.data = BarData(dsComp, dsBase).apply { barWidth = 0.4f }
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf(monthKeyToShort(compMonthKey), monthKeyToShort(baseMonthKey)))
        chart.xAxis.granularity = 1f
        chart.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.animateY(1000)
        chart.invalidate()
    }

    private fun monthKeyToFullLabel(mk: String): String {
        val parts = mk.split("-")
        val year = parts.getOrNull(0) ?: ""
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return mk
        val name = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")[m - 1]
        return "$name $year"
    }

    private fun monthKeyToShort(mk: String): String {
        val m = mk.split("-").getOrNull(1)?.toIntOrNull() ?: return mk
        return listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")[m - 1]
    }
}
