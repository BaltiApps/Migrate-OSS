package balti.migrate.backup.data.sources.sms

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import balti.migrate.common.data.model.SmsData
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.REDACTED
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_ADDRESS
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_BODY
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_DATE
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_DATE_SENT
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_ERROR_CODE
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_LOCKED
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_PERSON
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_PROTOCOL
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_READ
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_REPLY_PATH_PRESENT
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_SEEN
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_SERVICE_CENTER
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_STATUS
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_SUBJECT
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_TABLE_NAME
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_THREAD_ID
import baltiapps.migrate.domain.SmsDBConstant.Companion.SMS_TYPE
import baltiapps.migrate.domain.backup.sources.DataBackup
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.runCatchingWithProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class SmsDBWriter(
    private val dbUtils: DBUtils,
): DataBackup<SmsData> {

    private lateinit var sqLiteDatabase: SQLiteDatabase

    override fun setLocation(file: GenericFile) {
        super.setLocation(file)

        val dbFile = File(file.path).apply {
            if (exists()) delete()
        }

        dbUtils.getDataBase(dbFile).run {
            val sqlDropTable = "DROP TABLE IF EXISTS $SMS_TABLE_NAME"
            val sqlCreateTable = "CREATE TABLE $SMS_TABLE_NAME ( " +
                    "id INTEGER PRIMARY KEY" +
                    ", $SMS_ADDRESS TEXT" +
                    ", $SMS_BODY TEXT" +
                    ", $SMS_DATE BIGINT" +
                    ", $SMS_DATE_SENT BIGINT" +
                    ", $SMS_TYPE INTEGER" +
                    ", $SMS_PERSON TEXT" +
                    ", $SMS_PROTOCOL INTEGER" +
                    ", $SMS_SEEN TEXT" +
                    ", $SMS_SERVICE_CENTER TEXT" +
                    ", $SMS_STATUS INTEGER" +
                    ", $SMS_SUBJECT TEXT" +
                    ", $SMS_THREAD_ID INTEGER" +
                    ", $SMS_ERROR_CODE INTEGER" +
                    ", $SMS_READ TEXT" +
                    ", $SMS_LOCKED TEXT" +
                    ", $SMS_REPLY_PATH_PRESENT TEXT" +
                    ")"
            sqLiteDatabase = this
            execSQL(sqlDropTable)
            execSQL(sqlCreateTable)
        }
    }

    override fun backupDataItems(dataItems: List<SmsData>): Flow<Progress> {
        return flow {
            dataItems.forEachIndexed { index, item ->
                val progress = Progress(
                    itemId = item._id,
                    progressType = Progress.ProgressType.SMS_BACKUP,
                    percentage = getPercentage(index + 1, dataItems.size),
                    logs = "SMS (${index + 1}/${dataItems.size}) ${item.logInfo}",
                    logsForStorage = "SMS (${index + 1}/${dataItems.size}) $REDACTED",
                )
                runCatchingWithProgress(progress) {
                    writeRow(item)
                }.run { emit(this) }
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun writeRow(dataItem: SmsData) {
        val contentValues = ContentValues()
        Pair(contentValues, dataItem).let { (c, d) ->
            c.put("id", d._id)
            c.put(SMS_ADDRESS, d.smsAddress)
            c.put(SMS_BODY, d.smsBody)
            c.put(SMS_DATE, d.smsDate)
            c.put(SMS_DATE_SENT, d.smsDateSent)
            c.put(SMS_TYPE, d.smsType)
            c.put(SMS_PERSON, d.smsPerson)
            c.put(SMS_PROTOCOL, d.smsProtocol)
            c.put(SMS_SEEN, d.smsSeen)
            c.put(SMS_SERVICE_CENTER, d.smsServiceCenter)
            c.put(SMS_STATUS, d.smsStatus)
            c.put(SMS_SUBJECT, d.smsSubject)
            c.put(SMS_THREAD_ID, d.smsThreadID)
            c.put(SMS_ERROR_CODE, d.smsErrorCode)
            c.put(SMS_READ, d.smsRead)
            c.put(SMS_LOCKED, d.smsLocked)
            c.put(SMS_REPLY_PATH_PRESENT, d.smsReplyPathPresent)
        }
        sqLiteDatabase.insert(SMS_TABLE_NAME, null, contentValues)
    }

    override fun onBackupOver() {
        if (::sqLiteDatabase.isInitialized) {
            sqLiteDatabase.close()
        }
    }

}