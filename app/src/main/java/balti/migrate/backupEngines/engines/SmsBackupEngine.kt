package balti.migrate.backupEngines.engines

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.extraBackupsActivity.sms.containers.SmsDataPacketKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_SMS_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_SMS_VERIFY
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_SMS_VERIFY_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_SMS_WRITE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_SMS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_SMS_ADDRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_SMS_VERIFY
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
import java.io.File

class SmsBackupEngine(private val jobcode: Int,
                      private val bd: BackupIntentData,
                      private val smsPackets: ArrayList<SmsDataPacketKotlin>,
                      private val smsDBFileName: String) :
        ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_SMS) {

    private val smsDBFile by lazy { File(actualDestination, smsDBFileName) }
    private val errors by lazy { ArrayList<String>(0) }

    private fun writeSms(){
        try {

            File(actualDestination).mkdirs()
            if (smsDBFile.exists()) smsDBFile.delete()

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.backing_sms) + " : " + madePartName
            else engineContext.getString(R.string.backing_sms)

            actualBroadcast.apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SMS_ADDRESS, "")
                putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
            }
            broadcastProgress()

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

            val dataBase: SQLiteDatabase = getDataBase(smsDBFile)

            dataBase.let { db ->
                db.execSQL(DROP_TABLE)
                db.execSQL(CREATE_TABLE)

                for (i in 0 until smsPackets.size) {
                    try {

                        if (BackupServiceKotlin.cancelAll) {
                            commonTools.tryIt { db.close() }
                            break
                        }

                        val dataPacket = smsPackets[i]

                        val contentValues = ContentValues()
                        contentValues.put(SMS_ADDRESS, dataPacket.smsAddress)
                        contentValues.put(SMS_BODY, dataPacket.smsBody)
                        contentValues.put(SMS_DATE, dataPacket.smsDate)
                        contentValues.put(SMS_DATE_SENT, dataPacket.smsDateSent)
                        contentValues.put(SMS_TYPE, dataPacket.smsType)
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

                        actualBroadcast.apply {
                            putExtra(EXTRA_SMS_ADDRESS, dataPacket.smsAddress)
                            putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage(i+1, smsPackets.size))
                        }

                        broadcastProgress()
                    }
                    catch (e: Exception){
                        e.printStackTrace()
                        errors.add("$ERR_SMS_WRITE${bd.errorTag}: ${e.message}")
                    }
                }

                db.close()

                File(smsDBFile.absolutePath + "-shm").delete()
                File(smsDBFile.absolutePath + "-wal").delete()

                writeToFileList(smsDBFileName)
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_SMS_TRY_CATCH${bd.errorTag}: ${e.message}")
        }
    }

    private fun verifySms(){

        try {

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.verifying_sms) + " : " + madePartName
            else engineContext.getString(R.string.verifying_sms)

            actualBroadcast.apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
                removeExtra(EXTRA_SMS_ADDRESS)
            }

            broadcastProgress()

            val dataBase: SQLiteDatabase = getDataBase(smsDBFile)

            val cursor = dataBase.query(SMS_TABLE_NAME, arrayOf("id"), null, null, null, null, null)
            cursor.moveToFirst()
            var c = 0

            do {

                c++
                actualBroadcast.putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage(c, smsPackets.size))
                broadcastProgress()

            } while (cursor.moveToNext() && !BackupServiceKotlin.cancelAll)

            commonTools.tryIt { cursor.close() }
            commonTools.tryIt { dataBase.close() }

            if (c != smsPackets.size)
                errors.add("$ERR_SMS_VERIFY${bd.errorTag}: ${engineContext.getString(R.string.sms_records_incomplete)} - $c/${smsPackets.size}}")
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_SMS_VERIFY_TRY_CATCH${bd.errorTag}: ${e.message}")
        }
    }

    override fun doInBackground(vararg params: Any?): Any {

        writeSms()

        if (sharedPreferences.getBoolean(PREF_SMS_VERIFY, true) && errors.size == 0){
            verifySms()
        }

        return 0
    }

    override fun postExecuteFunction() {
        onBackupComplete.onBackupComplete(jobcode, errors.size == 0, errors)
    }

}