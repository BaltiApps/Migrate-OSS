package balti.migrate.backup.data.sources.sms

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import balti.migrate.backup.data.model.SmsData
import balti.migrate.backup.data.utils.getDataBase
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
import baltiapps.migrate.domain.backup.sources.DBWriter
import java.io.File

class SmsDBWriter: DBWriter<SmsData> {

    private lateinit var sqLiteDatabase: SQLiteDatabase

    override fun setup(fileLocation: String) {
        val dbFile = File(fileLocation).apply {
            if (exists()) delete()
        }

        getDataBase(dbFile).run {
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

    override fun writeRow(dataItem: SmsData) {
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

    override fun close() {
        if (::sqLiteDatabase.isInitialized) {
            sqLiteDatabase.close()
        }
    }

}