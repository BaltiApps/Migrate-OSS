package balti.migrate.backup.ui.screens.listScreen.contactBackupSelection

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.listScreen.ListScreenShell
import balti.migrate.common.ui.components.RenderContactItem
import baltiapps.migrate.domain.common.model.ContactListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactBackupSelection(
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    viewModel: ContactBackupSelectionViewModel = koinViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.state.collectAsStateWithLifecycle(
        lifecycleOwner = lifecycleOwner,
    )

    val activity = LocalActivity.current

    Content(
        state = { state },
        navigateUp = navigateUp,
        onSelectAll = {
            viewModel.performAction(ContactBackupSelectionAction.ToggleAllContacts(true))
        },
        onDeselectAll = {
            viewModel.performAction(ContactBackupSelectionAction.ToggleAllContacts(false))
        },
        requestPermission = {
            viewModel.performAction(ContactBackupSelectionAction.RequestPermission(activity))
        },
        onItemToggled = {
            viewModel.performAction(ContactBackupSelectionAction.ToggleContactItem(it))
        },
        onNext = {
            viewModel.performAction(ContactBackupSelectionAction.StageContacts(goToNextScreen))
        }
    )
}

@Composable
private fun Content(
    state: () -> ContactBackupSelectionState,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    requestPermission: () -> Unit,
    onItemToggled: (item: ContactListItem) -> Unit,
    onNext: () -> Unit,
) {
    val isStaging = state().isStaging
    ListScreenShell(
        backupTitle = stringResource(R.string.label_contacts_backup),
        navigateUp = navigateUp,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll,
        isStaging = isStaging,
        loadingProgress = state().progress,
        isPermissionGranted = state().hasPermission,
        requestPermission = requestPermission,
        onNext = onNext,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = state().contactList,
                    key = {
                        it._id
                    }
                ) { item ->
                    RenderContactItem(
                        item = item,
                        enabled = !isStaging,
                        onItemToggled = onItemToggled,
                    )
                }
            }
        }
    }
}