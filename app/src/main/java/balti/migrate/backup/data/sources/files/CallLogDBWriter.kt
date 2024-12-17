package balti.migrate.backup.data.sources.files

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import balti.migrate.backup.data.model.CallLogData
import balti.migrate.backup.data.utils.getDataBase
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
import baltiapps.migrate.domain.backup.sources.DBWriter
import java.io.File

class CallLogDBWriter: DBWriter<CallLogData> {

    private lateinit var sqLiteDatabase: SQLiteDatabase

    override fun setup(fileLocation: String) {
        val dbFile = File(fileLocation).apply {
            if (exists()) delete()
        }

        getDataBase(dbFile).run {
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

    override fun writeRow(dataItem: CallLogData) {
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

    override fun close() {
        if (::sqLiteDatabase.isInitialized) {
            sqLiteDatabase.close()
        }
    }

}