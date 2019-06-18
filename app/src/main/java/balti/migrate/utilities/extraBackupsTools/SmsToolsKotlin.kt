package balti.migrate.utilities.extraBackupsTools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.Telephony
import android.support.v4.app.ActivityCompat
import balti.migrate.extraBackupsActivity.sms.SmsDataPacketKotlin

class SmsToolsKotlin(val context: Context) {

    var errorEncountered = ""

    fun getSmsInboxCursor(): Cursor? {
        var cursor: Cursor? = null
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                cursor = context.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, null, null, null, null)
            }
        } catch (e: Exception) { e.printStackTrace() }

        return cursor
    }

    fun getSmsOutboxCursor(): Cursor? {
        var cursor: Cursor? = null
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                cursor = context.contentResolver.query(Telephony.Sms.Outbox.CONTENT_URI, null, null, null, null)
            }
        } catch (e: Exception) { e.printStackTrace() }

        return cursor
    }

    fun getSmsSentCursor(): Cursor? {
        var cursor: Cursor? = null
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                cursor = context.contentResolver.query(Telephony.Sms.Sent.CONTENT_URI, null, null, null, null)
            }
        } catch (e: Exception) { e.printStackTrace() }

        return cursor
    }

    fun getSmsDraftCursor(): Cursor? {
        var cursor: Cursor? = null
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                cursor = context.contentResolver.query(Telephony.Sms.Draft.CONTENT_URI, null, null, null, null)
            }
        } catch (e: Exception) { e.printStackTrace() }

        return cursor
    }

    fun getCallsPacket(cursor: Cursor, selected: Boolean): SmsDataPacketKotlin {

        val getCursorData = GetCursorData(cursor)

        val sdp = SmsDataPacketKotlin(
                getCursorData.getString(Telephony.Sms.ADDRESS),
                getCursorData.getString(Telephony.Sms.BODY),
                getCursorData.getString(Telephony.Sms.DATE),
                getCursorData.getString(Telephony.Sms.DATE_SENT),
                getCursorData.getString(Telephony.Sms.TYPE),
                getCursorData.getString(Telephony.Sms.CREATOR),

                getCursorData.getString(Telephony.Sms.PERSON),
                getCursorData.getString(Telephony.Sms.PROTOCOL),
                getCursorData.getString(Telephony.Sms.SEEN),
                getCursorData.getString(Telephony.Sms.SERVICE_CENTER),
                getCursorData.getString(Telephony.Sms.STATUS),
                getCursorData.getString(Telephony.Sms.SUBJECT),
                getCursorData.getString(Telephony.Sms.THREAD_ID),

                getCursorData.getInt(Telephony.Sms.ERROR_CODE),
                getCursorData.getInt(Telephony.Sms.READ),
                getCursorData.getInt(Telephony.Sms.LOCKED),
                getCursorData.getInt(Telephony.Sms.REPLY_PATH_PRESENT),

                selected
        )

        errorEncountered = getCursorData.errorEncountered.trim()
        return sdp

    }
}