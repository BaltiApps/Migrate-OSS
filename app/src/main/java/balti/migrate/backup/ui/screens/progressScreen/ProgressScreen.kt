package balti.migrate.backup.ui.screens.progressScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.backup.ui.components.ButtonStatus
import balti.migrate.backup.ui.components.NextFab
import baltiapps.migrate.domain.backup.model.Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProgressScreen(
    viewModel: ProgressScreenViewModel = koinViewModel(),
    cancelBackup: () -> Unit,
    closeProgressScreen: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val state by viewModel.state.collectAsStateWithLifecycle(
        lifecycleOwner = lifecycleOwner
    )

    Content(
        scope = coroutineScope,
        state = { state },
        onToggleErrorOnly = {
            viewModel.performAction(ProgressScreenAction.ToggleErrorOnly(it))
        },
        cancelBackup = {
            viewModel.performAction(ProgressScreenAction.CancelBackup)
            cancelBackup()
        },
        closeProgressScreen = closeProgressScreen,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    scope: CoroutineScope,
    state: () -> ProgressScreenState,
    onToggleErrorOnly: (Boolean) -> Unit,
    cancelBackup: () -> Unit,
    closeProgressScreen: () -> Unit,
) {
    var shouldAutoScroll by rememberSaveable { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val items = state().progressList
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
            BottomBar(
                state = state,
                scrollToTop = {
                    scope.launch {
                        shouldAutoScroll = false
                        scrollToTop(listState)
                    }
                },
                scrollToBottom = {
                    scope.launch {
                        scrollToBottom(listState, items)
                        shouldAutoScroll = true
                    }
                },
                cancelBackup = cancelBackup,
                closeProgressScreen = closeProgressScreen,
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
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.show_only_errors))
                Switch(
                    checked = state().errorOnly,
                    onCheckedChange = {
                        onToggleErrorOnly(it)
                    }
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1F)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    shouldAutoScroll = false
                                }
                            }
                        }
                    }
            ) {
                items(
                    items = items,
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = it.logs,
                        color = if (it.isFailure) {
                            MaterialTheme.colorScheme.error
                        } else Color.Unspecified
                    )
                }
            }
            LaunchedEffect(items) {
                scope.launch {
                    if (shouldAutoScroll) {
                        scrollToBottom(listState, items)
                    }
                }
            }
        }
    }
}

suspend fun scrollToBottom(
    listState: LazyListState,
    items: List<Progress>,
) {
    if (items.isNotEmpty()) {
        listState.scrollToItem(items.size - 1)
    }
}

suspend fun scrollToTop(
    listState: LazyListState,
) {
    listState.scrollToItem(0)
}

@Composable
private fun BottomBar(
    state: () -> ProgressScreenState,
    scrollToTop: () -> Unit,
    scrollToBottom: () -> Unit,
    cancelBackup: () -> Unit,
    closeProgressScreen: () -> Unit,
) {
    val isBackupFinished = state().isBackupFinished
    val isCancelling = state().isCancelling
    BottomAppBar(
        actions = {
            IconButton(
                onClick = scrollToTop,
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = stringResource(
                        R.string.scroll_up
                    )
                )
            }
            IconButton(
                onClick = scrollToBottom,
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(
                        R.string.scroll_down
                    )
                )
            }
        },
        floatingActionButton = {
            val buttonStatus = when {
                isBackupFinished ->
                    ButtonStatus.Success(stringResource(R.string.done), closeProgressScreen)
                isCancelling ->
                    ButtonStatus.Loading(stringResource(R.string.cancel)) {}
                else -> ButtonStatus.Error(stringResource(R.string.cancel), cancelBackup)
            }
            NextFab(buttonStatus)
        }
    )
}