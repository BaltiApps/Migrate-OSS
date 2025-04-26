package baltiapps.migrate.domain.common.sources

import baltiapps.migrate.domain.common.model.Progress

interface Preferences {
    companion object {
        const val KEY_SAVED_BACKUP_PROGRESS_LIST = "last_saved_backup_progress"
        const val KEY_SAVED_BACKUP_ERROR_LIST = "last_saved_backup_errors"
        const val KEY_SAVED_RESTORE_PROGRESS_LIST = "last_saved_restore_progress"
        const val KEY_SAVED_RESTORE_ERROR_LIST = "last_saved_restore_errors"
    }

    fun saveBackupProgressList(list: List<Progress>)
    fun getLastSavedBackupProgressList() : List<Progress>

    fun saveBackupErrorList(list: List<Progress>)
    fun getLastSavedBackupErrorList() : List<Progress>

    fun resetSavedBackupProgressList()
    fun resetSavedBackupErrorList()

    fun saveRestoreProgressList(list: List<Progress>)
    fun getLastSavedRestoreProgressList() : List<Progress>

    fun saveRestoreErrorList(list: List<Progress>)
    fun getLastSavedRestoreErrorList() : List<Progress>

    fun resetSavedRestoreProgressList()
    fun resetSavedRestoreErrorList()
}