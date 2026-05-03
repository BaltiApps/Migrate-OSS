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
    onProceed: (() -> Unit)? = null,
) {
    SimpleYesNoDialog(
        titleText = stringResource(R.string.external_data_restore_warning_title),
        dialogText = stringResource(R.string.external_data_restore_warning_description),
        onPositiveButton = onProceed ?: onDismiss,
        positiveButtonLabel = stringResource(if (onProceed != null) R.string.proceed else R.string.close),
        onNegativeButton = if (onProceed != null) onDismiss else null,
        negativeButtonLabel = if (onProceed != null) stringResource(R.string.go_back) else null,
        icon = Icons.Outlined.Storage,
        iconTint = SystemUpdatedColor,
    )
}
