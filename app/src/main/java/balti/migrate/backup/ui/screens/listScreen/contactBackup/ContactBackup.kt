package balti.migrate.backup.ui.screens.listScreen.contactBackup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.ListScreenShell
import baltiapps.migrate.domain.common.model.ContactListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactBackup(
    navigateUp: () -> Unit,
    goToNextScreen: () -> Unit,
    viewModel: ContactBackupViewModel = koinViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.state.collectAsStateWithLifecycle(
        lifecycleOwner = lifecycleOwner,
    )

    Content(
        state = { state },
        navigateUp = navigateUp,
        onSelectAll = {
            viewModel.performAction(ContactBackupAction.ToggleAllContacts(true))
        },
        onDeselectAll = {
            viewModel.performAction(ContactBackupAction.ToggleAllContacts(false))
        },
        onItemToggled = {
            viewModel.performAction(ContactBackupAction.ToggleContactItem(it))
        },
        onNext = {
            viewModel.performAction(ContactBackupAction.StageContacts(goToNextScreen))
        }
    )
}

@Composable
private fun Content(
    state: () -> ContactBackupState,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onItemToggled: (item: ContactListItem) -> Unit,
    onNext: () -> Unit,
) {
    val isLoading = state().progress.percentage < 1.0
    ListScreenShell(
        backupTitle = stringResource(R.string.label_contacts_backup),
        navigateUp = navigateUp,
        onSelectAll = onSelectAll,
        onDeselectAll = onDeselectAll,
        isLoading = isLoading,
        isStaging = state().isStaging,
        onNext = onNext,
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    progress = { state().progress.percentage.toFloat() },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state().contactList,
                        key = {
                            it._id
                        }
                    ) { item ->
                        ListItem(
                            modifier = Modifier.clickable {
                                onItemToggled(item)
                            },
                            headlineContent = {
                                Text(
                                    text = item.displayName.takeIf { it.isNotBlank() }
                                        ?: item.displayNumber
                                )
                            },
                            supportingContent = {
                                if (item.displayName.isNotBlank()) {
                                    Text(item.displayNumber)
                                }
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = null,
                                    enabled = !state().isStaging
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}