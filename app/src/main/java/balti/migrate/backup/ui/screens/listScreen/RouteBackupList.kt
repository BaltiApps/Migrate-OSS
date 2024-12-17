package balti.migrate.backup.ui.screens.listScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import balti.migrate.R
import kotlinx.serialization.Serializable

interface RouteBackupList {
    val label: String
        @Composable
        get() = ""

    @Serializable
    object RouteContactBackup: RouteBackupList {
        override val label: String
            @Composable
            get() = stringResource(R.string.label_contacts_backup)
    }

    @Serializable
    object RouteCallLogBackup: RouteBackupList {
        override val label: String
            @Composable
            get() = stringResource(R.string.label_call_log_backup)
    }

    @Serializable
    object RouteSmsBackup: RouteBackupList {
        override val label: String
            @Composable
            get() = stringResource(R.string.label_sms_backup)
    }
}