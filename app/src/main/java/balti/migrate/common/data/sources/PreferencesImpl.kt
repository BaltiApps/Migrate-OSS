package balti.migrate.common.data.sources

import android.content.Context
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.Preferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PreferencesImpl(
    private val applicationContext: Context,
) : Preferences {

    private val sharedPreferences by lazy {
        applicationContext.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    }

    private val editor by lazy {
        sharedPreferences.edit()
    }

    override fun saveBackupProgressList(list: List<Progress>) {
        editor.putString(Preferences.KEY_SAVED_BACKUP_PROGRESS_LIST, Json.encodeToString(list))
        editor.apply()
    }

    override fun getLastSavedBackupProgressList(): List<Progress> {
        val stringProgressList = sharedPreferences.getString(
            Preferences.KEY_SAVED_BACKUP_PROGRESS_LIST,
            ""
        ) ?: ""
        return Json.decodeFromString(stringProgressList)
    }

    override fun saveBackupErrorList(list: List<Progress>) {
        editor.putString(Preferences.KEY_SAVED_BACKUP_ERROR_LIST, Json.encodeToString(list))
        editor.apply()
    }

    override fun getLastSavedBackupErrorList(): List<Progress> {
        val stringErrorList = sharedPreferences.getString(
            Preferences.KEY_SAVED_BACKUP_ERROR_LIST,
            ""
        ) ?: ""
        return Json.decodeFromString(stringErrorList)
    }

    override fun resetSavedBackupProgressList() {
        saveBackupProgressList(emptyList())
    }

    override fun resetSavedBackupErrorList() {
        saveBackupErrorList(emptyList())
    }

    override fun saveRestoreProgressList(list: List<Progress>) {
        editor.putString(Preferences.KEY_SAVED_RESTORE_PROGRESS_LIST, Json.encodeToString(list))
        editor.apply()
    }

    override fun getLastSavedRestoreProgressList(): List<Progress> {
        val stringProgressList = sharedPreferences.getString(
            Preferences.KEY_SAVED_RESTORE_PROGRESS_LIST,
            ""
        ) ?: ""
        return Json.decodeFromString(stringProgressList)
    }

    override fun saveRestoreErrorList(list: List<Progress>) {
        editor.putString(Preferences.KEY_SAVED_RESTORE_ERROR_LIST, Json.encodeToString(list))
        editor.apply()
    }

    override fun getLastSavedRestoreErrorList(): List<Progress> {
        val stringErrorList = sharedPreferences.getString(
            Preferences.KEY_SAVED_RESTORE_ERROR_LIST,
            ""
        ) ?: ""
        return Json.decodeFromString(stringErrorList)
    }

    override fun resetSavedRestoreProgressList() {
        saveRestoreProgressList(emptyList())
    }

    override fun resetSavedRestoreErrorList() {
        saveRestoreErrorList(emptyList())
    }

    override fun shouldShowPermissionScreen(): Boolean {
        return sharedPreferences.getBoolean(Preferences.KEY_SHOULD_SHOW_PERMISSION_SCREEN, true)
    }

    override fun setShouldShowPermissionScreen(value: Boolean) {
        editor.putBoolean(Preferences.KEY_SHOULD_SHOW_PERMISSION_SCREEN, value)
        editor.apply()
    }
}