package balti.migrate.backupEngines.engines

import balti.migrate.R
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.utilities.CommonToolsKotlin.Companion.BACKUP_NAME_SETTINGS
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_SETTINGS_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_SETTINGS
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
                           private val fontScale: Double?,
                           private val keyboardText: String?) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_SETTINGS) {

    private val settingsFile by lazy { File(actualDestination, BACKUP_NAME_SETTINGS) }
    private val errors by lazy { ArrayList<String>(0) }

    override suspend fun doInBackground(arg: Any?): Any? {

        try {

            File(actualDestination).mkdirs()
            if (settingsFile.exists()) settingsFile.delete()

            val title = getTitle(R.string.writing_settings)

            resetBroadcast(true, title)

            val jsonObject = JSONObject()
            dpiText?.let { jsonObject.put(JSON_FIELD_DPI_TEXT, it.trim()) }
            adbState?.let { jsonObject.put(JSON_FIELD_ADB_TEXT, it) }
            fontScale?.let { jsonObject.put(JSON_FIELD_FONT_SCALE, it) }
            keyboardText?.let { jsonObject.put(JSON_FIELD_KEYBOARD_TEXT, it.trim()) }

            heavyTask {
                BufferedWriter(FileWriter(settingsFile, true)).run {
                    this.write(jsonObject.toString(4))
                    this.close()
                }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_SETTINGS_TRY_CATCH: ${e.message}")
        }

        return 0
    }

    override fun postExecuteFunction() {
        onEngineTaskComplete.onComplete(jobcode, errors, jobResults = arrayOf(settingsFile))
    }
}