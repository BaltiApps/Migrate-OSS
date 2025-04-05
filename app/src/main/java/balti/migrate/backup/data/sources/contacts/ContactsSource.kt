package balti.migrate.backup.data.sources.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import balti.migrate.common.data.model.ContactData
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.exceptions.ContentReadException
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.exceptions.PermissionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ContactsSource(
    private val context: Context,
    private val dbUtils: DBUtils,
): DataSource<ContactData> {
    override fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun getData(onFinishedLoading: (List<ContactData>) -> Unit): Flow<Progress> {
        val dataList = mutableListOf<ContactData>()

        return flow {
            if (!checkPermission()) {
                throw PermissionException(
                    permissionName = Manifest.permission.READ_CONTACTS,
                    message = "Contacts permission not granted"
                )
            }

            val cursor = dbUtils.getCursor(context, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

            val contactCount = cursor.count
            if (contactCount == 0) return@flow
            cursor.moveToFirst()

            for (i in 0 until contactCount) {
                getSingleContact(cursor).run {
                    dataList.add(this)
                    emit(
                        Progress(
                            progressType = Progress.ProgressType.CONTACTS_READ,
                            percentage = getPercentage(i+1, contactCount),
                            logs = this.logInfo
                        )
                    )
                    cursor.moveToNext()
                }
            }

            cursor.close()
            onFinishedLoading(dataList)
        }.flowOn(Dispatchers.IO)
    }

    private fun getSingleContact(
        cursor: Cursor
    ): ContactData {
        dbUtils.run {
            val lookupKey = getCursorData<String>(cursor, ContactsContract.Contacts.LOOKUP_KEY)

            val fullName =
                getCursorData<String>(cursor, ContactsContract.Contacts.DISPLAY_NAME)
            val primaryContact =
                getCursorData<String>(cursor, ContactsContract.CommonDataKinds.Phone.NUMBER)

            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)
            val fd: AssetFileDescriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
                ?: throw ContentReadException("Contacts - AssetFileDescriptor is null")

            val byteArray = fd.use {
                it.createInputStream().use {
                    it.readBytes()
                }
            }
            val vcardString = String(byteArray)

            return ContactData(
                _id = getCursorData<String>(cursor, ContactsContract.Contacts._ID),
                displayName = fullName,
                displayNumber = primaryContact,
                vcfContent = vcardString,
                logInfo = fullName.ifBlank { primaryContact },
            )
        }
    }
}