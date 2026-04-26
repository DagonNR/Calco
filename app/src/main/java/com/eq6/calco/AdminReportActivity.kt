package com.eq6.calco

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import com.eq6.calco.pdf.ReportPdfExporter
import com.eq6.calco.pdf.ReportRepository

data class MonthOption(val monthKey: String, val label: String) {
    override fun toString(): String = label
}

class AdminReportActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.US)

    private lateinit var tvTotalAmount: TextView
    private lateinit var tvBaseMonth: TextView

    private lateinit var tvBaseAmountCard: TextView
    private lateinit var tvCompAmountCard: TextView
    private lateinit var tvDiffAmount: TextView
    private lateinit var tvDiffPercent: TextView

    private lateinit var progressDonut: CircularProgressIndicator

    private var storeId: String = ""

    private var monthOptions: List<MonthOption> = emptyList()
    private var baseMonthKey: String = ""
    private var compMonthKey: String = ""

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_report)

        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvBaseMonth = findViewById(R.id.tvBaseMonth)

        tvBaseAmountCard = findViewById(R.id.tvBaseAmountCard)
        tvCompAmountCard = findViewById(R.id.tvCompAmountCard)
        tvDiffAmount = findViewById(R.id.tvDiffAmount)
        tvDiffPercent = findViewById(R.id.tvDiffPercent)

        progressDonut = findViewById(R.id.progressDonut)

        findViewById<View>(R.id.btnFilter).setOnClickListener { openFilterDialog() }

        findViewById<MaterialButton>(R.id.btnSalesList).setOnClickListener {
            Toast.makeText(this, "Pendiente: Lista de ventas", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnExportPdf).setOnClickListener {
            Toast.makeText(this, "Generando PDF...", Toast.LENGTH_SHORT).show()

            val repo = ReportRepository(FirebaseFirestore.getInstance())
            val exporter = ReportPdfExporter()
            val trendMonths = buildLastMonths(baseMonthKey, 6)

            fetchStoreName(
                storeId = storeId,
                onOk = { storeName ->

                    repo.fetchReportData(
                        storeId = storeId,
                        baseMonthKey = baseMonthKey,
                        compMonthKey = compMonthKey,
                        trendMonths = trendMonths,
                        onOk = { totals, trend, top ->
                            exporter.export(
                                context = this,
                                storeName = storeName,
                                totals = totals,
                                trend = trend,
                                top = top,
                                onOk = { uri ->
                                    Toast.makeText(this, "PDF guardado en Descargas", Toast.LENGTH_LONG).show()

                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(intent)
                                },
                                onFail = { msg ->
                                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onFail = { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
                    )

                },
                onFail = { msg ->
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                }
            )
        }

        setupBottomNav()

        StoreSession.getStoreId(
            onOk = { sid ->
                storeId = sid
                setupDefaultMonths()
                loadMonthOptionsThenRefresh()
            },
            onFail = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                finish()
            }
        )
    }

    private fun setupBottomNav() {
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNavAdmin)
        bottom.selectedItemId = R.id.nav_report
        bottom.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_users -> {
                    startActivity(Intent(this, UsersActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_home -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_report -> true
                R.id.nav_products -> {
                    startActivity(Intent(this, ProductsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDefaultMonths() {
        val cal = Calendar.getInstance()
        val current = String.format(Locale.getDefault(), "%04d-%02d",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1
        )

        cal.add(Calendar.MONTH, -1)
        val prev = String.format(Locale.getDefault(), "%04d-%02d",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1
        )

        baseMonthKey = current
        compMonthKey = prev
    }

    private fun loadMonthOptionsThenRefresh() {
        db.collection("stores").document(storeId)
            .collection("sales")
            .get()
            .addOnSuccessListener { snap ->
                val keys = snap.documents.mapNotNull { it.getString("monthKey") }.distinct().sortedDescending()

                val finalKeys = if (keys.isEmpty()) listOf(baseMonthKey, compMonthKey).distinct() else keys
                monthOptions = finalKeys.map { mk -> MonthOption(mk, monthKeyToLabel(mk)) }

                if (monthOptions.isNotEmpty()) {
                    if (monthOptions.none { it.monthKey == baseMonthKey }) baseMonthKey = monthOptions.first().monthKey
                    if (monthOptions.none { it.monthKey == compMonthKey }) compMonthKey =
                        if (monthOptions.size > 1) monthOptions[1].monthKey else monthOptions.first().monthKey
                }

                refreshReport()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error cargando meses: ${e.message}", Toast.LENGTH_LONG).show()
                refreshReport()
            }
    }

    private fun buildLastMonths(baseMonthKey: String, count: Int): List<String> {
        val parts = baseMonthKey.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()

        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, year)
        cal.set(java.util.Calendar.MONTH, month - 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)

        val months = mutableListOf<String>()
        for (i in 0 until count) {
            val mk = String.format(
                java.util.Locale.getDefault(),
                "%04d-%02d",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1
            )
            months.add(0, mk) // para que quede del más viejo al más nuevo
            cal.add(java.util.Calendar.MONTH, -1)
        }
        return months
    }

    private fun fetchStoreName(
        storeId: String,
        onOk: (String) -> Unit,
        onFail: (String) -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("stores")
            .document(storeId)
            .get()
            .addOnSuccessListener { doc ->
                val name = (doc.getString("name") ?: "").trim()
                onOk(if (name.isBlank()) "Tienda $storeId" else name)
            }
            .addOnFailureListener { e ->
                onFail(e.message ?: "Error leyendo tienda")
            }
    }

    private fun openFilterDialog() {
        if (monthOptions.isEmpty()) {
            Toast.makeText(this, "Aún no hay datos", Toast.LENGTH_SHORT).show()
            return
        }

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_report_filter, null)
        val spBase = view.findViewById<Spinner>(R.id.spBase)
        val spComp = view.findViewById<Spinner>(R.id.spComp)

        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, monthOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spBase.adapter = adapter
        spComp.adapter = adapter

        val baseIndex = monthOptions.indexOfFirst { it.monthKey == baseMonthKey }.coerceAtLeast(0)
        val compIndex = monthOptions.indexOfFirst { it.monthKey == compMonthKey }.coerceAtLeast(0)
        spBase.setSelection(baseIndex)
        spComp.setSelection(compIndex)

        AlertDialog.Builder(this)
            .setTitle("Filtro de meses")
            .setView(view)
            .setPositiveButton("Aplicar") { _, _ ->
                val newBase = (spBase.selectedItem as? MonthOption)?.monthKey ?: baseMonthKey
                val newComp = (spComp.selectedItem as? MonthOption)?.monthKey ?: compMonthKey

                baseMonthKey = newBase
                compMonthKey = newComp
                refreshReport()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun refreshReport() {
        val monthsToQuery = listOf(baseMonthKey, compMonthKey).distinct().filter { it.isNotEmpty() }

        if (monthsToQuery.isEmpty()) {
            updateUI(0.0, 0.0)
            return
        }

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
                updateUI(totalBase, totalComp)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error reporte: ${e.message}", Toast.LENGTH_LONG).show()
                updateUI(0.0, 0.0)
            }
    }

    private fun updateUI(totalBase: Double, totalComp: Double) {
        tvTotalAmount.text = moneyFmt.format(totalBase)
        tvBaseMonth.text = monthKeyToLabel(baseMonthKey)

        tvBaseAmountCard.text = "${monthKeyToShort(baseMonthKey)}\n${moneyFmt.format(totalBase)}"
        tvCompAmountCard.text = "${monthKeyToShort(compMonthKey)}\n${moneyFmt.format(totalComp)}"

        val diff = totalBase - totalComp
        val percent = if (totalComp == 0.0) {
            if (totalBase > 0) 100.0 else 0.0
        } else (diff / totalComp) * 100.0

        tvDiffAmount.text = (if (diff >= 0) "+" else "") + moneyFmt.format(diff)
        tvDiffPercent.text = String.format(Locale.getDefault(), "%.1f%%", percent)

        val red = android.graphics.Color.parseColor("#B91C1C")
        val green = android.graphics.Color.parseColor("#166534")
        val color = if (diff >= 0) green else red
        tvDiffAmount.setTextColor(color)
        tvDiffPercent.setTextColor(color)

        val sum = totalBase + totalComp
        val p = if (sum <= 0.0) 0 else ((totalBase / sum) * 100).toInt().coerceIn(0, 100)
        progressDonut.isIndeterminate = false
        progressDonut.progress = p
    }

    private fun monthKeyToLabel(mk: String): String {
        val parts = mk.split("-")
        if (parts.size != 2) return mk
        val year = parts[0]
        val m = parts[1].toIntOrNull() ?: return mk
        val month = when (m) {
            1 -> "Enero"
            2 -> "Febrero"
            3 -> "Marzo"
            4 -> "Abril"
            5 -> "Mayo"
            6 -> "Junio"
            7 -> "Julio"
            8 -> "Agosto"
            9 -> "Septiembre"
            10 -> "Octubre"
            11 -> "Noviembre"
            12 -> "Diciembre"
            else -> mk
        }
        return "$month $year"
    }

    private fun monthKeyToShort(mk: String): String {
        val parts = mk.split("-")
        if (parts.size != 2) return mk
        val m = parts[1].toIntOrNull() ?: return mk
        return when (m) {
            1 -> "Ene"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Abr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Ago"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dic"
            else -> mk
        }
    }
}
