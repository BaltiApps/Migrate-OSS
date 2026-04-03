package balti.migrate.restore.data.sources.sms

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony
import balti.migrate.common.data.model.SmsData
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.PermissionConstants
import baltiapps.migrate.domain.REDACTED
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.restore.sources.RestoreEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SmsRestoreEngine(
    private val context: Context,
    private val dbUtils: DBUtils,
    private val contextSource: ContextSource,
): RestoreEngine<SmsData> {
    override fun checkPermission(): Boolean {
        return contextSource.checkPermission(PermissionConstants.DEFAULT_SMS_APP)
    }

    override fun restoreDataItems(items: List<SmsData>): Flow<Progress> {
         return flow {
            if (!checkPermission()) return@flow
            items.forEachIndexed { index, item ->
                restoreSingleItem(item)
                emit(
                    Progress(
                        itemId = item._id,
                        progressType = Progress.ProgressType.SMS_RESTORE,
                        percentage = getPercentage(index + 1, items.size),
                        logs = "SMS (${index + 1}/${items.size}) ${item.logInfo}",
                        logsForStorage = "SMS (${index + 1}/${items.size}) $REDACTED",
                    )
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    private fun restoreSingleItem(dataItem: SmsData) {
        val cv = ContentValues()
        dataItem.run {
            dbUtils.putContentData(cv, Telephony.Sms.ADDRESS, smsAddress)
            dbUtils.putContentData(cv, Telephony.Sms.BODY, smsBody)
            dbUtils.putContentData(cv, Telephony.Sms.DATE, smsDate)
            dbUtils.putContentData(cv, Telephony.Sms.DATE_SENT, smsDateSent)
            dbUtils.putContentData(cv, Telephony.Sms.TYPE, smsType)
            dbUtils.putContentData(cv, Telephony.Sms.PERSON, smsPerson)
            dbUtils.putContentData(cv, Telephony.Sms.PROTOCOL, smsProtocol)
            dbUtils.putContentData(cv, Telephony.Sms.SEEN, smsSeen)
            dbUtils.putContentData(cv, Telephony.Sms.SERVICE_CENTER, smsServiceCenter)
            dbUtils.putContentData(cv, Telephony.Sms.STATUS, smsStatus)
            dbUtils.putContentData(cv, Telephony.Sms.SUBJECT, smsSubject)
            dbUtils.putContentData(cv, Telephony.Sms.THREAD_ID, smsThreadID)
            dbUtils.putContentData(cv, Telephony.Sms.ERROR_CODE, smsErrorCode)
            dbUtils.putContentData(cv, Telephony.Sms.READ, smsRead)
            dbUtils.putContentData(cv, Telephony.Sms.LOCKED, smsLocked)
            dbUtils.putContentData(cv, Telephony.Sms.REPLY_PATH_PRESENT, smsReplyPathPresent)
        }
        context.contentResolver.insert(Telephony.Sms.CONTENT_URI, cv)
    }
}