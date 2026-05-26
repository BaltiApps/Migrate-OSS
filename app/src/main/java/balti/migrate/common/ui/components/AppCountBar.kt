package balti.migrate.common.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import balti.migrate.R

@Composable
fun AppCountBar(
    totalCount: Int,
    selectedCount: Int,
    isStaging: Boolean,
    areAllApksSelected: Boolean,
    areAllDataSelected: Boolean,
    areAllPermissionsSelected: Boolean,
    onAllApkToggled: (Boolean) -> Unit,
    onAllDataToggled: (Boolean) -> Unit,
    onAllPermissionToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shouldEnableApkSelection: Boolean = true,
    shouldEnableDataSelection: Boolean = true,
    shouldEnablePermissionSelection: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "${stringResource(R.string.selected_items)} - $selectedCount/$totalCount",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.app_count_bar_legend),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.basicMarquee(),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CheckboxLabel("A")
            Checkbox(
                checked = areAllApksSelected,
                onCheckedChange = { onAllApkToggled(it) },
                enabled = shouldEnableApkSelection && !isStaging,
                modifier = Modifier.removeTopPadding()
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CheckboxLabel("D")
            Checkbox(
                checked = areAllDataSelected,
                onCheckedChange = { onAllDataToggled(it) },
                enabled = shouldEnableDataSelection && !isStaging,
                modifier = Modifier.removeTopPadding()
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CheckboxLabel("P")
            Checkbox(
                checked = areAllPermissionsSelected,
                onCheckedChange = { onAllPermissionToggled(it) },
                enabled = shouldEnablePermissionSelection && !isStaging,
                modifier = Modifier.removeTopPadding()
            )
        }
    }
}

@Composable
internal fun CheckboxLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelSmall)
}

internal fun Modifier.removeTopPadding() = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val topPadding = 10.dp.roundToPx()
    layout(placeable.width, placeable.height - topPadding) {
        placeable.placeRelative(0, -topPadding)
    }
}
