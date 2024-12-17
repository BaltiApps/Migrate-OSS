package balti.migrate.backup.ui.screens.listScreen

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import balti.migrate.R
import balti.migrate.backup.ui.components.ButtonStatus
import balti.migrate.backup.ui.components.NextFab
import balti.migrate.backup.ui.screens.listScreen.callLogBackup.CallLogBackup
import balti.migrate.backup.ui.screens.listScreen.contactBackup.ContactBackup
import balti.migrate.backup.ui.screens.listScreen.smsBackup.SmsBackup
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ListScreenRoot(
    navigateUp: () -> Unit,
    goToBackupNameScreen: () -> Unit,
    viewModel: ListScreenRootViewModel = koinViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle(
        lifecycleOwner = lifecycleOwner
    )

    viewModel.setGoToNextNavRoot(goToBackupNameScreen)

    Content(
        state = { state },
        navigateUp = navigateUp,
        setStagingBlock = viewModel::setStagingBlock,
        setToggleAllBlock = viewModel::setToggleAllBlock,
        onPostStaging = viewModel::onPostStaging,
        onSelectAll = {
            viewModel.performAction(ListScreenRootAction.SelectAll)
        },
        onDeselectAll = {
            viewModel.performAction(ListScreenRootAction.DeselectAll)
        },
        onLoadingItems = { isLoading ->
            viewModel.performAction(ListScreenRootAction.SetLoading(isLoading))
        },
        onNext = {
            viewModel.performAction(ListScreenRootAction.OnNextClicked)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: () -> ListScreenRootState,
    navigateUp: () -> Unit,
    setStagingBlock: (() -> Unit) -> Unit,
    setToggleAllBlock: ((Boolean) -> Unit) -> Unit,
    onPostStaging: (navController: NavHostController) -> Unit,
    onLoadingItems: (Boolean) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onNext: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = rememberNavController()

    BackHandler {
        navigateUp()
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(state().currentRoute.label, scrollBehavior) {
//                if (currentRouteIndex > 0) {
//                    navController.navigateUp()
//                }
            }
        },
        bottomBar = {
            BottomBar(
                state = state(),
                onSelectAll = onSelectAll,
                onDeselectAll = onDeselectAll,
                onNext = onNext,
            )
        }
    ) { values ->
        NavHost(
            modifier = Modifier
                .padding(values),
            navController = navController,
            startDestination = RouteBackupList.RouteContactBackup
        ) {
            composable<RouteBackupList.RouteContactBackup> {
                ContactBackup(
                    isLoading = onLoadingItems,
                    setStagingBlock = setStagingBlock,
                    onPostStaging = { onPostStaging(navController) },
                    setToggleAll = setToggleAllBlock,
                )
            }
            composable<RouteBackupList.RouteCallLogBackup> {
                CallLogBackup(
                    isLoading = onLoadingItems,
                    setStagingBlock = setStagingBlock,
                    onPostStaging = { onPostStaging(navController) },
                    setToggleAll = setToggleAllBlock,
                )
            }
            composable<RouteBackupList.RouteSmsBackup> {
                SmsBackup(
                    isLoading = onLoadingItems,
                    setStagingBlock = setStagingBlock,
                    onPostStaging = { onPostStaging(navController) },
                    setToggleAll = setToggleAllBlock,
                )
            }
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
    state: ListScreenRootState,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onNext: () -> Unit,
) {
    val isLoading = state.isLoading
    val isStaging = state.isStaging
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