package balti.migrate.utilities.extraBackupsTools

import android.database.Cursor

class GetCursorData(private val cursor: Cursor) {

    var errorEncountered = ""

    fun getString(columnName: String): String {
        return try { cursor.getString(cursor.getColumnIndex(columnName)) }
        catch (e: Exception) {
            errorEncountered += e.message.toString() + "\n"
            ""
        }
    }

    fun getLong(columnName: String): Long {
        return try { cursor.getLong(cursor.getColumnIndex(columnName)) }
        catch (e: Exception) {
            errorEncountered += e.message.toString() + "\n"
            0L
        }
    }

    fun getInt(columnName: String): Int {
        return try { cursor.getInt(cursor.getColumnIndex(columnName)) }
        catch (e: Exception) {
            errorEncountered += e.message.toString() + "\n"
            0
        }
    }
}