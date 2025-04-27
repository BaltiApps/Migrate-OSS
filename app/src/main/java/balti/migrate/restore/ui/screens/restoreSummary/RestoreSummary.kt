package balti.migrate.restore.ui.screens.restoreSummary

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.components.ButtonStatus
import balti.migrate.common.ui.components.NextFab
import balti.migrate.restore.ui.screens.restoreSummary.components.DelegatedRestore
import balti.migrate.restore.ui.screens.restoreSummary.components.LoadingDialog
import balti.migrate.restore.ui.screens.restoreSummary.components.SpecialPermissions
import balti.migrate.restore.ui.screens.restoreSummary.components.MigrateRestore
import balti.migrate.restore.ui.screens.restoreSummary.components.SimpleYesNoDialog
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RestoreSummary(
    navigateUp: () -> Unit,
    startRestoreServiceAndGoToNextScreen: () -> Unit,
    viewModel: RestoreSummaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val activity = LocalActivity.current

    LaunchedEffect(state.isInitialized) {
        if (!state.isInitialized) return@LaunchedEffect

        val taskMap = mapOf(
            ExportContactsTask::class.simpleName to ExportContactsTask(),
            ShowDialogForContactsTask::class.simpleName to ShowDialogForContactsTask(),
            LaunchContactAppTask::class.simpleName to LaunchContactAppTask(
                getActivity = { activity },
            ),
            ShowDialogForSms::class.simpleName to ShowDialogForSms(),
            SetAsDefaultSmsAppTask::class.simpleName to SetAsDefaultSmsAppTask(
                getActivity = { activity },
            ),
            StartRestoreServiceTask::class.simpleName to StartRestoreServiceTask(
                runService = startRestoreServiceAndGoToNextScreen,
            ),
        )
        viewModel.onAction(RestoreSummaryAction.SetTaskMap(taskMap))
    }

    Content(
        state = { state },
        onStartRestore = { viewModel.onAction(RestoreSummaryAction.StartRestore) },
        navigateUp = navigateUp,
    )

    val contactsExportingPercentage = state.contactsExportProgress.percentage

    BackHandler { navigateUp() }

    if (state.countContacts > 0
        && contactsExportingPercentage > 0
        && contactsExportingPercentage < 1.0) {
        LoadingDialog(
            text = stringResource(R.string.exporting_percentage, (contactsExportingPercentage * 100).toInt()),
            progress = state.contactsExportProgress,
        )
    } else if (state.contactsUserActionState == UserActionState.ACTION_PROMPT) {
        SimpleYesNoDialog(
            dialogText = stringResource(R.string.contacts_delegate_description),
            onProceed = {
                viewModel.onAction(RestoreSummaryAction.ProceedWithContacts)
            },
            onSkip = {
                viewModel.onAction(RestoreSummaryAction.SkipContacts)
            },
            icon = Icons.Outlined.Contacts,
        )
    } else if (state.smsUserActionState == UserActionState.ACTION_PROMPT) {
        SimpleYesNoDialog(
            dialogText = stringResource(R.string.default_sms_app_description),
            onProceed = {
                viewModel.onAction(RestoreSummaryAction.ProceedWithSms)
            },
            onSkip = {
                viewModel.onAction(RestoreSummaryAction.SkipSms)
            },
            icon = Icons.Outlined.Sms,
        )
    }
}

@Composable
private fun Content(
    state: () -> RestoreSummaryState,
    onStartRestore: () -> Unit,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBar(navigateUp = navigateUp)
        },
        bottomBar = {
            BottomBar(onStartRestore = onStartRestore)
        },
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            DelegatedRestore(
                state = state(),
                modifier = Modifier.fillMaxWidth(),
            )
            SpecialPermissions(
                state = state(),
                modifier = Modifier.fillMaxWidth(),
            )
            MigrateRestore(
                state = state(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BottomBar(
    onStartRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier,
        actions = {},
        floatingActionButton = {
            NextFab(
                buttonStatus = ButtonStatus.Unspecified(
                    label = stringResource(R.string.start),
                    onPressed = onStartRestore
                )
            )
        }
    )
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
            Text(text = stringResource(R.string.restore_summary))
        },
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.go_back),
                )
            }
        }
    )
}

@Preview
@Composable
private fun ContentPreview() {
    Content(
        state = {
            RestoreSummaryState(
                countContacts = 5,
                countCallLogs = 10,
                countSms = 15,
                contactsUserActionState = UserActionState.ACTION_PROCEED,
                smsUserActionState = UserActionState.ACTION_AWAITING,
            )
        },
        onStartRestore = {},
        navigateUp = {},
    )
}