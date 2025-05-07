package balti.migrate.backup.ui.screens.backupName

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.components.ButtonStatus
import balti.migrate.common.ui.components.NextFab
import balti.migrate.common.utils.PermissionUtils
import balti.migrate.common.utils.getDefaultBackupName
import baltiapps.migrate.domain.DEFAULT_BACKUP_ROOT
import baltiapps.migrate.domain.backup.model.BackupLocation
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BackupName(
    navigateUp: () -> Unit,
    goToNextScreen: (BackupLocation) -> Unit,
    viewModel: BackupNameViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Content(
        state = { state },
        navigateUp = navigateUp,
        goToNextScreen = goToNextScreen,
        onBackupNameChanged = {
            viewModel.onAction(BackupNameAction.NameChanged(it))
        },
        onUriSelectClicked = PermissionUtils.requestSafLocation {
            viewModel.onAction(BackupNameAction.OnSafLocationSelected(it))
        }
    )
}

@Composable
private fun Content(
    state: () -> BackupNameState,
    navigateUp: () -> Unit,
    goToNextScreen: (BackupLocation) -> Unit,
    onBackupNameChanged: (String) -> Unit,
    onUriSelectClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                navigateUp = navigateUp,
            )
        },
        bottomBar = {
            BottomBar(
                backupName = state().backupName,
                backupUriString = state().safUriString,
                goToNextScreen = goToNextScreen,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state().backupName,
                    onValueChange = {
                        onBackupNameChanged(it)
                    },
                    label = {
                        Text(stringResource(R.string.enter_backup_name))
                    },
                    placeholder = {
                        Text(getDefaultBackupName())
                    }
                )
            }
            SafUriSelector(
                state = state(),
                onUriSelectClicked = onUriSelectClicked,
            )
        }
    }
}

@Composable
private fun SafUriSelector(
    state: BackupNameState,
    onUriSelectClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.safUriString != null && state.isSafUriAccessible == true) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Column(
                modifier = Modifier.weight(1F),
            ) {
                Text(
                    text = if (state.isSafUriAccessible == false) {
                        stringResource(R.string.backup_location_not_accessible)
                    } else {
                        stringResource(R.string.setup_backup_location)
                    }
                )
                Spacer(Modifier.size(8.dp))
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = onUriSelectClicked
                ) {
                    Text(stringResource(R.string.setup))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(stringResource(R.string.backup_info))
        },
        navigationIcon = {
            IconButton(
                onClick = navigateUp
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.go_back)
                )
            }
        }
    )
}

@Composable
private fun BottomBar(
    backupName: String,
    backupUriString: String?,
    goToNextScreen: (BackupLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomAppBar(
        modifier = modifier,
        actions = {},
        floatingActionButton = {
            val buttonStatus = ButtonStatus.Unspecified(
                label = stringResource(R.string.start),
                onPressed = {
                    goToNextScreen(
                        BackupLocation(
                            backupName = backupName,
                            backupUriString = backupUriString,
                        )
                    )
                }
            )
            NextFab(
                buttonStatus = buttonStatus
            )
        }
    )
}