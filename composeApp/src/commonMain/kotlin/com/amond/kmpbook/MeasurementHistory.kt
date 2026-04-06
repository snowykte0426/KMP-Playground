package com.amond.kmpbook

import androidx.compose.runtime.Composable

data class MeasurementHistoryEntry(
    val timestampMillis: Long,
    val chestCircumferenceCm: Int,
    val hipCircumferenceCm: Int,
    val koreanCupSize: String,
    val topSize: String,
    val bottomSize: String,
    val confidenceLabel: String,
)

interface MeasurementHistoryStore {
    fun load(): List<MeasurementHistoryEntry>
    fun save(entry: MeasurementHistoryEntry)
    fun clear()
}

@Composable
expect fun rememberMeasurementHistoryStore(): MeasurementHistoryStore
