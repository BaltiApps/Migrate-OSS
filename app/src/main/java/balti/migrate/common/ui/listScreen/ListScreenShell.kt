package balti.migrate.common.ui.listScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import balti.migrate.R
import balti.migrate.common.ui.components.ButtonStatus
import balti.migrate.common.ui.components.LoadingProgressBar
import balti.migrate.common.ui.components.NextFab
import baltiapps.migrate.domain.common.model.Progress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreenShell(
    backupTitle: String,
    navigateUp: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    isStaging: Boolean,
    loadingProgress: Progress,
    isPermissionGranted: Boolean = true,
    requestPermission: (() -> Unit)? = null,
    onNext: () -> Unit,
    content: @Composable (paddingValues: PaddingValues) -> Unit,
) {
    val isLoading = loadingProgress.percentage < 1.0 && loadingProgress.percentage > 0.0
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(backupTitle, scrollBehavior, navigateUp)
        },
        bottomBar = {
            BottomBar(
                onSelectAll = onSelectAll,
                onDeselectAll = onDeselectAll,
                isLoading = isLoading,
                isStaging = isStaging,
                onNext = onNext,
            )
        }
    ) { paddingValues ->
        when {
            !isPermissionGranted -> requestPermission?.let { ShowPermissionRequest(it, paddingValues) }
            isLoading -> ShowLoading(loadingProgress, paddingValues)
            else -> content(paddingValues)
        }
    }
}

@Composable
private fun ShowLoading(loadingProgress: Progress, paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(),
    ) {
        LoadingProgressBar(
            modifier = Modifier.align(Alignment.Center),
            progress = loadingProgress
        )
    }
}

@Composable
private fun ShowPermissionRequest(requestPermission: () -> Unit, paddingValues: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = requestPermission
        ) {
            Text(text = "Request Permission")
        }
    }
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
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    isLoading: Boolean,
    isStaging: Boolean,
    onNext: () -> Unit,
) {
    BottomAppBar(
        actions = {
            IconButton(
                modifier = Modifier
                    .padding(4.dp)
                    .padding(bottom = 6.dp),
                onClick = {
                    if (!isLoading) onSelectAll()
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.DoneAll,
                    contentDescription = stringResource(R.string.select_all)
                )
            }
            IconButton(
                modifier = Modifier
                    .padding(4.dp)
                    .padding(bottom = 6.dp),
                onClick = {
                    if (!isLoading) onDeselectAll()
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ClearAll,
                    contentDescription = stringResource(R.string.deselect_all)
                )
            }
        },
        floatingActionButton = {
            val label = stringResource(R.string.next)
            val buttonStatus = if (isStaging) {
                ButtonStatus.Loading(label) {}
            } else ButtonStatus.Unspecified(label, onNext)
            NextFab(
                buttonStatus = buttonStatus
            )
        }
    )
}