package balti.migrate.backupEngines.engines

import balti.filex.FileX
import balti.migrate.AppInstance.Companion.adbState
import balti.migrate.AppInstance.Companion.dpiText
import balti.migrate.AppInstance.Companion.fontScale
import balti.migrate.AppInstance.Companion.keyboardText
import balti.migrate.R
import balti.migrate.backupEngines.ParentBackupClass_new
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.ProgressType.PROGRESS_TYPE_SETTINGS
import balti.migrate.utilities.CommonToolsKotlin.Companion.BACKUP_NAME_SETTINGS
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_SETTINGS_TRY_CATCH
import balti.migrate.utilities.constants.SettingsFields.Companion.JSON_FIELD_ADB_TEXT
import balti.migrate.utilities.constants.SettingsFields.Companion.JSON_FIELD_DPI_TEXT
import balti.migrate.utilities.constants.SettingsFields.Companion.JSON_FIELD_FONT_SCALE
import balti.migrate.utilities.constants.SettingsFields.Companion.JSON_FIELD_KEYBOARD_TEXT
import org.json.JSONObject

class SettingsBackupEngine : ParentBackupClass_new(PROGRESS_TYPE_SETTINGS) {

    override val className: String = "SettingsBackupEngine"
    private val settingsFile by lazy { FileX.new(fileXDestination, BACKUP_NAME_SETTINGS) }

    override suspend fun backgroundProcessing(): Any {

        try {

            FileX.new(fileXDestination).mkdirs()

            val title = getTitle(R.string.writing_settings)

            resetBroadcast(true, title)
            settingsFile.createNewFile(overwriteIfExists = true)
            settingsFile.refreshFile()

            val jsonObject = JSONObject()
            dpiText?.let { jsonObject.put(JSON_FIELD_DPI_TEXT, it.trim()) }
            adbState?.let { jsonObject.put(JSON_FIELD_ADB_TEXT, it) }
            fontScale?.let { jsonObject.put(JSON_FIELD_FONT_SCALE, it) }
            keyboardText?.let { jsonObject.put(JSON_FIELD_KEYBOARD_TEXT, it.trim()) }

            heavyTask {
                settingsFile.writeOneLine(jsonObject.toString(4))
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_SETTINGS_TRY_CATCH: ${e.message}")
        }

        return settingsFile
    }
}