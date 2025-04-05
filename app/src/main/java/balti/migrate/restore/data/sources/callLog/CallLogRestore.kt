package balti.migrate.restore.data.sources.callLog

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import balti.migrate.common.model.CallLogData
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.restore.sources.DataRestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CallLogRestore(
    private val context: Context,
    private val dbUtils: DBUtils,
): DataRestore<CallLogData> {
    override fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun restoreDataItems(items: List<CallLogData>): Flow<Progress> {
        return flow {
            if (!checkPermission()) return@flow
            items.forEachIndexed { index, item ->
                restoreSingleItem(item)
                emit(
                    Progress(
                        progressType = Progress.ProgressType.CALL_LOG_RESTORE,
                        percentage = getPercentage(index + 1, items.size),
                        logs = "(${index + 1}/${items.size}) ${item.logInfo}"
                    )
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    private fun restoreSingleItem(dataItem: CallLogData) {
        val cv = ContentValues()
        dataItem.run {
            dbUtils.putContentData(cv, CallLog.Calls.COUNTRY_ISO, callsCountryIso)
            dbUtils.putContentData(cv, CallLog.Calls.DATA_USAGE, callsDataUsage)
            dbUtils.putContentData(cv, CallLog.Calls.FEATURES, callsFeatures)
            dbUtils.putContentData(cv, CallLog.Calls.GEOCODED_LOCATION, callsGeocodedLocation)
            dbUtils.putContentData(cv, CallLog.Calls.IS_READ, callsIsRead)
            dbUtils.putContentData(cv, CallLog.Calls.NUMBER, callsNumber)
            dbUtils.putContentData(cv, CallLog.Calls.NUMBER_PRESENTATION, callsNumberPresentation)
            dbUtils.putContentData(cv, CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME, callsPhoneAccountComponentName)
            dbUtils.putContentData(cv, CallLog.Calls.PHONE_ACCOUNT_ID, callsPhoneAccountId)
            dbUtils.putContentData(cv, CallLog.Calls.TRANSCRIPTION, callsTranscription)
            dbUtils.putContentData(cv, CallLog.Calls.TYPE, callsType)
            dbUtils.putContentData(cv, CallLog.Calls.VOICEMAIL_URI, callsVoicemailUri)
            dbUtils.putContentData(cv, CallLog.Calls.DATE, callsDate)
            dbUtils.putContentData(cv, CallLog.Calls.DURATION, callsDuration)
            dbUtils.putContentData(cv, CallLog.Calls.NEW, callsNew)
        }
        context.contentResolver.insert(CallLog.Calls.CONTENT_URI, cv)
    }
}