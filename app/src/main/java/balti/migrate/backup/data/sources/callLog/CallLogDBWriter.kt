package balti.migrate.backup.data.sources.callLog

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import balti.migrate.common.data.model.CallLogData
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_CACHED_NAME
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_COUNTRY_ISO
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_DATA_USAGE
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_DATE
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_DURATION
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_FEATURES
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_GEOCODED_LOCATION
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_IS_READ
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_NEW
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_NUMBER
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_NUMBER_PRESENTATION
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_PHONE_ACCOUNT_COMPONENT_NAME
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_PHONE_ACCOUNT_ID
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_TABLE_NAME
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_TRANSCRIPTION
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_TYPE
import baltiapps.migrate.domain.CallLogDBConstants.Companion.CALLS_VOICEMAIL_URI
import baltiapps.migrate.domain.REDACTED
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

class CallLogDBWriter(
    private val dbUtils: DBUtils,
): DataBackup<CallLogData> {

    private lateinit var sqLiteDatabase: SQLiteDatabase

    override fun setLocation(file: GenericFile) {
        super.setLocation(file)

        val dbFile = File(file.path).apply {
            if (exists()) delete()
        }

        dbUtils.getDataBase(dbFile).run {
            val sqlDropTable = "DROP TABLE IF EXISTS $CALLS_TABLE_NAME"
            val sqlCreateTable = "CREATE TABLE $CALLS_TABLE_NAME ( " +
                    "id INTEGER PRIMARY KEY" +
                    ", $CALLS_CACHED_NAME TEXT" +
                    ", $CALLS_COUNTRY_ISO TEXT" +
                    ", $CALLS_DATA_USAGE BIGINT" +
                    ", $CALLS_FEATURES INTEGER" +
                    ", $CALLS_GEOCODED_LOCATION TEXT" +
                    ", $CALLS_IS_READ TEXT" +
                    ", $CALLS_NUMBER TEXT" +
                    ", $CALLS_NUMBER_PRESENTATION INTEGER" +
                    ", $CALLS_PHONE_ACCOUNT_COMPONENT_NAME TEXT" +
                    ", $CALLS_PHONE_ACCOUNT_ID TEXT" +
                    ", $CALLS_TRANSCRIPTION TEXT" +
                    ", $CALLS_TYPE INTEGER" +
                    ", $CALLS_VOICEMAIL_URI TEXT" +
                    ", $CALLS_DATE BIGINT" +
                    ", $CALLS_DURATION BIGINT" +
                    ", $CALLS_NEW TEXT" +
                    ")"
            sqLiteDatabase = this
            execSQL(sqlDropTable)
            execSQL(sqlCreateTable)
        }
    }

    override fun backupDataItems(dataItems: List<CallLogData>): Flow<Progress> {
        return flow {
            dataItems.forEachIndexed { index, item ->
                val progress = Progress(
                    itemId = item._id,
                    progressType = Progress.ProgressType.CALL_LOG_BACKUP,
                    percentage = getPercentage(index + 1, dataItems.size),
                    logs = "CALL LOG (${index + 1}/${dataItems.size}) ${item.logInfo}",
                    logsForStorage = "CALL LOG (${index + 1}/${dataItems.size}) $REDACTED",
                )
                runCatchingWithProgress(progress) {
                    writeRow(item)
                }.run { emit(this) }
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun writeRow(dataItem: CallLogData) {
        val contentValues = ContentValues()
        Pair(contentValues, dataItem).let { (c, d) ->
            c.put("id", d._id)
            c.put(CALLS_CACHED_NAME, d.callsCachedName)
            c.put(CALLS_COUNTRY_ISO, d.callsCountryIso)
            c.put(CALLS_DATA_USAGE, d.callsDataUsage)
            c.put(CALLS_FEATURES, d.callsFeatures)
            c.put(CALLS_GEOCODED_LOCATION, d.callsGeocodedLocation)
            c.put(CALLS_IS_READ, d.callsIsRead)
            c.put(CALLS_NUMBER, d.callsNumber)
            c.put(CALLS_NUMBER_PRESENTATION, d.callsNumberPresentation)
            c.put(CALLS_PHONE_ACCOUNT_COMPONENT_NAME, d.callsPhoneAccountComponentName)
            c.put(CALLS_PHONE_ACCOUNT_ID, d.callsPhoneAccountId)
            c.put(CALLS_TRANSCRIPTION, d.callsTranscription)
            c.put(CALLS_TYPE, d.callsType)
            c.put(CALLS_VOICEMAIL_URI, d.callsVoicemailUri)
            c.put(CALLS_DATE, d.callsDate)
            c.put(CALLS_DURATION, d.callsDuration)
            c.put(CALLS_NEW, d.callsNew)
        }
        sqLiteDatabase.insert(CALLS_TABLE_NAME, null, contentValues)
    }

    override fun onBackupOver() {
        if (::sqLiteDatabase.isInitialized) {
            sqLiteDatabase.close()
        }
    }

}