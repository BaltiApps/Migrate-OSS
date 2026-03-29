package balti.migrate.backup.ui.screens.backupName

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.BottomAppBar
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
import balti.migrate.common.ui.components.LoadingDialog
import balti.migrate.common.ui.components.LocationSelector
import balti.migrate.common.ui.components.NextFab
import balti.migrate.common.utils.PermissionUtils
import balti.migrate.common.utils.getDefaultBackupName
import balti.migrate.restore.ui.screens.restoreSummary.components.SimpleYesNoDialog
import baltiapps.migrate.domain.backup.model.BackupLocation
import baltiapps.migrate.domain.common.utils.StringUtils.getHumanReadableSize
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
        onStartBackup = {
            viewModel.onAction(BackupNameAction.StartBackup(goToNextScreen))
        },
        onBackupNameChanged = {
            viewModel.onAction(BackupNameAction.NameChanged(it))
        },
        onUriSelectClicked = PermissionUtils.requestSafLocation {
            viewModel.onAction(BackupNameAction.OnSafLocationSelected(it))
        },
        onDismissNoSpaceDialog = {
            viewModel.onAction(BackupNameAction.DismissNoSpaceDialog)
        }
    )
}

@Composable
private fun Content(
    state: () -> BackupNameState,
    navigateUp: () -> Unit,
    onStartBackup: () -> Unit,
    onBackupNameChanged: (String) -> Unit,
    onUriSelectClicked: () -> Unit,
    onDismissNoSpaceDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state().isScanningAppSizes) {
        LoadingDialog(
            text = "${stringResource(R.string.checking_app_sizes)}\n${state().appSizeScanProgress?.displayText}",
            maxLines = 2,
            progress = state().appSizeScanProgress,
        )
    }

    if (state().shouldShowNoSpaceDialog) {
        SimpleYesNoDialog(
            titleText = stringResource(R.string.insufficient_storage_title),
            dialogText = stringResource(
                R.string.insufficient_storage_message,
                getHumanReadableSize(state().requiredSpaceBytes),
                getHumanReadableSize(state().availableSpaceBytes)
            ),
            positiveButtonLabel = stringResource(R.string.see_sizes),
            negativeButtonLabel = stringResource(android.R.string.cancel),
            icon = Icons.Outlined.Storage,
            onPositiveButton = {  },
            onNegativeButton = onDismissNoSpaceDialog,
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                navigateUp = navigateUp,
            )
        },
        bottomBar = {
            BottomBar(
                onStartBackup = onStartBackup,
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
                val isSaf = state().isSaf
                if (isSaf != null) {
                    LocationSelector(
                        isFallback = !isSaf,
                        locationLabel = state().locationString,
                        isLocationAccessible = !isSaf || state().isSafUriAccessible,
                        onSelectClicked = onUriSelectClicked
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
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
    onStartBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomAppBar(
        modifier = modifier,
        actions = {},
        floatingActionButton = {
            val buttonStatus = ButtonStatus.Unspecified(
                label = stringResource(R.string.start),
                onPressed = onStartBackup
            )
            NextFab(
                buttonStatus = buttonStatus
            )
        }
    )
}
