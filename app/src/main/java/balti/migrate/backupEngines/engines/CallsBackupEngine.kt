package balti.migrate.backupEngines.engines

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import balti.filex.FileX
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.extraBackupsActivity.engines.calls.containers.CallsDataPacketsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_CALLS_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_CALLS_WRITE
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_CALLS_WRITE_TO_ACTUAL
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CALLS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_CALLS_VERIFY
import balti.migrate.utilities.CommonToolsKotlin.Companion.WARNING_CALLS
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
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc.getPercentage
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean

class CallsBackupEngine(private val jobcode: Int,
                        private val bd: BackupIntentData,
                        private val callsPackets: ArrayList<CallsDataPacketsKotlin>,
                        private val callsDBFileName: String) :
        ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_CALLS) {

    private val callsNameWithoutExtension by lazy {
        callsDBFileName.run {
            val ext = ".calls.db"
            if (endsWith(ext))
                substring(0, length - ext.length)
            else this
        }
    }
    private val callsDBFileActual by lazy { FileX.new(actualDestination, callsDBFileName) }
    private val internalDB by lazy { FileX.new(CACHE_DIR, callsDBFileName, true) }
    private val errors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }

    private fun writeCalls(){
        try {

            FileX.new(actualDestination).mkdirs()

            val title = getTitle(R.string.backing_calls)
            if (callsDBFileActual.exists()) callsDBFileActual.delete()
            if (internalDB.exists()) internalDB.delete()

            resetBroadcast(false, title)

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

            val dataBase: SQLiteDatabase = getDataBase(internalDB)

            dataBase.let { db ->
                db.execSQL(DROP_TABLE)
                db.execSQL(CREATE_TABLE)

                var display = ""

                for (i in 0 until callsPackets.size) {
                    try {

                        if (BackupServiceKotlin.cancelAll) {
                            tryIt { db.close() }
                            break
                        }
                        val dataPacket = callsPackets[i]

                        if (!dataPacket.selected) continue

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

                        broadcastProgress("", display, true, getPercentage(i+1, callsPackets.size))
                    }
                    catch (e: Exception){
                        e.printStackTrace()
                        errors.add("$ERR_CALLS_WRITE: $display ${e.message}")
                    }
                }

                db.close()
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_CALLS_TRY_CATCH: ${e.message}")
        }
    }

    private fun verifyCalls(){

        try {

            val title = getTitle(R.string.verifying_calls)

            resetBroadcast(false, title)

            var totalSelected = 0
            callsPackets.forEach { if (it.selected) totalSelected++ }

            val dataBase: SQLiteDatabase = getDataBase(internalDB)

            val cursor = dataBase.query(CALLS_TABLE_NAME, arrayOf("id"), null, null, null, null, null)
            cursor.moveToFirst()

            var c = 0
            do {

                c++
                broadcastProgress("", "", true, getPercentage(c, callsPackets.size))

            } while (cursor.moveToNext() && !BackupServiceKotlin.cancelAll)

            tryIt { cursor.close() }
            tryIt { dataBase.close() }

            if (c != totalSelected)
                warnings.add("$WARNING_CALLS: ${engineContext.getString(R.string.call_logs_incomplete)} - $c/${totalSelected}}")
        }
        catch (e: Exception){
            e.printStackTrace()
            warnings.add("$WARNING_CALLS: ${e.message}")
        }
    }

    private fun copyInternalDbFilesToBackup(){

        broadcastProgress(getStringFromRes(R.string.copying_database_to_backup), "", true)

        // copy the main file
        try {
            callsDBFileActual.createNewFile(overwriteIfExists = true)
            callsDBFileActual.refreshFile()
            internalDB.copyTo(callsDBFileActual, true)
            tryIt { internalDB.delete() }
        } catch (e: Exception) {
            errors.add("$ERR_CALLS_WRITE_TO_ACTUAL: ${getStringFromRes(R.string.call_log_write_to_actual_failed)} ${e.message}")
        }

        // copy other files like .journal .journal-wal
        try {
            CACHE.listFiles { file: FileX -> file.name.startsWith(callsNameWithoutExtension) }?.forEach {
                it.copyTo(FileX.new(actualDestination, it.name))
                tryIt { it.delete() }
            }
        } catch (e: Exception) {
            warnings.add("$ERR_CALLS_WRITE_TO_ACTUAL: ${getStringFromRes(R.string.call_log_auxiliary_write_to_actual_failed)} ${e.message}")
        }
    }

    override suspend fun doInBackground(arg: Any?): Any? {

        writeCalls()

        if (getPrefBoolean(PREF_CALLS_VERIFY, true) && errors.size == 0){
            verifyCalls()
        }

        copyInternalDbFilesToBackup()

        return 0
    }

    override fun postExecuteFunction() {
        val filesGenerated = FileX.new(actualDestination).listFiles { file: FileX ->
            file.name.startsWith(callsNameWithoutExtension)
        }
        onEngineTaskComplete.onComplete(jobcode, errors, warnings, filesGenerated)
    }
}