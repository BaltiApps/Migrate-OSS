package balti.migrate.restore.ui.screens.progressScreen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.components.ButtonStatus
import balti.migrate.common.ui.components.KeepScreenOn
import balti.migrate.common.ui.components.LoadingProgressBar
import balti.migrate.common.ui.components.NextFab
import balti.migrate.common.ui.progressScreen.ErrorLayoutToggle
import balti.migrate.common.ui.progressScreen.ProgressLogLayout
import balti.migrate.common.ui.progressScreen.ProgressScreenBottomBar
import balti.migrate.common.ui.progressScreen.ScrollAnchor
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RestoreProgressScreen(
    viewModel: RestoreProgressScreenViewModel = koinViewModel(),
    cancelRestore: () -> Unit,
    closeProgressScreen: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val activity = LocalActivity.current

    Content(
        state = { state },
        onToggleErrorOnly = {
            viewModel.performAction(RestoreProgressScreenAction.ToggleErrorOnly(it))
        },
        cancelRestore = {
            viewModel.performAction(RestoreProgressScreenAction.CancelRestore)
            cancelRestore()
        },
        pauseLogs = {
            viewModel.performAction(RestoreProgressScreenAction.PauseProgressLogs)
        },
        resumeLogs = {
            viewModel.performAction(RestoreProgressScreenAction.ResumeProgressLogs)
        },
        changeSmsApp = {
            Toast.makeText(activity, R.string.toast_text_change_sms_app, Toast.LENGTH_SHORT).show()
            viewModel.performAction(RestoreProgressScreenAction.ChangeSmsApp(activity))
        },
        closeProgressScreen = closeProgressScreen,
    )

    BackHandler { closeProgressScreen() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: () -> RestoreProgressScreenState,
    onToggleErrorOnly: (Boolean) -> Unit,
    cancelRestore: () -> Unit,
    pauseLogs: () -> Unit,
    resumeLogs: () -> Unit,
    changeSmsApp: () -> Unit,
    closeProgressScreen: () -> Unit,
) {
    var scrollAnchor by remember { mutableStateOf(ScrollAnchor.BOTTOM) }
    val items = if (state().errorOnly) state().errorList else state().progressList

    KeepScreenOn(
        shouldKeepScreenOn = !state().isRestoreFinished,
    )

    SmsAppChangeDialog(
        shouldShow = state().shouldChangeSmsApp,
        onAgree = changeSmsApp
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("")
                },
                navigationIcon = {
                    IconButton(
                        onClick = closeProgressScreen
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            ProgressScreenBottomBar(
                onScrollToTop = {
                    scrollAnchor = ScrollAnchor.TOP
                },
                onScrollToBottom = {
                    scrollAnchor = ScrollAnchor.BOTTOM
                },
                fabContent = {
                    val buttonStatus = when {
                        state().isRestoreFinished ->
                            ButtonStatus.Success(stringResource(R.string.done), closeProgressScreen)
                        state().isCancelling ->
                            ButtonStatus.Loading(stringResource(R.string.cancel)) {}
                        else -> ButtonStatus.Error(stringResource(R.string.cancel), cancelRestore)
                    }
                    NextFab(buttonStatus)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ErrorLayoutToggle(
                isErrorOnly = state().errorOnly,
                onToggleErrorOnly = onToggleErrorOnly,
                modifier = Modifier.fillMaxWidth()
            )
            if (state().isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingProgressBar(progress = null)
                }
                return@Column
            }
            ProgressLogLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F),
                items = items,
                pauseLogs = {
                    pauseLogs()
                    scrollAnchor = ScrollAnchor.INDETERMINATE
                },
                resumeLogs = resumeLogs,
                scrollAnchor = scrollAnchor,
                isFinished = state().isRestoreFinished
            )
        }
    }
}