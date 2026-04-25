package balti.migrate.restore.ui.screens.listScreen.contactRestoreSelection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.components.CountBar
import balti.migrate.common.ui.components.RenderContactItem
import balti.migrate.common.ui.listScreen.ListScreenShell
import balti.migrate.common.ui.listScreen.ListState
import baltiapps.migrate.domain.common.model.ContactListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactRestoreSelection(
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    viewModel: ContactRestoreSelectionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Content(
        state = { state },
        navigateUp = navigateUp,
        onSelectAll = {
            viewModel.onAction(ContactRestoreSelectionAction.ToggleAllContacts(true))
        },
        onDeselectAll = {
            viewModel.onAction(ContactRestoreSelectionAction.ToggleAllContacts(false))
        },
        onItemToggled = {
            viewModel.onAction(ContactRestoreSelectionAction.ToggleContactItem(it))
        },
        onNext = {
            viewModel.onAction(ContactRestoreSelectionAction.StageContacts(goToNextScreen))
        },
        onSkip = goToNextScreen,
    )

    BackHandler { navigateUp() }
}

@Composable
private fun Content(
    state: () -> ContactRestoreSelectionState,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onItemToggled: (ContactListItem) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val listState = ListState(
        listTitle = stringResource(R.string.label_contacts_restore),
        isStaging = state().isStaging,
        hasPermission = true,
        permissionDescription = "",
        progress = state().progress,
        hasNoData = state().contactListItems.isEmpty(),
    )
    ListScreenShell(
        listState = listState,
        navigateUp = navigateUp,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll,
        onPermissionRequest = {},
        onNext = onNext,
        onSkip = onSkip,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            stickyHeader {
                val allItems = state().contactListItems
                val selectedItems = allItems.filter { it.isChecked }
                CountBar(
                    totalCount = allItems.size,
                    selectedCount = selectedItems.size,
                )
            }
            items(
                items = state().contactListItems,
                key = { it._id }
            ) { item ->
                RenderContactItem(
                    item = item,
                    enabled = !listState.isStaging,
                    onItemToggled = onItemToggled,
                )
            }
        }
    }
}