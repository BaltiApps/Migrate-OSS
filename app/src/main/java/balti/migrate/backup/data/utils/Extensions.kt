package balti.migrate.backup.data.utils

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import baltiapps.migrate.domain.exceptions.ContentReadException
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sdf by lazy {
    SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
}

inline fun <reified T> getCursorData(
    cursor: Cursor,
    columnName: String,
): T {
    val columnIndex = cursor.getColumnIndex(columnName)
    if (columnIndex < 0)
        throw ContentReadException("Column $columnName index less than 0 - $columnIndex")
    return when(T::class) {
        String::class -> (cursor.getString(columnIndex) ?: "") as T
        Int::class -> cursor.getInt(columnIndex) as T
        Long::class -> cursor.getLong(columnIndex) as T
        Boolean::class -> (cursor.getInt(columnIndex) > 0) as T
        else -> throw IllegalStateException("Cursor read - Unknown data type - ${T::class}")
    }
}

fun convertToDisplayDate(date: Long): String {
    return sdf.format(Date(date))
}

fun getDataBase(dbFile: File): SQLiteDatabase {
    val location = dbFile.canonicalPath
    if (!dbFile.exists()) {
        dbFile.createNewFile()
    }
    return SQLiteDatabase.openDatabase(
        location,
        null,
        SQLiteDatabase.NO_LOCALIZED_COLLATORS or SQLiteDatabase.OPEN_READWRITE
    )
}