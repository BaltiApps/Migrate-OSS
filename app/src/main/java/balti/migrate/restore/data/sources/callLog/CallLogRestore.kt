package balti.migrate.restore.data.sources.callLog

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import balti.migrate.common.model.CallLogData
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.restore.sources.DataRestore

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

    override fun restoreDataItem(dataItem: CallLogData) {
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