package balti.migrate.restore.data.sources.contacts

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import balti.migrate.common.data.model.ContactData
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.ContactsDBConstants
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.DBReader
import baltiapps.migrate.domain.exceptions.ContentReadException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class ContactsDBReader(
    private val dbUtils: DBUtils,
): DBReader<ContactData> {

    private lateinit var sqLiteDatabase: SQLiteDatabase

    override fun setup(file: GenericFile) {
        val dbFile = File(file.path).apply {
            if (!canRead()) throw ContentReadException("Cannot read contact DB file - ${file.path}")
        }

        sqLiteDatabase = dbUtils.getDataBase(dbFile)
    }

    override fun readRows(onFinished: (List<ContactData>) -> Unit): Flow<Progress> {
        val dataList = mutableListOf<ContactData>()

        return flow {
            val cursor = dbUtils.getCursor(sqLiteDatabase, ContactsDBConstants.CONTACTS_TABLE_NAME)

            val totalCount = cursor.count
            if (totalCount == 0) return@flow
            cursor.moveToFirst()

            for (i in 0 until totalCount) {
                getSingleContact(cursor).run {
                    dataList.add(this)
                    emit(
                        Progress(
                            progressType = Progress.ProgressType.CONTACTS_BACKUP_READ,
                            percentage = getPercentage(i+1, totalCount),
                            logs = this.logInfo
                        )
                    )
                    cursor.moveToNext()
                }
            }
            cursor.close()
            onFinished(dataList)
        }.flowOn(Dispatchers.IO)
    }

    private fun getSingleContact(
        cursor: Cursor,
    ): ContactData {

        dbUtils.run {
            val displayName = getCursorData<String>(cursor, ContactsDBConstants.DISPLAY_NAME)

            return ContactData(
                _id = getCursorData<String>(cursor, "id"),
                displayName = displayName,
                vcfContent = getCursorData(cursor, ContactsDBConstants.VCF_CONTENT),
                isLocalContact = true,
                logInfo = displayName,
            )
        }
    }

    override fun close() {
        if (::sqLiteDatabase.isInitialized) {
            sqLiteDatabase.close()
        }
    }
}