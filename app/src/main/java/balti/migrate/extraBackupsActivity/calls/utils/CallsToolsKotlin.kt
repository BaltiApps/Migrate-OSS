package balti.migrate.extraBackupsActivity.calls.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import android.support.v4.app.ActivityCompat
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.utils.GetCursorData


class CallsToolsKotlin(val context: Context) {

    var errorEncountered = ""

    fun getCallsCursor(): Cursor? {
        var cursor: Cursor? = null
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
                cursor = context.contentResolver.query(CallLog.Calls.CONTENT_URI, null, null, null, null)
            }
        } catch (e: Exception) { e.printStackTrace() }

        return cursor
    }

    fun getCallsPacket(cursor: Cursor, selected: Boolean): CallsDataPacketsKotlin {

        val getCursorData = GetCursorData(cursor)

        val cdp = CallsDataPacketsKotlin(
                getCursorData.getString(CallLog.Calls.CACHED_FORMATTED_NUMBER),
                getCursorData.getString(CallLog.Calls.CACHED_LOOKUP_URI),
                getCursorData.getString(CallLog.Calls.CACHED_MATCHED_NUMBER),
                getCursorData.getString(CallLog.Calls.CACHED_NAME),
                getCursorData.getString(CallLog.Calls.CACHED_NORMALIZED_NUMBER),
                getCursorData.getString(CallLog.Calls.CACHED_NUMBER_LABEL),
                getCursorData.getString(CallLog.Calls.CACHED_NUMBER_TYPE),
                getCursorData.getString(CallLog.Calls.CACHED_PHOTO_ID),
                getCursorData.getString(CallLog.Calls.COUNTRY_ISO),
                getCursorData.getString(CallLog.Calls.DATA_USAGE),
                getCursorData.getString(CallLog.Calls.FEATURES),
                getCursorData.getString(CallLog.Calls.GEOCODED_LOCATION),
                getCursorData.getString(CallLog.Calls.IS_READ),
                getCursorData.getString(CallLog.Calls.NUMBER),
                getCursorData.getString(CallLog.Calls.NUMBER_PRESENTATION),
                getCursorData.getString(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME),
                getCursorData.getString(CallLog.Calls.PHONE_ACCOUNT_ID),
                getCursorData.getString(CallLog.Calls.TRANSCRIPTION),
                getCursorData.getString(CallLog.Calls.TYPE),
                getCursorData.getString(CallLog.Calls.VOICEMAIL_URI),

                getCursorData.getLong(CallLog.Calls.DATE),
                getCursorData.getLong(CallLog.Calls.DURATION),
                getCursorData.getLong(CallLog.Calls.NEW),

                selected
        )

        errorEncountered = getCursorData.errorEncountered.trim()
        return cdp

    }
}