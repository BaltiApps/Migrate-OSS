package balti.migrate.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import balti.migrate.R

@Composable
fun CountBar(
    totalCount: Int,
    selectedCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.selected_items),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(
            modifier = Modifier
                .widthIn(min = 8.dp)
                .weight(1F)
        )
        Text(
            text = "$selectedCount/$totalCount",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview
@Composable
private fun CountBarPreview() {
    CountBar(
        totalCount = 10,
        selectedCount = 5,
    )
}