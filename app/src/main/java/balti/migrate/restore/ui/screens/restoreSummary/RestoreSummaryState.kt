package balti.migrate.restore.ui.screens.restoreSummary

import baltiapps.migrate.domain.common.model.Progress

data class RestoreSummaryState(
    val isInitialized: Boolean = false,
    val countdown: Int = 5,
    val countContacts: Int = 0,
    val countCallLogs: Int = 0,
    val countSms: Int = 0,
    val contactsExportProgress: Progress = Progress.Empty,
    val contactsUserActionState: UserActionState = UserActionState.NOT_APPLICABLE,
    val smsUserActionState: UserActionState = UserActionState.NOT_APPLICABLE,
)

enum class UserActionState {
    NOT_APPLICABLE,
    ACTION_AWAITING,
    ACTION_PROMPT,
    ACTION_PROCEED,
    ACTION_CANCELLED,
}