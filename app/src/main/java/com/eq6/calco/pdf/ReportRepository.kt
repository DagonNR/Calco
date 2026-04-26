package com.eq6.calco.pdf

import com.google.firebase.firestore.FirebaseFirestore

class ReportRepository(private val db: FirebaseFirestore) {

    fun fetchReportData(
        storeId: String,
        baseMonthKey: String,
        compMonthKey: String,
        trendMonths: List<String>,
        onOk: (MonthTotals, List<TrendPoint>, List<TopProductRow>) -> Unit,
        onFail: (String) -> Unit
    ) {
        val storeRef = db.collection("stores").document(storeId)
        val salesCol = storeRef.collection("sales")

        val months2 = listOf(baseMonthKey, compMonthKey).distinct()

        salesCol.whereIn("monthKey", months2).get()
            .addOnSuccessListener { snap ->
                val baseSales = snap.documents.filter { it.getString("monthKey") == baseMonthKey }
                val compSales = snap.documents.filter { it.getString("monthKey") == compMonthKey }

                val baseTotal = baseSales.sumOf { it.getDouble("amount") ?: 0.0 }
                val compTotal = compSales.sumOf { it.getDouble("amount") ?: 0.0 }

                val totals = MonthTotals(baseMonthKey, compMonthKey, baseTotal, compTotal)

                salesCol.whereIn("monthKey", trendMonths.distinct()).get()
                    .addOnSuccessListener { trendSnap ->
                        val trendMap = mutableMapOf<String, Double>()
                        for (mk in trendMonths) trendMap[mk] = 0.0
                        for (d in trendSnap.documents) {
                            val mk = d.getString("monthKey") ?: continue
                            val amt = d.getDouble("amount") ?: 0.0
                            trendMap[mk] = (trendMap[mk] ?: 0.0) + amt
                        }
                        val trend = trendMonths.map { mk -> TrendPoint(mk, trendMap[mk] ?: 0.0) }

                        fetchTopProductsByMonth(
                            storeId = storeId,
                            baseSaleIds = baseSales.map { it.id },
                            compSaleIds = compSales.map { it.id },
                            onOk = { topRows ->
                                onOk(totals, trend, topRows)
                            },
                            onFail = onFail
                        )
                    }
                    .addOnFailureListener { e -> onFail("Error tendencia: ${e.message}") }
            }
            .addOnFailureListener { e -> onFail("Error ventas: ${e.message}") }
    }

    private fun fetchTopProductsByMonth(
        storeId: String,
        baseSaleIds: List<String>,
        compSaleIds: List<String>,
        onOk: (List<TopProductRow>) -> Unit,
        onFail: (String) -> Unit
    ) {
        val storeRef = db.collection("stores").document(storeId)

        val baseMap = mutableMapOf<String, Long>() // productName -> units
        val compMap = mutableMapOf<String, Long>()

        fun addItemsToMap(itemsSnap: com.google.firebase.firestore.QuerySnapshot, target: MutableMap<String, Long>) {
            for (d in itemsSnap.documents) {
                val name = d.getString("productName") ?: continue
                val qty = d.getLong("quantity") ?: 0L
                target[name] = (target[name] ?: 0L) + qty
            }
        }

        fun readItemsSequential(
            saleIds: List<String>,
            idx: Int,
            target: MutableMap<String, Long>,
            done: () -> Unit
        ) {
            if (idx >= saleIds.size) {
                done()
                return
            }
            val saleId = saleIds[idx]
            storeRef.collection("sales").document(saleId).collection("items").get()
                .addOnSuccessListener { itemsSnap ->
                    addItemsToMap(itemsSnap, target)
                    readItemsSequential(saleIds, idx + 1, target, done)
                }
                .addOnFailureListener { e ->
                    onFail("Error items ($saleId): ${e.message}")
                }
        }

        readItemsSequential(baseSaleIds, 0, baseMap) {
            readItemsSequential(compSaleIds, 0, compMap) {
                val allNames = (baseMap.keys + compMap.keys).toSet()

                val rows = allNames.map { name ->
                    TopProductRow(
                        productName = name,
                        baseUnits = baseMap[name] ?: 0L,
                        compUnits = compMap[name] ?: 0L
                    )
                }
                    .sortedByDescending { maxOf(it.baseUnits, it.compUnits) }
                    .take(8)

                onOk(rows)
            }
        }
    }
}