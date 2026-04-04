package balti.migrate.restore.ui.screens.restoreSummary

sealed class RestoreSummaryAction {
    data class StartRestore(val runService: () -> Unit) : RestoreSummaryAction()
    data object OnUserProceedContactImport : RestoreSummaryAction()
    data object SkipContacts : RestoreSummaryAction()
    data object OnContactImported : RestoreSummaryAction()
    data object OnUserProceedSetDefaultSmsApp : RestoreSummaryAction()
    data object SkipSms : RestoreSummaryAction()
    data object OnDefaultSmsAppSet : RestoreSummaryAction()
    data class OnNotificationPermissionResult(val isGranted: Boolean) : RestoreSummaryAction()
    data object DismissNoSpaceDialog : RestoreSummaryAction()
    data object ShowAppSizesDialog : RestoreSummaryAction()
    data object DismissAppSizesDialog : RestoreSummaryAction()
}