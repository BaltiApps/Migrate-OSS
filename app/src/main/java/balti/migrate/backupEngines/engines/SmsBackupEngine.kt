package balti.migrate.backupEngines.engines

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import balti.filex.FileX
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.AppInstance.Companion.smsList
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass_new
import balti.migrate.backupEngines.utils.EngineJobResultHolder
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.ProgressType.PROGRESS_TYPE_SMS
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_SMS_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_SMS_WRITE
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_SMS_WRITE_TO_ACTUAL
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

class SmsBackupEngine(private val smsDBFileName: String) : ParentBackupClass_new(PROGRESS_TYPE_SMS) {

    override val className: String = "SmsBackupEngine"

    private val smsNameWithoutExtension by lazy {
        smsDBFileName.run {
            val ext = ".sms.db"
            if (endsWith(ext))
                substring(0, length - ext.length)
            else this
        }
    }
    private val smsDBFileActual by lazy { FileX.new(fileXDestination, smsDBFileName) }
    private val internalDB by lazy { FileX.new(CACHE_DIR, smsDBFileName, true) }

    private val smsPackets by lazy { smsList }

    private val generatedFiles = ArrayList<FileX>(0)

    private fun writeSms(){
        try {

            FileX.new(fileXDestination).mkdirs()

            val title = getTitle(R.string.backing_sms)
            if (smsDBFileActual.exists()) smsDBFileActual.delete()
            if (internalDB.exists()) internalDB.delete()

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

            val dataBase: SQLiteDatabase = getDataBase(internalDB)

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

            val dataBase: SQLiteDatabase = getDataBase(internalDB)

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
                warnings.add("$WARNING_SMS: ${getStringFromRes(R.string.sms_records_incomplete)} - $c/${totalSelected}}")
        }
        catch (e: Exception){
            e.printStackTrace()
            warnings.add("$WARNING_SMS: ${e.message}")
        }
    }

    private fun copyInternalDbFilesToBackup(){

        broadcastProgress(getStringFromRes(R.string.copying_database_to_backup), "", true)

        // copy the main file
        try {
            smsDBFileActual.createNewFile(overwriteIfExists = true)
            smsDBFileActual.exists()
            internalDB.copyTo(smsDBFileActual, true)
            generatedFiles.add(smsDBFileActual)
            tryIt { internalDB.delete() }
        } catch (e: Exception) {
            errors.add("$ERR_SMS_WRITE_TO_ACTUAL: ${getStringFromRes(R.string.sms_records_write_to_actual_failed)} ${e.message}")
        }

        // copy other files like .journal .journal-wal
        try {
            FileX.new(CACHE_DIR).listFiles { file: FileX -> file.name.startsWith(smsNameWithoutExtension) }?.forEach {
                it.copyTo(
                    FileX.new(fileXDestination, it.name).apply {
                        generatedFiles.add(this)
                    }
                )
                tryIt { it.delete() }
            }
        } catch (e: Exception) {
            warnings.add("$ERR_SMS_WRITE_TO_ACTUAL: ${getStringFromRes(R.string.sms_records_auxiliary_write_to_actual_failed)} ${e.message}")
        }
    }

    override suspend fun backgroundProcessing(): EngineJobResultHolder {

        writeSms()

        if (!BackupServiceKotlin.cancelAll && getPrefBoolean(PREF_SMS_VERIFY, true) && errors.size == 0){
            verifySms()
        }

        copyInternalDbFilesToBackup()

        return EngineJobResultHolder(errors.isEmpty(), generatedFiles, errors, warnings)
    }

}