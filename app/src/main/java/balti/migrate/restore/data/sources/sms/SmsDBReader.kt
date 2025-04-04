package balti.migrate.restore.data.sources.sms

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import balti.migrate.backup.data.model.SmsData
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.SmsDBConstant
import baltiapps.migrate.domain.backup.getPercentage
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.DBReader
import baltiapps.migrate.domain.exceptions.ContentReadException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class SmsDBReader(
    private val dbUtils: DBUtils,
): DBReader<SmsData> {

    private lateinit var sqLiteDatabase: SQLiteDatabase

    override fun setup(fileLocation: String) {
        val dbFile = File(fileLocation).apply {
            if (!canRead()) throw ContentReadException("Cannot read SMS DB file - $fileLocation")
        }

        sqLiteDatabase = dbUtils.getDataBase(dbFile)
    }

    override fun readRows(onFinished: (List<SmsData>) -> Unit): Flow<Progress> {
        val dataList = mutableListOf<SmsData>()

        return flow {
            val cursor = dbUtils.getCursor(sqLiteDatabase, SmsDBConstant.SMS_TABLE_NAME)

            val totalCount = cursor.count
            if (totalCount == 0) return@flow
            cursor.moveToFirst()

            for (i in 0 until totalCount) {
                getSingleSms(cursor).run {
                    dataList.add(this)
                    emit(
                        Progress(
                            progressType = Progress.ProgressType.SMS_BACKUP_READ,
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

    private fun getSingleSms(
        cursor: Cursor,
    ): SmsData {

        dbUtils.run {
            val smsAddress = getCursorData<String>(cursor, SmsDBConstant.SMS_ADDRESS)
            return SmsData(
                _id = getCursorData<String>(cursor, "id"),
                logInfo = smsAddress,

                smsAddress = smsAddress,
                smsBody = getCursorData<String>(cursor, SmsDBConstant.SMS_BODY),
                smsDate = getCursorData<Long>(cursor, SmsDBConstant.SMS_DATE),
                smsDateSent = getCursorData<Long>(cursor, SmsDBConstant.SMS_DATE_SENT),
                smsType = getCursorData<Int>(cursor, SmsDBConstant.SMS_TYPE),
                smsPerson = getCursorData<String>(cursor, SmsDBConstant.SMS_PERSON),
                smsProtocol = getCursorData<Int>(cursor, SmsDBConstant.SMS_PROTOCOL),
                smsSeen = getCursorData<Boolean>(cursor, SmsDBConstant.SMS_SEEN),
                smsServiceCenter = getCursorData<String>(cursor, SmsDBConstant.SMS_SERVICE_CENTER),
                smsStatus = getCursorData<Int>(cursor, SmsDBConstant.SMS_STATUS),
                smsSubject = getCursorData<String>(cursor, SmsDBConstant.SMS_SUBJECT),
                smsThreadID = getCursorData<Int>(cursor, SmsDBConstant.SMS_THREAD_ID),
                smsErrorCode = getCursorData<Int>(cursor, SmsDBConstant.SMS_ERROR_CODE),
                smsRead = getCursorData<Boolean>(cursor, SmsDBConstant.SMS_READ),
                smsLocked = getCursorData<Boolean>(cursor, SmsDBConstant.SMS_LOCKED),
                smsReplyPathPresent = getCursorData<Boolean>(cursor, SmsDBConstant.SMS_REPLY_PATH_PRESENT),
            )
        }
    }

    override fun close() {
        if (::sqLiteDatabase.isInitialized) {
            sqLiteDatabase.close()
        }
    }
}