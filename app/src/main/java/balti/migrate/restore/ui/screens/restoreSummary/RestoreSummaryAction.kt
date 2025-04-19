package balti.migrate.restore.ui.screens.restoreSummary

sealed class RestoreSummaryAction {
    data class SetTaskMap(val taskMap: Map<String?, RestoreTask>) : RestoreSummaryAction()
    data object StartRestore : RestoreSummaryAction()
    data object ProceedWithContacts : RestoreSummaryAction()
    data object SkipContacts : RestoreSummaryAction()
    data object ProceedWithSms : RestoreSummaryAction()
    data object SkipSms : RestoreSummaryAction()
}