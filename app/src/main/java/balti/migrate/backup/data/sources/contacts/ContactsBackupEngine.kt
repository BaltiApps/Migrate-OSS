package balti.migrate.backup.data.sources.contacts

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import balti.migrate.common.data.model.ContactData
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.ContactsDBConstants.Companion.CONTACTS_TABLE_NAME
import baltiapps.migrate.domain.ContactsDBConstants.Companion.DISPLAY_NAME
import baltiapps.migrate.domain.ContactsDBConstants.Companion.VCF_CONTENT
import baltiapps.migrate.domain.REDACTED
import baltiapps.migrate.domain.backup.sources.BackupEngine
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.runCatchingWithProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class ContactsBackupEngine(
    private val dbUtils: DBUtils,
): BackupEngine<ContactData> {

    private lateinit var sqLiteDatabase: SQLiteDatabase

    override fun setLocation(file: GenericFile) {
        super.setLocation(file)

        val dbFile = File(file.path).apply {
            if (exists()) delete()
        }

        dbUtils.getDataBase(dbFile).apply {
            val sqlDropTable = "DROP TABLE IF EXISTS $CONTACTS_TABLE_NAME"
            val sqlCreateTable = "CREATE TABLE $CONTACTS_TABLE_NAME ( " +
                    "id INTEGER PRIMARY KEY" +
                    ", $DISPLAY_NAME TEXT" +
                    ", $VCF_CONTENT TEXT" +
                    ")"
            sqLiteDatabase = this
            execSQL(sqlDropTable)
            execSQL(sqlCreateTable)
        }
    }

    override fun backupDataItems(dataItems: List<ContactData>): Flow<Progress> {
        return flow {
            dataItems.forEachIndexed { index, item ->
                val progress = Progress(
                    itemId = item._id,
                    progressType = Progress.ProgressType.CONTACTS_BACKUP,
                    percentage = getPercentage(index + 1, dataItems.size),
                    logs = "CONTACT (${index + 1}/${dataItems.size}) ${item.logInfo}",
                    logsForStorage = "CONTACT (${index + 1}/${dataItems.size}) $REDACTED",
                )
                runCatchingWithProgress(progress) {
                    writeRow(item)
                }.run { emit(this) }
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun writeRow(dataItem: ContactData) {
        val contentValues = ContentValues()
        Pair(contentValues, dataItem).let { (c, d) ->
            c.put("id", d._id)
            c.put(DISPLAY_NAME, d.displayName)
            c.put(VCF_CONTENT, d.vcfContent)
        }
        sqLiteDatabase.insert(CONTACTS_TABLE_NAME, null, contentValues)
    }

    override fun onBackupOver() {
        if (::sqLiteDatabase.isInitialized) {
            sqLiteDatabase.close()
        }
    }
}