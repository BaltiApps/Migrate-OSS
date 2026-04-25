package baltiapps.migrate.domain.common.utils

import java.util.Locale

object StringUtils {
    fun getHumanReadableTimeDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun getHumanReadableSize(sizeInBytes: Long): String {
        val bytes = sizeInBytes.toDouble()
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            sizeInBytes < kb -> "$sizeInBytes B"
            sizeInBytes < mb -> String.format(Locale.US, "%.2f KB", bytes / kb)
            sizeInBytes < gb -> String.format(Locale.US, "%.2f MB", bytes / mb)
            else -> String.format(Locale.US, "%.2f GB", bytes / gb)
        }
    }
}
