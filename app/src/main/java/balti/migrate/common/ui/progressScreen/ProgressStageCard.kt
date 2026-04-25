package balti.migrate.common.ui.progressScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import baltiapps.migrate.domain.common.model.Progress

@Composable
fun ProgressStageCard(
    progressType: Progress.ProgressType,
    title: String,
    subtitle: String,
    percentage: Float,
    modifier: Modifier = Modifier,
) {
    val icon = progressType.toIcon()
    val iconTint = when (progressType) {
        Progress.ProgressType.BACKUP_FINISHED,
        Progress.ProgressType.RESTORE_FINISHED -> MaterialTheme.colorScheme.primary
        Progress.ProgressType.BACKUP_FINISHED_WITH_ERRORS,
        Progress.ProgressType.RESTORE_FINISHED_WITH_ERRORS,
        Progress.ProgressType.BACKUP_CANCELLED,
        Progress.ProgressType.RESTORE_CANCELLED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (percentage <= 0f) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

}

private fun Progress.ProgressType.toIcon(): ImageVector = when (this) {
    Progress.ProgressType.STANDBY -> Icons.Outlined.Schedule
    Progress.ProgressType.CONTACTS_READ,
    Progress.ProgressType.CONTACTS_BACKUP,
    Progress.ProgressType.CONTACTS_BACKUP_READ,
    Progress.ProgressType.CONTACTS_EXPORT -> Icons.Outlined.Person
    Progress.ProgressType.CALL_LOG_READ,
    Progress.ProgressType.CALL_LOG_BACKUP,
    Progress.ProgressType.CALL_LOG_BACKUP_READ,
    Progress.ProgressType.CALL_LOG_RESTORE -> Icons.Outlined.Call
    Progress.ProgressType.SMS_READ,
    Progress.ProgressType.SMS_BACKUP,
    Progress.ProgressType.SMS_BACKUP_READ,
    Progress.ProgressType.SMS_RESTORE -> Icons.Outlined.Sms
    Progress.ProgressType.APP_LIST_READ,
    Progress.ProgressType.APP_INFO_BACKUP,
    Progress.ProgressType.APP_BACKUP,
    Progress.ProgressType.APP_INFO_READ,
    Progress.ProgressType.APP_RESTORE -> Icons.Outlined.Apps
    Progress.ProgressType.EXTERNAL_DATA_BACKUP,
    Progress.ProgressType.EXTERNAL_DATA_RESTORE,
    Progress.ProgressType.APP_SIZE_READ -> Icons.Outlined.Storage
    Progress.ProgressType.EXPORTING_BACKUP -> Icons.Outlined.Backup
    Progress.ProgressType.BACKUP_FINISHED,
    Progress.ProgressType.RESTORE_FINISHED -> Icons.Outlined.CheckCircle
    Progress.ProgressType.BACKUP_FINISHED_WITH_ERRORS,
    Progress.ProgressType.RESTORE_FINISHED_WITH_ERRORS -> Icons.Outlined.Warning
    Progress.ProgressType.BACKUP_CANCELLED,
    Progress.ProgressType.RESTORE_CANCELLED -> Icons.Outlined.Cancel
}
