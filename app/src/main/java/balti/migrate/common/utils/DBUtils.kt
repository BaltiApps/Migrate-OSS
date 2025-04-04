package balti.migrate.common.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import baltiapps.migrate.domain.exceptions.ContentReadException
import baltiapps.migrate.domain.exceptions.UnknownDataTypeException
import java.io.File

class DBUtils {
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
            else -> throw UnknownDataTypeException(
                type = T::class,
                message = "Cursor read - Unknown data type - ${T::class}"
            )
        }
    }

    inline fun <reified T> putContentData(
        contentValues: ContentValues,
        columnName: String,
        data: T,
    ) {
        when(T::class) {
            String::class -> contentValues.put(columnName, data as String)
            Int::class -> contentValues.put(columnName, data as Int)
            Long::class -> contentValues.put(columnName, data as Long)
            Boolean::class -> contentValues.put(columnName, if (data as Boolean) 1 else 0)
            else -> throw UnknownDataTypeException(
                type = T::class,
                message = "Enter content values - Unknown data type - ${T::class}"
            )
        }
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

    fun getCursor(
        context: Context,
        uri: Uri,
    ): Cursor {
        return context.contentResolver.query(
            uri,
            null,
            null,
            null,
            null,
        ) ?: throw ContentReadException("Read cursor is null for Uri - $uri")
    }

    fun getCursor(
        db: SQLiteDatabase,
        tableName: String,
    ): Cursor {
        return db.query(
            tableName,
            null,
            null,
            null,
            null,
            null,
            null,
        )
    }
}