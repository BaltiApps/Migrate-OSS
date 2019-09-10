package balti.migrate.extraBackupsActivity.contacts.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.support.v4.app.ActivityCompat
import java.io.ByteArrayOutputStream
import java.io.InputStream

class VcfToolsKotlin(val context: Context) {

    var errorEncountered = ""

    fun getContactsCursor(): Cursor? {
        var cursor: Cursor? = null
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                cursor = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
            }
        } catch (e: Exception) { e.printStackTrace() }

        return cursor
    }

    fun getVcfData(cursor: Cursor): Array<String> {
        val lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY))
        var vcardstring: String
        var fullName: String
        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)
        val fd: AssetFileDescriptor
        try {
            fd = context.contentResolver.openAssetFileDescriptor(uri, "r")
            val fis = fd.createInputStream()
            val buf = readBytes(fis)
            fis.read(buf)
            vcardstring = String(buf)
            fullName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
        } catch (e: Exception) {
            e.printStackTrace()
            fullName = ""
            vcardstring = ""
            errorEncountered = e.message.toString()
        }

        return arrayOf(fullName, vcardstring)
    }

    private fun readBytes(stream: InputStream): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        var len: Int
        val buffer = ByteArray(2048)
        while (true) {
            len = stream.read(buffer)
            if (len != -1)
            byteArrayOutputStream.write(buffer, 0, len)
            else break
        }
        return byteArrayOutputStream.toByteArray()
    }

}