package balti.migrate.common.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import balti.migrate.R
import balti.migrate.app.ui.theme.SystemCoreColor
import balti.migrate.app.ui.theme.SystemUpdatedColor
import balti.migrate.common.data.model.AppIcon
import baltiapps.migrate.domain.common.model.AppListItem

@Composable
fun RenderAppListItem(
    item: AppListItem,
    enabled: Boolean,
    onApkSelected: (item: AppListItem) -> Unit,
    onDataSelected: (item: AppListItem) -> Unit,
    onPermissionSelected: (item: AppListItem) -> Unit,
    onItemClicked: (AppListItem) -> Unit,
) {
    val alphaModifier = Modifier.alpha(
        if (enabled) 1f else 0.38f
    )

    val appTextColor = when {
        item.isUpdatedSystemApp -> SystemUpdatedColor
        item.isSystemApp -> SystemCoreColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .clickable(enabled = enabled) {
                onItemClicked(item)
            }
            .padding(dimensionResource(R.dimen.column_item_padding)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppListAppIcon(
            item = item,
            modifier = alphaModifier.size(48.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
        ) {
            Text(
                text = item.appName,
                modifier = alphaModifier,
                color = appTextColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                modifier = Modifier.basicMarquee(),
                text = item._id,
                fontWeight = FontWeight.Thin,
                style = MaterialTheme.typography.bodySmall,
                color = appTextColor,
            )
            if (item.isSelf) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    modifier = Modifier.basicMarquee(),
                    text = stringResource(R.string.restore_cannot_restore_self),
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemUpdatedColor,
                )
            } else if (item.isVersionLowerThanInstalled) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    modifier = Modifier.basicMarquee(),
                    text = stringResource(R.string.apk_version_lower_than_installed),
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemUpdatedColor,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = item.isApkSelected,
                onCheckedChange = {
                    onApkSelected(item)
                },
                enabled = enabled && item.isApkEnabled,
            )
            Checkbox(
                checked = item.isDataSelected,
                onCheckedChange = {
                    onDataSelected(item)
                },
                enabled = enabled && item.isDataEnabled,
            )
            Checkbox(
                checked = item.isPermissionsSelected,
                onCheckedChange = {
                    onPermissionSelected(item)
                },
                enabled = enabled && item.isPermissionsEnabled,
            )
        }
    }
}

@Composable
private fun AppListAppIcon(
    item: AppListItem,
    modifier: Modifier = Modifier,
) {
    (item.appIcon as? AppIcon)?.run {
        val bitmap = this.drawable.toBitmap().asImageBitmap()
        Image(
            modifier = modifier,
            bitmap = bitmap,
            contentDescription = null,
        )
    }
}

@Preview
@Composable
private fun RenderAppListItemPreview() {
    val context = LocalContext.current

    val item = AppListItem(
        _id = "com.example.app",
        appName = "Example App",

        appIcon = AppIcon(
            drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)!!
        ),

        versionName = "1.0.0",
        versionCode = 1,

        isApkSelected = true,
        isDataSelected = false,
        isPermissionsSelected = true,

        isSystemApp = false,
        isUpdatedSystemApp = false,
    )

    RenderAppListItem(
        item = item,
        enabled = true,
        onApkSelected = {},
        onDataSelected = {},
        onPermissionSelected = {},
        onItemClicked = {},
    )
}
