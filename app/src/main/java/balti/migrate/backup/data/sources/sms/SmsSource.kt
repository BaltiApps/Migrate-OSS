package balti.migrate.backup.data.sources.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.Telephony
import androidx.core.content.ContextCompat
import balti.migrate.backup.data.model.SmsData
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.exceptions.PermissionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SmsSource(
    private val context: Context,
    private val dbUtils: DBUtils,
): DataSource<SmsData> {
    override fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun getData(onFinishedLoading: (List<SmsData>) -> Unit): Flow<Progress> {
        val dataList = mutableListOf<SmsData>()

        return flow {
            if (!checkPermission()) {
                throw PermissionException(
                    permissionName = Manifest.permission.READ_SMS,
                    message = "SMS Permission not granted"
                )
            }

            val cursor = dbUtils.getCursor(context, Telephony.Sms.CONTENT_URI)

            val totalCount = cursor.count
            if (totalCount == 0) return@flow
            cursor.moveToFirst()

            for (i in 0 until totalCount) {
                getSingleSms(cursor).run {
                    dataList.add(this)
                    emit(
                        Progress(
                            progressType = Progress.ProgressType.SMS_READ,
                            percentage = getPercentage(i+1, totalCount),
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

    private fun getSingleSms(cursor: Cursor): SmsData {
        dbUtils.run {
            val smsAddress = getCursorData<String>(cursor, Telephony.Sms.ADDRESS)
            return SmsData(
                _id = getCursorData<String>(cursor, Telephony.Sms._ID),
                logInfo = smsAddress,

                smsAddress = smsAddress,
                smsBody = getCursorData<String>(cursor, Telephony.Sms.BODY),
                smsDate = getCursorData<Long>(cursor, Telephony.Sms.DATE),
                smsDateSent = getCursorData<Long>(cursor, Telephony.Sms.DATE_SENT),
                smsType = getCursorData<Int>(cursor, Telephony.Sms.TYPE),
                smsPerson = getCursorData<String>(cursor, Telephony.Sms.PERSON),
                smsProtocol = getCursorData<Int>(cursor, Telephony.Sms.PROTOCOL),
                smsSeen = getCursorData<Boolean>(cursor, Telephony.Sms.SEEN),
                smsServiceCenter = getCursorData<String>(cursor, Telephony.Sms.SERVICE_CENTER),
                smsStatus = getCursorData<Int>(cursor, Telephony.Sms.STATUS),
                smsSubject = getCursorData<String>(cursor, Telephony.Sms.SUBJECT),
                smsThreadID = getCursorData<Int>(cursor, Telephony.Sms.THREAD_ID),
                smsErrorCode = getCursorData<Int>(cursor, Telephony.Sms.ERROR_CODE),
                smsRead = getCursorData<Boolean>(cursor, Telephony.Sms.READ),
                smsLocked = getCursorData<Boolean>(cursor, Telephony.Sms.LOCKED),
                smsReplyPathPresent = getCursorData<Boolean>(cursor, Telephony.Sms.REPLY_PATH_PRESENT),
            )
        }
    }
}