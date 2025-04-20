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
    ACTION_AWAITING,            // this means that a user action is needed and being awaited.
    ACTION_PROMPT,              // to signal UI to show an alert dialog to provide explanation to the user
    ACTION_PROCEED,             // user agrees to the explanation in the previous step, clicks "Proceed"
                                // in the alert dialog, hence do the work.
    ACTION_CANCELLED,           // User clicked "Skip" or otherwise cancelled the action.
    ACTION_NO_ACTION_NEEDED,    // If evaluation shows user action is not needed, example no need to show
                                // an explanation dialog or ask for permission.
}