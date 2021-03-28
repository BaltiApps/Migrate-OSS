package balti.migrate.backupEngines.engines

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.extraBackupsActivity.sms.containers.SmsDataPacketKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_SMS_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_SMS_WRITE
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_SMS_WRITE_TO_ACTUAL
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_SMS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SMS_VERIFY
import balti.migrate.utilities.CommonToolsKotlin.Companion.WARNING_SMS
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_ADDRESS
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_BODY
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_CREATOR
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_DATE
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_DATE_SENT
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_ERROR
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_LOCKED
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_PERSON
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_PROTOCOL
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_READ
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_REPLY_PATH_PRESENT
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_SEEN
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_SERVICE_CENTER
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_STATUS
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_SUBJECT
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_TABLE_NAME
import balti.migrate.utilities.constants.SmsDBConstant.Companion.SMS_TYPE
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc.getPercentage
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean

class SmsBackupEngine(private val jobcode: Int,
                      private val bd: BackupIntentData,
                      private val smsPackets: ArrayList<SmsDataPacketKotlin>,
                      private val smsDBFileName: String) :
        ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_SMS) {

    private val smsNameWithoutExtension by lazy {
        smsDBFileName.run {
            val ext = ".sms.db"
            if (endsWith(ext))
                substring(0, length - ext.length)
            else this
        }
    }
    private val smsDBFileActual by lazy { FileX.new(actualDestination, smsDBFileName) }
    private val internalDB by lazy { FileX.new(CACHE_DIR, smsDBFileName) }
    private val errors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }

    private fun writeSms(){
        try {

            FileX.new(actualDestination).mkdirs()
            if (smsDBFileActual.exists()) smsDBFileActual.delete()
            if (internalDB.exists()) internalDB.delete()

            val title = getTitle(R.string.backing_sms)

            resetBroadcast(false, title)

            val DROP_TABLE = "DROP TABLE IF EXISTS $SMS_TABLE_NAME"
            val CREATE_TABLE = "CREATE TABLE $SMS_TABLE_NAME ( " +
                    "id INTEGER PRIMARY KEY, " +
                    "$SMS_ADDRESS TEXT, " +
                    "$SMS_BODY TEXT, " +
                    "$SMS_TYPE TEXT, " +
                    "$SMS_DATE TEXT, " +

                    "$SMS_DATE_SENT TEXT, " +
                    "$SMS_CREATOR TEXT, " +
                    "$SMS_PERSON TEXT, " +
                    "$SMS_PROTOCOL TEXT, " +

                    "$SMS_SEEN TEXT, " +
                    "$SMS_SERVICE_CENTER TEXT, " +
                    "$SMS_STATUS TEXT, " +
                    "$SMS_SUBJECT TEXT, " +

                    "$SMS_ERROR INTEGER, " +
                    "$SMS_READ INTEGER, " +
                    "$SMS_LOCKED INTEGER, " +
                    "$SMS_REPLY_PATH_PRESENT INTEGER )"

            val dataBase: SQLiteDatabase = getDataBase(if (FileXInit.isTraditional) smsDBFileActual else internalDB)

            dataBase.let { db ->
                db.execSQL(DROP_TABLE)
                db.execSQL(CREATE_TABLE)

                for (i in 0 until smsPackets.size) {
                    try {

                        if (BackupServiceKotlin.cancelAll) {
                            tryIt { db.close() }
                            break
                        }

                        val dataPacket = smsPackets[i]

                        if (!dataPacket.selected) continue

                        val contentValues = ContentValues()
                        contentValues.put(SMS_ADDRESS, dataPacket.smsAddress)
                        contentValues.put(SMS_BODY, dataPacket.smsBody)
                        contentValues.put(SMS_TYPE, dataPacket.smsType)
                        contentValues.put(SMS_DATE, dataPacket.smsDate)

                        contentValues.put(SMS_DATE_SENT, dataPacket.smsDateSent)
                        contentValues.put(SMS_CREATOR, dataPacket.smsCreator)
                        contentValues.put(SMS_PERSON, dataPacket.smsPerson)
                        contentValues.put(SMS_PROTOCOL, dataPacket.smsProtocol)

                        contentValues.put(SMS_SEEN, dataPacket.smsSeen)
                        contentValues.put(SMS_SERVICE_CENTER, dataPacket.smsServiceCenter)
                        contentValues.put(SMS_STATUS, dataPacket.smsStatus)
                        contentValues.put(SMS_SUBJECT, dataPacket.smsSubject)

                        contentValues.put(SMS_ERROR, dataPacket.smsError)
                        contentValues.put(SMS_READ, dataPacket.smsRead)
                        contentValues.put(SMS_LOCKED, dataPacket.smsLocked)
                        contentValues.put(SMS_REPLY_PATH_PRESENT, dataPacket.smsReplyPathPresent)

                        db.insert(SMS_TABLE_NAME, null, contentValues)

                        broadcastProgress("", dataPacket.smsAddress.toString(), true, getPercentage(i+1, smsPackets.size))

                    }
                    catch (e: Exception){
                        e.printStackTrace()
                        errors.add("$ERR_SMS_WRITE: ${e.message}")
                    }
                }

                db.close()
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_SMS_TRY_CATCH: ${e.message}")
        }
    }

    private fun verifySms(){

        try {

            val title = getTitle(R.string.verifying_sms)

            resetBroadcast(false, title)

            var totalSelected = 0
            smsPackets.forEach { if (it.selected) totalSelected++ }

            val dataBase: SQLiteDatabase = getDataBase(if (FileXInit.isTraditional) smsDBFileActual else internalDB)

            val cursor = dataBase.query(SMS_TABLE_NAME, arrayOf("id"), null, null, null, null, null)
            cursor.moveToFirst()

            var c = 0
            do {

                c++
                broadcastProgress("", "", true, getPercentage(c, smsPackets.size))

            } while (cursor.moveToNext() && !BackupServiceKotlin.cancelAll)

            tryIt { cursor.close() }
            tryIt { dataBase.close() }

            if (c != totalSelected)
                warnings.add("$WARNING_SMS: ${engineContext.getString(R.string.sms_records_incomplete)} - $c/${totalSelected}}")
        }
        catch (e: Exception){
            e.printStackTrace()
            warnings.add("$WARNING_SMS: ${e.message}")
        }
    }

    override suspend fun doInBackground(arg: Any?): Any? {

        writeSms()

        if (!BackupServiceKotlin.cancelAll && getPrefBoolean(PREF_SMS_VERIFY, true) && errors.size == 0){
            verifySms()
        }

        if (!FileXInit.isTraditional) {

            // copy the main file
            try {
                internalDB.copyTo(smsDBFileActual, true)
                tryIt { internalDB.delete() }
            } catch (e: Exception) {
                errors.add("$ERR_SMS_WRITE_TO_ACTUAL: ${getStringFromRes(R.string.sms_records_write_to_actual_failed)} ${e.message}")
            }

            // copy other files like .journal .journal-wal
            try {
                FileX.new(CACHE_DIR).listFiles { file: FileX -> file.name.startsWith(smsNameWithoutExtension) }?.forEach {
                    it.copyTo(FileX.new(actualDestination, it.name))
                    tryIt { it.delete() }
                }
            } catch (e: Exception) {
                warnings.add("$ERR_SMS_WRITE_TO_ACTUAL: ${getStringFromRes(R.string.sms_records_auxiliary_write_to_actual_failed)} ${e.message}")
            }
        }

        return 0
    }

    override fun postExecuteFunction() {
        val filesGenerated = FileX.new(actualDestination).listFiles { file: FileX ->
            file.name.startsWith(smsNameWithoutExtension)
        }
        onEngineTaskComplete.onComplete(jobcode, errors, warnings, filesGenerated)
    }

}