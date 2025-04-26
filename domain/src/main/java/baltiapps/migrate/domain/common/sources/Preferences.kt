package baltiapps.migrate.domain.common.sources

import baltiapps.migrate.domain.common.model.Progress

interface Preferences {
    companion object {
        const val KEY_SAVED_PROGRESS_LIST = "last_saved_progress"
        const val KEY_SAVED_ERROR_LIST = "last_saved_errors"
    }

    fun saveProgressList(list: List<Progress>)
    fun getLastSavedProgressList() : List<Progress>

    fun saveErrorList(list: List<Progress>)
    fun getLastSavedErrorList() : List<Progress>

    fun resetSavedProgressList()
    fun resetSavedErrorList()
}