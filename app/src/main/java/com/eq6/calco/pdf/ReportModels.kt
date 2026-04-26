package com.eq6.calco.pdf

data class MonthTotals(
    val baseMonthKey: String,
    val compMonthKey: String,
    val baseTotal: Double,
    val compTotal: Double
)

data class TrendPoint(
    val monthKey: String,
    val total: Double
)

data class TopProductRow(
    val productName: String,
    val baseUnits: Long,
    val compUnits: Long
)