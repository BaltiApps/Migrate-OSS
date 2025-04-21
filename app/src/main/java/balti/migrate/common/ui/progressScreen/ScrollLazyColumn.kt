package balti.migrate.common.ui.progressScreen

import androidx.compose.foundation.lazy.LazyListState

suspend fun scrollToBottomLazyColumn(
    listState: LazyListState,
    items: List<*>,
) {
    if (items.isNotEmpty()) {
        listState.scrollToItem(items.size - 1)
    }
}

suspend fun scrollToTopLazyColumn(
    listState: LazyListState,
) {
    listState.scrollToItem(0)
}