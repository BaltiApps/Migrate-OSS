package balti.migrate.restore.data.sources.callLog

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import balti.migrate.common.model.CallLogData
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.CallLogDBConstants
import baltiapps.migrate.domain.backup.getPercentage
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.DBReader
import baltiapps.migrate.domain.exceptions.ContentReadException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class CallLogDBReader(
    private val dbUtils: DBUtils,
): DBReader<CallLogData> {

    private lateinit var sqLiteDatabase: SQLiteDatabase

    override fun setup(fileLocation: String) {
        val dbFile = File(fileLocation).apply {
            if (!canRead()) throw ContentReadException("Cannot read call log DB file - $fileLocation")
        }

        sqLiteDatabase = dbUtils.getDataBase(dbFile)
    }

    override fun readRows(onFinished: (List<CallLogData>) -> Unit): Flow<Progress> {
        val dataList = mutableListOf<CallLogData>()

        return flow {
            val cursor = dbUtils.getCursor(sqLiteDatabase, CallLogDBConstants.CALLS_TABLE_NAME)

            val totalCount = cursor.count
            if (totalCount == 0) return@flow
            cursor.moveToFirst()

            for (i in 0 until totalCount) {
                getSingleCallLog(cursor).run {
                    dataList.add(this)
                    emit(
                        Progress(
                            progressType = Progress.ProgressType.CALL_LOG_BACKUP_READ,
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

    private fun getSingleCallLog(
        cursor: Cursor,
    ): CallLogData {

        dbUtils.run {
            val callerNumber = getCursorData<String>(cursor, CallLogDBConstants.CALLS_NUMBER)
            val callerName = getCursorData<String>(cursor, CallLogDBConstants.CALLS_CACHED_NAME)

            return CallLogData(
                _id = getCursorData<String>(cursor, "id"),
                logInfo = callerName.ifBlank { callerNumber },

                callsCachedName = callerName,
                callsCountryIso = getCursorData<String>(cursor, CallLogDBConstants.CALLS_COUNTRY_ISO),
                callsDataUsage = getCursorData<Long>(cursor, CallLogDBConstants.CALLS_DATA_USAGE),
                callsFeatures = getCursorData<Int>(cursor, CallLogDBConstants.CALLS_FEATURES),
                callsGeocodedLocation = getCursorData<String>(cursor, CallLogDBConstants.CALLS_GEOCODED_LOCATION),
                callsIsRead = getCursorData<Boolean>(cursor, CallLogDBConstants.CALLS_IS_READ),
                callsNumber = callerNumber,
                callsNumberPresentation = getCursorData<Int>(cursor, CallLogDBConstants.CALLS_NUMBER_PRESENTATION),
                callsPhoneAccountComponentName = getCursorData<String>(cursor, CallLogDBConstants.CALLS_PHONE_ACCOUNT_COMPONENT_NAME),
                callsPhoneAccountId = getCursorData<String>(cursor, CallLogDBConstants.CALLS_PHONE_ACCOUNT_ID),
                callsTranscription = getCursorData<String>(cursor, CallLogDBConstants.CALLS_TRANSCRIPTION),
                callsType = getCursorData<Int>(cursor, CallLogDBConstants.CALLS_TYPE),
                callsVoicemailUri = getCursorData<String>(cursor, CallLogDBConstants.CALLS_VOICEMAIL_URI),
                callsDate = getCursorData<Long>(cursor, CallLogDBConstants.CALLS_DATE),
                callsDuration = getCursorData<Long>(cursor, CallLogDBConstants.CALLS_DURATION),
                callsNew = getCursorData<Boolean>(cursor, CallLogDBConstants.CALLS_NEW),
            )
        }
    }

    override fun close() {
        if (::sqLiteDatabase.isInitialized) {
            sqLiteDatabase.close()
        }
    }
}