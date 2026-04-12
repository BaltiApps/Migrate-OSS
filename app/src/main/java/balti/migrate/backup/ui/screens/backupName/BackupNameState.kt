package balti.migrate.backup.ui.screens.backupName

import baltiapps.migrate.domain.common.model.AppSizeInfo
import baltiapps.migrate.domain.common.model.Progress

data class BackupNameState(
    val backupName: String,
    val isSaf: Boolean?,
    val safUriString: String?,
    val locationString: String,
    val isLocationAccessible: Boolean,
    val isScanningAppSizes: Boolean = false,
    val isSpaceCalculationCancelled: Boolean = false,
    val appSizeScanProgress: Progress? = null,
    val appSizes: List<AppSizeInfo> = listOf(),
    val shouldShowNoSpaceDialog: Boolean = false,
    val shouldShowAppSizesDialog: Boolean = false,
    val totalSpaceBytes: Long = 0L,
    val availableSpaceBytes: Long = 0L,
    val requiredSpaceBytes: Long = 0L,
)
