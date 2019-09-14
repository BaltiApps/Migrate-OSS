package balti.migrate.backupEngines.engines

import balti.migrate.R
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.utilities.CommonToolKotlin.Companion.BACKUP_NAME_SETTINGS
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_SETTINGS_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_SETTINGS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.constants.SettingsFields.Companion.JSON_FIELD_ADB_TEXT
import balti.migrate.utilities.constants.SettingsFields.Companion.JSON_FIELD_DPI_TEXT
import balti.migrate.utilities.constants.SettingsFields.Companion.JSON_FIELD_FONT_SCALE
import balti.migrate.utilities.constants.SettingsFields.Companion.JSON_FIELD_KEYBOARD_TEXT
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

class SettingsBackupEngine(private val jobcode: Int,
                           private val bd: BackupIntentData,
                           private val dpiText: String?,
                           private val adbState: Int?,
                           private val fontScale: Int?,
                           private val keyboardText: String?) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_SETTINGS) {

    private val settingsFile by lazy { File(actualDestination, BACKUP_NAME_SETTINGS) }
    private val errors by lazy { ArrayList<String>(0) }

    override fun doInBackground(vararg params: Any?): Any {

        try {

            File(actualDestination).mkdirs()
            if (settingsFile.exists()) settingsFile.delete()

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.writing_settings) + " : " + madePartName
            else engineContext.getString(R.string.writing_settings)

            actualBroadcast.putExtra(EXTRA_TITLE, title)
            commonTools.LBM?.sendBroadcast(actualBroadcast)

            val jsonObject = JSONObject()
            dpiText?.let { jsonObject.put(JSON_FIELD_DPI_TEXT, it) }
            adbState?.let { jsonObject.put(JSON_FIELD_ADB_TEXT, it) }
            fontScale?.let { jsonObject.put(JSON_FIELD_FONT_SCALE, it) }
            keyboardText?.let { jsonObject.put(JSON_FIELD_KEYBOARD_TEXT, it) }

            BufferedWriter(FileWriter(settingsFile, true)).run {
                this.write(jsonObject.toString())
                this.close()
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_SETTINGS_TRY_CATCH${bd.errorTag}: ${e.message}")
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