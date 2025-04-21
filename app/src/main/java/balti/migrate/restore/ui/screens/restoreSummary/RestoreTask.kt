package balti.migrate.restore.ui.screens.restoreSummary

import android.app.Activity

interface RestoreTask {
    fun shouldRunTask(state : RestoreSummaryState): Boolean
}

class ExportContactsTask : RestoreTask {
    override fun shouldRunTask(state: RestoreSummaryState): Boolean = state.countContacts > 0
}

class ShowDialogForContactsTask: RestoreTask {
    override fun shouldRunTask(state: RestoreSummaryState): Boolean {
        return state.countContacts > 0 &&
                state.contactsUserActionState == UserActionState.ACTION_AWAITING
    }
}

class LaunchContactAppTask(
    val getActivity: () -> Activity?,
): RestoreTask {
    override fun shouldRunTask(state : RestoreSummaryState): Boolean {
        return state.countContacts > 0 &&
                state.contactsUserActionState == UserActionState.ACTION_PROCEED
    }
}

class ShowDialogForSms: RestoreTask {
    override fun shouldRunTask(state: RestoreSummaryState): Boolean {
        return state.countSms > 0 && state.smsUserActionState == UserActionState.ACTION_AWAITING
    }
}

class SetAsDefaultSmsAppTask(
    val getActivity: () -> Activity?,
): RestoreTask {
    override fun shouldRunTask(state : RestoreSummaryState): Boolean {
        return state.countSms > 0 && state.smsUserActionState == UserActionState.ACTION_PROCEED
    }
}

class StartRestoreServiceTask(
    val runService: () -> Unit,
): RestoreTask {
    override fun shouldRunTask(state : RestoreSummaryState): Boolean = true
}

fun determineNextTask(
    currentTaskClassName: String?,
    state: RestoreSummaryState
): String? {
    return when (currentTaskClassName) {
        ExportContactsTask::class.simpleName -> ShowDialogForContactsTask::class.simpleName
        ShowDialogForContactsTask::class.simpleName -> {
            when (state.contactsUserActionState) {
                UserActionState.ACTION_PROCEED -> LaunchContactAppTask::class.simpleName
                UserActionState.ACTION_CANCELLED -> ShowDialogForSms::class.simpleName
                UserActionState.NOT_APPLICABLE -> ShowDialogForSms::class.simpleName
                else -> null
            }
        }
        LaunchContactAppTask::class.simpleName -> ShowDialogForSms::class.simpleName
        ShowDialogForSms::class.simpleName -> {
            when (state.smsUserActionState) {
                UserActionState.ACTION_PROCEED -> SetAsDefaultSmsAppTask::class.simpleName
                UserActionState.ACTION_CANCELLED -> StartRestoreServiceTask::class.simpleName
                UserActionState.ACTION_NO_ACTION_NEEDED -> StartRestoreServiceTask::class.simpleName
                UserActionState.NOT_APPLICABLE -> StartRestoreServiceTask::class.simpleName
                else -> null
            }
        }
        SetAsDefaultSmsAppTask::class.simpleName -> StartRestoreServiceTask::class.simpleName
        else -> null
    }
}