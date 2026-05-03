package balti.migrate.restore.ui.screens.restoreSummary

import baltiapps.migrate.domain.common.model.AppSizeInfo
import baltiapps.migrate.domain.common.model.Progress

data class RestoreSummaryState(
    val isInitialized: Boolean = false,
    val countdown: Int = 5,
    val countContacts: Int = 0,
    val countCallLogs: Int = 0,
    val countSms: Int = 0,
    val countApps: Int = 0,
    val countExternalDataApps: Int = 0,
    val contactsExportProgress: Progress = Progress.Empty,
    val contactSummaryState: RestoreSummaryItemState,
    val smsSummaryState: RestoreSummaryItemState,
    val appsSummaryState: RestoreSummaryItemState,
    val notificationSummaryState: RestoreSummaryItemState,
    val shouldShowExternalDataWarningDialog: Boolean = false,
    val shouldShowNoSpaceDialog: Boolean = false,
    val requiredSpaceBytes: Long = 0L,
    val availableSpaceBytes: Long = 0L,
    val appSizeInfos: List<AppSizeInfo> = emptyList(),
    val shouldShowAppSizesDialog: Boolean = false,
)
