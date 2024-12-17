package balti.migrate.backup.data.sources

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import androidx.core.content.ContextCompat
import balti.migrate.backup.data.model.CallLogData
import balti.migrate.backup.data.utils.getCursorData
import baltiapps.migrate.domain.exceptions.ContentReadException
import baltiapps.migrate.domain.backup.getPercentage
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.sources.PlatformDataSource
import baltiapps.migrate.domain.exceptions.PermissionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CallLogSource(
    private val context: Context,
): PlatformDataSource<CallLogData> {
    override fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun getData(onFinishedLoading: (List<CallLogData>) -> Unit): Flow<Progress> {
        val dataList = mutableListOf<CallLogData>()

        return flow {
            if (!checkPermission()) {
                throw PermissionException(
                    permissionName = Manifest.permission.READ_CALL_LOG,
                    message = "Call log permission not granted"
                )
            }

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                null
            ) ?: throw ContentReadException("Call log read cursor is null!")

            val totalCount = cursor.count
            if (totalCount == 0) return@flow
            cursor.moveToFirst()

            for (i in 0 until totalCount) {
                getSingleCallLog(cursor).run {
                    dataList.add(this)
                    emit(
                        Progress(
                            progressType = Progress.ProgressType.CALL_LOG_READ,
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

    private fun getSingleCallLog(
        cursor: Cursor,
    ): CallLogData {

        val callerNumber = getCursorData<String>(cursor, CallLog.Calls.NUMBER)
        val callerName = getCursorData<String>(cursor, CallLog.Calls.CACHED_NAME)

        return CallLogData(
            _id = getCursorData<String>(cursor, CallLog.Calls._ID),
            logInfo = callerName.ifBlank { callerNumber },

            callsCachedName = callerName,
            callsCountryIso = getCursorData<String>(cursor, CallLog.Calls.COUNTRY_ISO),
            callsDataUsage = getCursorData<Long>(cursor, CallLog.Calls.DATA_USAGE),
            callsFeatures = getCursorData<Int>(cursor, CallLog.Calls.FEATURES),
            callsGeocodedLocation = getCursorData<String>(cursor, CallLog.Calls.GEOCODED_LOCATION),
            callsIsRead = getCursorData<Boolean>(cursor, CallLog.Calls.IS_READ),
            callsNumber = callerNumber,
            callsNumberPresentation = getCursorData<Int>(cursor, CallLog.Calls.NUMBER_PRESENTATION),
            callsPhoneAccountComponentName = getCursorData<String>(cursor, CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME),
            callsPhoneAccountId = getCursorData<String>(cursor, CallLog.Calls.PHONE_ACCOUNT_ID),
            callsTranscription = getCursorData<String>(cursor, CallLog.Calls.TRANSCRIPTION),
            callsType = getCursorData<Int>(cursor, CallLog.Calls.TYPE),
            callsVoicemailUri = getCursorData<String>(cursor, CallLog.Calls.VOICEMAIL_URI),
            callsDate = getCursorData<Long>(cursor, CallLog.Calls.DATE),
            callsDuration = getCursorData<Long>(cursor, CallLog.Calls.DURATION),
            callsNew = getCursorData<Boolean>(cursor, CallLog.Calls.NEW),
        )
    }
}