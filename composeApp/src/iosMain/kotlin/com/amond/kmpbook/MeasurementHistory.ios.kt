package com.amond.kmpbook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberMeasurementHistoryStore(): MeasurementHistoryStore {
    return remember {
        object : MeasurementHistoryStore {
            private val entries = mutableListOf<MeasurementHistoryEntry>()

            override fun load(): List<MeasurementHistoryEntry> = entries.toList()

            override fun save(entry: MeasurementHistoryEntry) {
                entries.add(0, entry)
                if (entries.size > 10) {
                    entries.removeLast()
                }
            }

            override fun clear() {
                entries.clear()
            }
        }
    }
}
