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
}