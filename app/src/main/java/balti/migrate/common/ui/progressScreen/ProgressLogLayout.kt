package balti.migrate.common.ui.progressScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import baltiapps.migrate.domain.common.model.Progress

@Composable
fun ProgressLogLayout(
    items: List<Progress>,
    pauseLogs: () -> Unit,
    resumeLogs: () -> Unit,
    modifier: Modifier = Modifier,
    scrollAnchor: ScrollAnchor = ScrollAnchor.BOTTOM,
) {
    val listState = rememberLazyListState()
    var shouldAutoScroll by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(scrollAnchor) {
        when (scrollAnchor) {
            ScrollAnchor.TOP -> {
                shouldAutoScroll = false
                scrollToTopLazyColumn(listState)
            }
            ScrollAnchor.BOTTOM -> {
                shouldAutoScroll = true
                scrollToBottomLazyColumn(listState, items)
            }
            else -> {}
        }
    }
    Column(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                                pauseLogs()
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
            item {
                LaunchedEffect(true) {
                    resumeLogs()
                    shouldAutoScroll = true
                }
            }
        }
        LaunchedEffect(items) {
            if (shouldAutoScroll) {
                scrollToBottomLazyColumn(listState, items)
            }
        }
    }
}

private suspend fun scrollToBottomLazyColumn(
    listState: LazyListState,
    items: List<*>,
) {
    if (items.isNotEmpty()) {
        listState.scrollToItem(items.size - 1)
    }
}

private suspend fun scrollToTopLazyColumn(
    listState: LazyListState,
) {
    listState.scrollToItem(0)
}

enum class ScrollAnchor {
    TOP,
    BOTTOM,
    INDETERMINATE,
}