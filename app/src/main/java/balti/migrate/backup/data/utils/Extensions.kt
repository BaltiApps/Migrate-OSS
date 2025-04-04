package balti.migrate.backup.data.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sdf by lazy {
    SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
}

fun convertToDisplayDate(date: Long): String {
    return sdf.format(Date(date))
}