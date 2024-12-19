package balti.migrate.backup.ui.screens.listScreen

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getDefaultBackupName(): String {
    val format = "dd-MMM-yyyy_hh-mm-ss-a"
    val sdf = SimpleDateFormat(format, Locale.getDefault())
    return sdf.format(Date().time)
}