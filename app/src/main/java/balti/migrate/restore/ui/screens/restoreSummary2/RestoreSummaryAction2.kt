package balti.migrate.restore.ui.screens.restoreSummary2

sealed class RestoreSummaryAction2 {
    data class StartRestore(val runService: () -> Unit) : RestoreSummaryAction2()
    data object OnUserProceedContactImport : RestoreSummaryAction2()
    data object SkipContacts : RestoreSummaryAction2()
    data object OnContactImported : RestoreSummaryAction2()
    data object OnUserProceedSetDefaultSmsApp : RestoreSummaryAction2()
    data object SkipSms : RestoreSummaryAction2()
    data object OnDefaultSmsAppSet : RestoreSummaryAction2()
}