package balti.migrate.common.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import balti.migrate.R
import balti.migrate.app.ui.theme.SystemUpdatedColor

@Composable
fun ExternalDataRestoreWarningDialog(
    onDismiss: () -> Unit,
) {
    SimpleYesNoDialog(
        titleText = stringResource(R.string.external_data_restore_warning_title),
        dialogText = stringResource(R.string.external_data_restore_warning_description),
        onPositiveButton = onDismiss,
        positiveButtonLabel = stringResource(R.string.close),
        icon = Icons.Outlined.Storage,
        iconTint = SystemUpdatedColor,
    )
}
