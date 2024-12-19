package balti.migrate.backup.ui.screens.listScreen.contactBackup

import balti.migrate.backup.ui.screens.listScreen.ListScreenGenericViewModel
import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.usecase.ReadContactsUseCase
import baltiapps.migrate.domain.backup.usecase.StageSelectedContacts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ContactBackupViewModel(
    private val readContactsUseCase: ReadContactsUseCase,
    private val stageSelectedContacts: StageSelectedContacts,
) : ListScreenGenericViewModel<ContactListItem>() {

    private val _state = MutableStateFlow(ContactBackupState())
    val state = _state.asStateFlow()

    override fun getListFromState(): List<ContactListItem> {
        return _state.value.contactList
    }

    override fun updateStateWithProgress(progress: Progress) {
        _state.update {
            it.copy(progress = progress)
        }
    }

    override fun updateStateStaging(isStaging: Boolean) {
        _state.update {
            it.copy(isStaging = isStaging)
        }
    }

    override fun updateStateWithItems(list: List<ContactListItem>) {
        _state.update {
            it.copy(contactList = list)
        }
    }

    init {
        super.readItems(
            reader = readContactsUseCase::read,
            getReadItems = readContactsUseCase::getReadContacts,
        )
    }

    fun performAction(action: ContactBackupAction) {
        when (action) {
            is ContactBackupAction.ToggleContactItem -> {
                super.toggleItem(action.item)
            }
            is ContactBackupAction.ToggleAllContacts -> {
                super.toggleAll(action.isChecked)
            }
            is ContactBackupAction.StageContacts -> {
                super.stageItems(
                    stagingBlock = stageSelectedContacts::invoke,
                    onStagingDone = { action.onStagingDone() }
                )
            }
        }
    }
}