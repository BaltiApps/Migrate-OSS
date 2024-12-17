package balti.migrate.backup.ui.screens.listScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.backup.model.ListItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.model.SmsListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class ListSubScreenViewModel<T: ListItem>: ViewModel() {

    abstract fun getListFromState(): List<T>
    abstract fun updateStateWithItems(list: List<T>)
    abstract fun updateStateWithProgress(progress: Progress)
    abstract fun updateStateStaging(isStaging: Boolean)

    fun readItems(
        reader: suspend () -> Flow<Progress>,
        getReadItems: () -> List<T>,
    ) {
        viewModelScope.launch {
            reader().onCompletion {
                updateStateWithItems(getReadItems())
            }.collect { progress ->
                updateStateWithProgress(progress)
            }
        }
    }

    fun stageItems(
        stagingBlock: (List<T>) -> Unit,
        onStagingDone: () -> Unit,
    ) {
        viewModelScope.launch {
            updateStateStaging(true)
            withContext(Dispatchers.IO) {
                stagingBlock(getListFromState())
            }
            updateStateStaging(false)
            onStagingDone()
        }
    }

    fun toggleAll(isChecked: Boolean) {
        val newList = getListFromState().mapNotNull {
            when (it) {
                is ContactListItem -> it.copy(isChecked = isChecked)
                is CallLogListItem -> it.copy(isChecked = isChecked)
                is SmsListItem -> it.copy(isChecked = isChecked)
                else -> null
            }
        }
        updateStateWithItems(newList as List<T>)
    }

    fun toggleItem(item: T) {
        val newItem = when(item) {
            is ContactListItem -> item.copy(isChecked = !item.isChecked)
            is CallLogListItem -> item.copy(isChecked = !item.isChecked)
            is SmsListItem -> item.copy(isChecked = !item.isChecked)
            else -> null
        } ?: return
        replaceListItem(
            list = getListFromState(),
            item = item,
            newItem = newItem,
        ).run {
            updateStateWithItems(this as List<T>)
        }
    }

    private fun <T: ListItem> replaceListItem(
        list: List<T>,
        item: T,
        newItem: T,
    ): List<T> {
        return list.toMutableList().apply {
            val index = indexOf(item)
            if (index != -1) {
                this[index] = newItem
            }
        }
    }
}