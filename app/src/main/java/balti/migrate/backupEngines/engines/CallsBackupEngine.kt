package balti.migrate.backupEngines.engines

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import balti.migrate.R
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_CALLS_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_CALLS_VERIFY
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_CALLS_VERIFY_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_CALLS_WRITE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_CALLS_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_CALLS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_CALLS_VERIFY
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_CACHED_FORMATTED_NUMBER
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_CACHED_LOOKUP_URI
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_CACHED_MATCHED_NUMBER
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_CACHED_NAME
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_CACHED_NORMALIZED_NUMBER
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_CACHED_NUMBER_LABEL
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_CACHED_NUMBER_TYPE
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_CACHED_PHOTO_ID
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_COUNTRY_ISO
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_DATA_USAGE
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_DATE
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_DURATION
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_FEATURES
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_GEOCODED_LOCATION
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_IS_READ
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_NEW
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_NUMBER
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_NUMBER_PRESENTATION
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_PHONE_ACCOUNT_COMPONENT_NAME
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_PHONE_ACCOUNT_ID
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_TABLE_NAME
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_TRANSCRIPTION
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_TYPE
import balti.migrate.utilities.constants.CallsDBConstants.Companion.CALLS_VOICEMAIL_URI
import java.io.File

class CallsBackupEngine(private val jobcode: Int,
                        private val bd: BackupIntentData,
                        private val callsPackets: ArrayList<CallsDataPacketsKotlin>,
                        private val callsDBFileName: String) :
        ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_CALLS) {

    private val callsDBFile by lazy { File(actualDestination, callsDBFileName) }
    private val errors by lazy { ArrayList<String>(0) }

    private fun writeCalls(){
        try {

            File(actualDestination).mkdirs()
            if (callsDBFile.exists()) callsDBFile.delete()

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.backing_calls) + " : " + madePartName
            else engineContext.getString(R.string.backing_calls)

            actualBroadcast.apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CALLS_NAME, "")
                putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
            }

            commonTools.LBM?.sendBroadcast(actualBroadcast)

            val DROP_TABLE = "DROP TABLE IF EXISTS $CALLS_TABLE_NAME"
            val CREATE_TABLE = "CREATE TABLE $CALLS_TABLE_NAME ( " +
                    "id INTEGER PRIMARY KEY, " +
                    "$CALLS_CACHED_FORMATTED_NUMBER TEXT, " +
                    "$CALLS_CACHED_LOOKUP_URI TEXT, " +
                    "$CALLS_CACHED_MATCHED_NUMBER TEXT," +
                    "$CALLS_CACHED_NAME TEXT, " +
                    "$CALLS_CACHED_NORMALIZED_NUMBER TEXT, " +
                    "$CALLS_CACHED_NUMBER_LABEL TEXT, " +
                    "$CALLS_CACHED_NUMBER_TYPE TEXT, " +
                    "$CALLS_CACHED_PHOTO_ID TEXT, " +
                    "$CALLS_COUNTRY_ISO TEXT, " +
                    "$CALLS_DATA_USAGE TEXT, " +
                    "$CALLS_FEATURES TEXT, " +
                    "$CALLS_GEOCODED_LOCATION TEXT, " +
                    "$CALLS_IS_READ TEXT, " +
                    "$CALLS_NUMBER TEXT, " +
                    "$CALLS_NUMBER_PRESENTATION TEXT, " +
                    "$CALLS_PHONE_ACCOUNT_COMPONENT_NAME TEXT, " +
                    "$CALLS_PHONE_ACCOUNT_ID TEXT, " +
                    "$CALLS_TRANSCRIPTION TEXT," +
                    "$CALLS_TYPE TEXT, " +
                    "$CALLS_VOICEMAIL_URI TEXT, " +
                    "$CALLS_DATE INTEGER, " +
                    "$CALLS_DURATION INTEGER, " +
                    "$CALLS_NEW INTEGER )"

            val dataBase: SQLiteDatabase = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
                SQLiteDatabase.openDatabase(callsDBFile.absolutePath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS or SQLiteDatabase.OPEN_READWRITE)
            else SQLiteDatabase.openOrCreateDatabase(callsDBFile.absolutePath, null)

            dataBase.let { db ->
                db.execSQL(DROP_TABLE)
                db.execSQL(CREATE_TABLE)

                var display = ""

                for (i in 0 until callsPackets.size) {
                    try {

                        if (isBackupCancelled) break
                        val dataPacket = callsPackets[i]

                        display = if (dataPacket.callsCachedName != null && dataPacket.callsCachedName != "")
                            dataPacket.callsCachedName
                        else dataPacket.callsNumber.toString()

                        val contentValues = ContentValues()
                        contentValues.put(CALLS_CACHED_FORMATTED_NUMBER, dataPacket.callsCachedFormattedNumber)
                        contentValues.put(CALLS_CACHED_LOOKUP_URI, dataPacket.callsCachedLookupUri)
                        contentValues.put(CALLS_CACHED_MATCHED_NUMBER, dataPacket.callsCachedMatchedNumber)
                        contentValues.put(CALLS_CACHED_NAME, dataPacket.callsCachedName)
                        contentValues.put(CALLS_CACHED_NORMALIZED_NUMBER, dataPacket.callsCachedNormalizedNumber)
                        contentValues.put(CALLS_CACHED_NUMBER_LABEL, dataPacket.callsCachedNumberLabel)
                        contentValues.put(CALLS_CACHED_NUMBER_TYPE, dataPacket.callsCachedNumberType)
                        contentValues.put(CALLS_CACHED_PHOTO_ID, dataPacket.callsCachedPhotoId)
                        contentValues.put(CALLS_COUNTRY_ISO, dataPacket.callsCountryIso)
                        contentValues.put(CALLS_DATA_USAGE, dataPacket.callsDataUsage)
                        contentValues.put(CALLS_FEATURES, dataPacket.callsFeatures)
                        contentValues.put(CALLS_GEOCODED_LOCATION, dataPacket.callsGeocodedLocation)
                        contentValues.put(CALLS_IS_READ, dataPacket.callsIsRead)
                        contentValues.put(CALLS_NUMBER, dataPacket.callsNumber)
                        contentValues.put(CALLS_NUMBER_PRESENTATION, dataPacket.callsNumberPresentation)
                        contentValues.put(CALLS_PHONE_ACCOUNT_COMPONENT_NAME, dataPacket.callsPhoneAccountComponentName)
                        contentValues.put(CALLS_PHONE_ACCOUNT_ID, dataPacket.callsPhoneAccountId)
                        contentValues.put(CALLS_TRANSCRIPTION, dataPacket.callsTranscription)
                        contentValues.put(CALLS_TYPE, dataPacket.callsType)
                        contentValues.put(CALLS_VOICEMAIL_URI, dataPacket.callsVoicemailUri)
                        contentValues.put(CALLS_DATE, dataPacket.callsDate)
                        contentValues.put(CALLS_DURATION, dataPacket.callsDuration)
                        contentValues.put(CALLS_NEW, dataPacket.callsNew)

                        db.insert(CALLS_TABLE_NAME, null, contentValues)

                        actualBroadcast.apply {
                            putExtra(EXTRA_CALLS_NAME, display)
                            putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage(i+1, callsPackets.size))
                        }

                        commonTools.LBM?.sendBroadcast(actualBroadcast)
                    }
                    catch (e: Exception){
                        e.printStackTrace()
                        errors.add("$ERR_CALLS_WRITE${bd.errorTag}: $display ${e.message}")
                    }
                }

                db.close()
            }

            writeToFileList(callsDBFileName)
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_CALLS_TRY_CATCH${bd.errorTag}: ${e.message}")
        }
    }

    private fun verifyCalls(){

        try {

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.verifying_calls) + " : " + madePartName
            else engineContext.getString(R.string.verifying_calls)

            actualBroadcast.apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
                removeExtra(EXTRA_CALLS_NAME)
            }

            commonTools.LBM?.sendBroadcast(actualBroadcast)

            val dataBase: SQLiteDatabase = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
                SQLiteDatabase.openDatabase(callsDBFile.absolutePath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS or SQLiteDatabase.OPEN_READWRITE)
            else SQLiteDatabase.openOrCreateDatabase(callsDBFile.absolutePath, null)

            val cursor = dataBase.query(CALLS_TABLE_NAME, arrayOf("id"), null, null, null, null, null)
            cursor.moveToFirst()
            var c = 0

            do {

                c++
                actualBroadcast.putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage(c, callsPackets.size))
                commonTools.LBM?.sendBroadcast(actualBroadcast)

            } while (cursor.moveToNext() && !isBackupCancelled)

            commonTools.tryIt { cursor.close() }

            if (c != callsPackets.size)
                errors.add("$ERR_CALLS_VERIFY${bd.errorTag}: ${engineContext.getString(R.string.call_logs_incomplete)} - $c/${callsPackets.size}}")
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_CALLS_VERIFY_TRY_CATCH${bd.errorTag}: ${e.message}")
        }
    }

    override fun doInBackground(vararg params: Any?): Any {

        writeCalls()

        if (sharedPreferences.getBoolean(PREF_CALLS_VERIFY, true) && errors.size == 0){
            verifyCalls()
        }

        return 0
    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        if (errors.size == 0)
            onBackupComplete.onBackupComplete(jobcode, true, 0)
        else onBackupComplete.onBackupComplete(jobcode, false, errors)
    }
}