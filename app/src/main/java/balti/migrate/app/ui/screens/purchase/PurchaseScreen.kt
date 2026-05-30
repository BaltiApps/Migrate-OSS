package balti.migrate.app.ui.screens.purchase

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.app.ui.screens.purchase.components.ErrorContent
import balti.migrate.app.ui.screens.purchase.components.PurchaseContent
import balti.migrate.app.ui.screens.purchase.components.ThankYouContent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PurchaseScreen(
    viewModel: PurchaseScreenViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current ?: return
    Content(
        state = state,
        onItemSelected = { item ->
            viewModel.performAction(PurchaseScreenAction.OnItemSelected(item, activity))
        },
        onRetry = {
            viewModel.performAction(PurchaseScreenAction.OnRetry)
        },
        onRestorePurchase = {
            viewModel.performAction(PurchaseScreenAction.OnRestorePurchase)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: PurchaseScreenState,
    onItemSelected: (PurchaseItem) -> Unit,
    onRetry: () -> Unit,
    onRestorePurchase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val minColumnHeight = maxHeight
            val scrollModifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minColumnHeight)
                .padding(dimensionResource(R.dimen.screen_padding))
                .verticalScroll(rememberScrollState())

            when {
                state.purchasedItem != null -> ThankYouContent(
                    item = state.purchasedItem,
                    modifier = scrollModifier,
                )
                state.isError -> ErrorContent(
                    onRetry = onRetry,
                    modifier = scrollModifier,
                )
                else -> PurchaseContent(
                    state = state,
                    onItemSelected = onItemSelected,
                    onRestorePurchase = onRestorePurchase,
                    isLoading = state.isLoading,
                    modifier = scrollModifier,
                )
            }
        }
    }
}


@Preview
@Composable
private fun PurchaseScreenPreview() {
    Content(
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
    )
}

@Preview
@Composable
private fun PurchaseScreenErrorPreview() {
    Content(
        state = PurchaseScreenState(isError = true),
        onItemSelected = {},
        onRetry = {},
        onRestorePurchase = {},
    )
}
