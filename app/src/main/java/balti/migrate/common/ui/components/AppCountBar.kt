package balti.migrate.common.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
        Text(
            text = "${stringResource(R.string.selected_items)} - $selectedCount/$totalCount",
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.weight(1f))
        Checkbox(
            checked = areAllApksSelected,
            onCheckedChange = {
                onAllApkToggled(it)
            },
            enabled = shouldEnableApkSelection && !isStaging,
        )
        Checkbox(
            checked = areAllDataSelected,
            onCheckedChange = {
                onAllDataToggled(it)
            },
            enabled = shouldEnableDataSelection && !isStaging,
        )
        Checkbox(
            checked = areAllPermissionsSelected,
            onCheckedChange = {
                onAllPermissionToggled(it)
            },
            enabled = shouldEnablePermissionSelection && !isStaging,
        )
    }
}