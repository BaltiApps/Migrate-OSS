package balti.migrate.backup.data.sources.callLog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import androidx.core.content.ContextCompat
import balti.migrate.common.data.model.CallLogData
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.exceptions.PermissionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CallLogSource(
    private val context: Context,
    private val dbUtils: DBUtils,
): DataSource<CallLogData> {
    override suspend fun checkPermission(): Boolean {
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

            val cursor = dbUtils.getCursor(context, CallLog.Calls.CONTENT_URI)

            val totalCount = cursor.count
            if (totalCount == 0) {
                cursor.close()
                return@flow
            }
            cursor.moveToFirst()

            cursor.use {
                for (i in 0 until totalCount) {
                    getSingleCallLog(cursor).run {
                        dataList.add(this)
                        emit(
                            Progress(
                                itemId = this._id,
                                progressType = Progress.ProgressType.CALL_LOG_READ,
                                percentage = getPercentage(i+1, totalCount),
                                logs = this.logInfo
                            )
                        )
                        cursor.moveToNext()
                    }
                }
            }

            onFinishedLoading(dataList)
        }.flowOn(Dispatchers.IO)
    }

    private fun getSingleCallLog(
        cursor: Cursor,
    ): CallLogData {

        dbUtils.run {
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
}