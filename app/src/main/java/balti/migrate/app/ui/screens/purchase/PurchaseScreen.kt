package balti.migrate.app.ui.screens.purchase

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.app.data.purchasesV2.UiContextImpl
import balti.migrate.app.ui.screens.purchase.components.ErrorContent
import balti.migrate.app.ui.screens.purchase.components.PendingContent
import balti.migrate.app.ui.screens.purchase.components.PurchaseContent
import balti.migrate.app.ui.screens.purchase.components.ThankYouContent
import balti.migrate.common.utils.ObserveEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PurchaseScreen(
    viewModel: PurchaseScreenViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current ?: return
    val snackbarHostState = remember { SnackbarHostState() }
    val cancelledMessage = stringResource(R.string.purchase_cancelled)

    ObserveEvents(
        eventsStream = viewModel.events,
        onEvent = { event ->
            when (event) {
                is PurchaseScreenEvent.PurchaseCancelled -> snackbarHostState.showSnackbar(cancelledMessage)
            }
        }
    )

    Content(
        state = state,
        snackbarHostState = snackbarHostState,
        onItemSelected = { item ->
            viewModel.performAction(PurchaseScreenAction.OnItemSelected(item, UiContextImpl(activity)))
        },
        onRetry = {
            viewModel.performAction(PurchaseScreenAction.OnRetry)
        },
        onRestorePurchase = {
            viewModel.performAction(PurchaseScreenAction.OnRestorePurchase)
        },
        onRefresh = {
            viewModel.performAction(PurchaseScreenAction.OnRefresh)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: PurchaseScreenState,
    snackbarHostState: SnackbarHostState,
    onItemSelected: (PurchaseItem) -> Unit,
    onRetry: () -> Unit,
    onRestorePurchase: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadFailedMessage = stringResource(R.string.purchase_error)
    val purchaseFailedMessage = stringResource(R.string.purchase_failed)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // isRefreshing stays false: the pull only triggers a refresh, progress is shown by the bar below
        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                val minColumnHeight = maxHeight
                val scrollModifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minColumnHeight)
                    .padding(dimensionResource(R.dimen.screen_padding))
                    .verticalScroll(rememberScrollState())

                when (val purchaseState = state.purchaseState) {
                    is PurchaseState.Purchased -> ThankYouContent(
                        item = purchaseState.item,
                        modifier = scrollModifier,
                    )
                    is PurchaseState.Pending -> PendingContent(
                        modifier = scrollModifier,
                    )
                    is PurchaseState.LoadFailed -> ErrorContent(
                        message = loadFailedMessage,
                        onRetry = onRetry,
                        modifier = scrollModifier,
                    )
                    is PurchaseState.PurchaseFailed -> ErrorContent(
                        message = purchaseFailedMessage,
                        onRetry = onRetry,
                        modifier = scrollModifier,
                    )
                    is PurchaseState.Idle -> PurchaseContent(
                        state = state,
                        onItemSelected = onItemSelected,
                        onRestorePurchase = onRestorePurchase,
                        isLoading = state.isLoading,
                        modifier = scrollModifier,
                    )
                }

                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = dimensionResource(R.dimen.screen_padding))
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                    )
                }
            }
        }
    }
}


@Preview
@Composable
private fun PurchaseScreenPreview() {
    Content(
        snackbarHostState = remember { SnackbarHostState() },
        state = PurchaseScreenState(
            items = listOf(
                PurchaseItem("migrate_donate_supporter", "Supporter", "$2.00"),
                PurchaseItem("migrate_donate_backer", "Backer", "$4.00"),
                PurchaseItem("migrate_donate_legend", "Legend", "$6.00"),
            )
        ),
        onItemSelected = {},
        onRetry = {},
        onRestorePurchase = {},
        onRefresh = {},
    )
}

@Preview
@Composable
private fun PurchaseScreenErrorPreview() {
    Content(
        snackbarHostState = remember { SnackbarHostState() },
        state = PurchaseScreenState(purchaseState = PurchaseState.LoadFailed),
        onItemSelected = {},
        onRetry = {},
        onRestorePurchase = {},
        onRefresh = {},
    )
}
