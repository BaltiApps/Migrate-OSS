package baltiapps.migrate.domain.common.utils

import java.util.Locale

object StringUtils {
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
