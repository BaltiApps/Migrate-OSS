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
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.exceptions.ContentReadException
import baltiapps.migrate.domain.exceptions.PermissionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ContactsSource(
    private val context: Context,
    private val dbUtils: DBUtils,
): DataSource<ContactData> {
    override suspend fun checkPermission(): Boolean {
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

            val cursor = dbUtils.getCursor(context, ContactsContract.Contacts.CONTENT_URI)

            val contactCount = cursor.count
            if (contactCount == 0) {
                cursor.close()
                return@flow
            }
            cursor.moveToFirst()

            cursor.use {
                for (i in 0 until contactCount) {
                    getSingleContact(cursor).run {
                        dataList.add(this)
                        emit(
                            Progress(
                                itemId = this._id,
                                progressType = Progress.ProgressType.CONTACTS_READ,
                                percentage = getPercentage(i+1, contactCount),
                                logs = this.logInfo
                            )
                        )
                        cursor.moveToNext()
                    }
                }
            }

            onFinishedLoading(dataList)
        }.flowOn(Dispatchers.IO)
    }

    private fun getSingleContact(
        cursor: Cursor
    ): ContactData {
        dbUtils.run {
            val contactId = getCursorData<String>(cursor, ContactsContract.Contacts._ID)
            val lookupKey = getCursorData<String>(cursor, ContactsContract.Contacts.LOOKUP_KEY)

            val fullName =
                getCursorData<String>(cursor, ContactsContract.Contacts.DISPLAY_NAME)

            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)
            val fd: AssetFileDescriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
                ?: throw ContentReadException("Contacts - AssetFileDescriptor is null")

            val byteArray = fd.use {
                it.createInputStream().use {
                    it.readBytes()
                }
            }
            val vcardString = String(byteArray)

            val accountType = getContactAccountType(contactId)

            return ContactData(
                _id = contactId,
                displayName = fullName,
                vcfContent = vcardString,
                isLocalContact = accountType.isBlank(),
                logInfo = fullName,
            )
        }
    }

    private fun getContactAccountType(contactId: String): String {
        val cursor = context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.ACCOUNT_TYPE),
            "${ContactsContract.RawContacts.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )

        cursor.use { c ->
            return if (c?.moveToFirst() == true) {
                dbUtils.getCursorData<String>(c, ContactsContract.RawContacts.ACCOUNT_TYPE)
            } else ""
        }
    }

}