package balti.migrate.common.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import balti.migrate.common.data.model.AppIcon
import baltiapps.migrate.domain.common.model.AppListItem

@Composable
fun ExternalDataAppRow(
    item: AppListItem,
    enabled: Boolean,
    onExternalDataToggled: (AppListItem) -> Unit,
    onExternalMediaToggled: (AppListItem) -> Unit,
    onRowClicked: (AppListItem) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onRowClicked(item) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (item.appIcon as? AppIcon)?.run {
            Image(
                bitmap = this.drawable.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
        }

        Text(
            text = item.appName,
            modifier = Modifier.padding(horizontal = 8.dp).weight(1f),
        )

        Checkbox(
            checked = item.isExternalDataSelected,
            onCheckedChange = { onExternalDataToggled(item) },
            enabled = enabled,
        )
        Checkbox(
            checked = item.isExternalMediaSelected,
            onCheckedChange = { onExternalMediaToggled(item) },
            enabled = enabled,
        )
    }
}
