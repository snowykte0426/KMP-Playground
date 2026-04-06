package com.amond.kmpbook

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val PrefName = "measurement_history"
private const val KeyHistory = "entries"
private const val MaxEntries = 10

@Composable
actual fun rememberMeasurementHistoryStore(): MeasurementHistoryStore {
    val context = LocalContext.current
    return remember(context) {
        AndroidMeasurementHistoryStore(context.applicationContext)
    }
}

private class AndroidMeasurementHistoryStore(
    context: Context,
) : MeasurementHistoryStore {
    private val prefs = context.getSharedPreferences(PrefName, Context.MODE_PRIVATE)

    override fun load(): List<MeasurementHistoryEntry> {
        val raw = prefs.getString(KeyHistory, null) ?: return emptyList()
        return raw.split("\n")
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size != 7) return@mapNotNull null
                MeasurementHistoryEntry(
                    timestampMillis = parts[0].toLongOrNull() ?: return@mapNotNull null,
                    chestCircumferenceCm = parts[1].toIntOrNull() ?: return@mapNotNull null,
                    hipCircumferenceCm = parts[2].toIntOrNull() ?: return@mapNotNull null,
                    koreanCupSize = parts[3],
                    topSize = parts[4],
                    bottomSize = parts[5],
                    confidenceLabel = parts[6],
                )
            }
    }

    override fun save(entry: MeasurementHistoryEntry) {
        val updated = (listOf(entry) + load()).take(MaxEntries)
        prefs.edit().putString(
            KeyHistory,
            updated.joinToString("\n") {
                listOf(
                    it.timestampMillis,
                    it.chestCircumferenceCm,
                    it.hipCircumferenceCm,
                    it.koreanCupSize,
                    it.topSize,
                    it.bottomSize,
                    it.confidenceLabel,
                ).joinToString("|")
            },
        ).apply()
    }

    override fun clear() {
        prefs.edit().remove(KeyHistory).apply()
    }
}
