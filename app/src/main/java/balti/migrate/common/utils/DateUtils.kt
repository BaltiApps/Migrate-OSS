package balti.migrate.common.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sdf by lazy {
    SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
}

fun convertToDisplayDate(date: Long): String {
    return sdf.format(Date(date))
}

fun getDefaultBackupName(): String {
    val format = "dd-MMM-yyyy_hh-mm-ss-a"
    val sdf = SimpleDateFormat(format, Locale.getDefault())
    return sdf.format(Date().time)
}