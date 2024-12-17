package balti.migrate.backup.ui.screens.listScreen.contactBackup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import baltiapps.migrate.domain.backup.model.ContactListItem
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactBackup(
    isLoading: (Boolean) -> Unit,
    setStagingBlock: (() -> Unit) -> Unit,
    onPostStaging: () -> Unit,
    setToggleAll: ((Boolean) -> Unit) -> Unit,
    viewModel: ContactBackupViewModel = koinViewModel(),
) {
    setStagingBlock {
        viewModel.performAction(ContactBackupAction.StageContacts(onPostStaging))
    }
    setToggleAll {
        viewModel.performAction(ContactBackupAction.ToggleAllContacts(it))
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.state.collectAsStateWithLifecycle(
        lifecycleOwner = lifecycleOwner,
    )

    Content(
        state = { state },
        isLoading = isLoading,
        onItemToggled = { item ->
            viewModel.performAction(
                ContactBackupAction.ToggleContactItem(item)
            )
        }
    )
}

@Composable
private fun Content(
    state: () -> ContactBackupState,
    isLoading: (Boolean) -> Unit,
    onItemToggled: (ContactListItem) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (state().progress.percentage < 1.0) {
            isLoading(true)
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
                isLoading(false)
            }
        }
    }
}