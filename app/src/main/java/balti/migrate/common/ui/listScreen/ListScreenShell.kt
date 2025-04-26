package balti.migrate.common.ui.listScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import balti.migrate.R
import balti.migrate.common.ui.components.ButtonStatus
import balti.migrate.common.ui.components.NextFab
import baltiapps.migrate.domain.common.model.Progress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreenShell(
    listState: ListState,
    navigateUp: () -> Unit,
    onSelectAll: (() -> Unit)?,
    onDeselectAll: (() -> Unit)?,
    onPermissionRequest: () -> Unit,
    onNext: () -> Unit,
    nextButtonCustomLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(listState.listTitle, scrollBehavior, navigateUp)
        },
        bottomBar = {
            BottomBar(
                onSelectAll = onSelectAll,
                onDeselectAll = onDeselectAll,
                isLoading = listState.isLoading,
                isStaging = listState.isStaging,
                hasPermission = listState.hasPermission,
                hasNoData = listState.hasNoData,
                onNext = onNext,
                nextButtonCustomLabel = nextButtonCustomLabel,
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !listState.hasPermission -> ListPermissionRequestLayout(
                    listState.permissionDescription,
                    onPermissionRequest,
                    onNext
                )

                listState.isLoading -> ListLoadingLayout(listState.progress)
                listState.hasNoData -> ListNoDataLayout(
                    title = listState.customNoDataMessage
                )
                else -> content()
            }
        }
    }
}

data class ListState(
    val listTitle: String,
    val isStaging: Boolean,
    val hasPermission: Boolean,
    val permissionDescription: String,
    val progress: Progress,
    val hasNoData: Boolean,
    val customNoDataMessage: String? = null,
) {
    val loadingProgress: Double
        get() = progress.percentage
    val isLoading: Boolean
        get() = hasPermission && loadingProgress < 1.0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    backupTitle: String,
    scrollBehavior: TopAppBarScrollBehavior,
    navigateUp: () -> Unit,
) {
    LargeTopAppBar(
        title = {
            Text(backupTitle)
        },
        navigationIcon = {
            IconButton(
                onClick = { navigateUp() },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.go_back)
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}


@Composable
private fun BottomBar(
    onSelectAll: (() -> Unit)?,
    onDeselectAll: (() -> Unit)?,
    isLoading: Boolean,
    isStaging: Boolean,
    hasPermission: Boolean,
    hasNoData: Boolean,
    onNext: () -> Unit,
    nextButtonCustomLabel: String? = null,
) {
    val shouldShowSkip = !hasPermission || isLoading || hasNoData
    val nextButtonLabel = when {
        nextButtonCustomLabel != null -> nextButtonCustomLabel
        shouldShowSkip -> stringResource(R.string.skip)
        else -> stringResource(R.string.next)
    }
    BottomAppBar(
        actions = {
            if (!shouldShowSkip && onSelectAll != null) {
                IconButton(
                    modifier = Modifier
                        .padding(4.dp)
                        .padding(bottom = 6.dp),
                    onClick = onSelectAll
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DoneAll,
                        contentDescription = stringResource(R.string.select_all)
                    )
                }
            }
            if (!shouldShowSkip && onDeselectAll != null) {
                IconButton(
                    modifier = Modifier
                        .padding(4.dp)
                        .padding(bottom = 6.dp),
                    onClick = onDeselectAll
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ClearAll,
                        contentDescription = stringResource(R.string.deselect_all)
                    )
                }
            }
        },
        floatingActionButton = {
            val buttonStatus = if (isStaging) {
                ButtonStatus.Loading(nextButtonLabel) {}
            } else ButtonStatus.Unspecified(nextButtonLabel, onNext)
            NextFab(
                buttonStatus = buttonStatus
            )
        }
    )
}