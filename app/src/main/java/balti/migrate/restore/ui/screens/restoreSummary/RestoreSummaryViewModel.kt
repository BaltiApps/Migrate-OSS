package balti.migrate.restore.ui.screens.restoreSummary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.MainActivity
import balti.migrate.common.data.model.ContactData
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.sources.fileSystem.TextWriterImpl
import baltiapps.migrate.domain.INTERNAL_ROUGH_WORK_DIRECTORY
import baltiapps.migrate.domain.PermissionConstants
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.usecase.ExportContactsForRestoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RestoreSummaryViewModel(
    private val applicationContext: Context,
    private val restoreDataRepository: RestoreDataRepository,
    private val exportContactsForRestoreUseCase: ExportContactsForRestoreUseCase,
    private val contextSource: ContextSource,
) : ViewModel() {

    private val _state = MutableStateFlow(RestoreSummaryState())
    val state = _state.asStateFlow()

    companion object {
        private const val VCF_FILE_NAME = "contacts.vcf"
    }

    private fun getUserAction(count: Int): UserActionState {
        return if (count > 0) {
            UserActionState.ACTION_AWAITING
        } else UserActionState.NOT_APPLICABLE
    }

    init {
        val contactCount = restoreDataRepository.stagedContacts.size
        val smsCount = restoreDataRepository.stagedSms.size
        _state.update {
            it.copy(
                isInitialized = true,
                countContacts = contactCount,
                countCallLogs = restoreDataRepository.stagedCallLogs.size,
                countSms = smsCount,
                contactsUserActionState = getUserAction(contactCount),
                smsUserActionState = getUserAction(smsCount),
            )
        }
    }

    private var vcfFile: GenericFile? = null
    private var taskMap: Map<String, RestoreTask> = emptyMap()

    private fun runTask(task: RestoreTask, onTaskComplete: () -> Unit) {
        viewModelScope.launch {
            when (task) {
                is ExportContactsTask -> {
                    val directory = File(applicationContext.filesDir, INTERNAL_ROUGH_WORK_DIRECTORY)
                    directory.mkdirs()
                    val vcfFile = JavaFile(File(directory, VCF_FILE_NAME))
                    this@RestoreSummaryViewModel.vcfFile = vcfFile
                    val textWriter = TextWriterImpl()
                    exportContactsForRestoreUseCase.invoke(
                        file = vcfFile,
                        textWriter = textWriter,
                        getContactContent = { (it as? ContactData)?.vcfContent ?: "" }
                    ).onCompletion {
                        _state.update {
                            it.copy(
                                contactsExportProgress = it.contactsExportProgress.copy(percentage = 1.0),
                            )
                        }
                        onTaskComplete()
                    }.collect { exportProgress ->
                        _state.update {
                            it.copy(contactsExportProgress = exportProgress)
                        }
                    }
                }

                is ShowDialogForContactsTask -> {
                    _state.update {
                        it.copy(
                            contactsUserActionState = UserActionState.ACTION_PROMPT,
                        )
                    }
                    onTaskComplete()
                }

                is LaunchContactAppTask -> withContext(Dispatchers.Main) {
                    (task.getActivity() as? MainActivity)?.launchContactChooser(
                        vcfFile = vcfFile,
                        onUserConfirmation = { isSuccessful ->
                            if (!isSuccessful) {
                                _state.update {
                                    it.copy(
                                        contactsUserActionState = UserActionState.ACTION_CANCELLED
                                    )
                                }
                            }
                            onTaskComplete()
                        }
                    )
                }

                is ShowDialogForSms -> {
                    _state.update {
                        it.copy(
                            smsUserActionState = UserActionState.ACTION_PROMPT,
                        )
                    }
                    onTaskComplete()
                }

                is SetAsDefaultSmsAppTask -> withContext(Dispatchers.Main) {
                    (task.getActivity() as? MainActivity)?.requestPermission(
                        permission = PermissionConstants.DEFAULT_SMS_APP,
                        onUserConfirmation = {
                            if (!contextSource.checkPermission(PermissionConstants.DEFAULT_SMS_APP)) {
                                _state.update {
                                    it.copy(
                                        smsUserActionState = UserActionState.ACTION_CANCELLED
                                    )
                                }
                            }
                            onTaskComplete()
                        }
                    )
                }

                is StartRestoreServiceTask -> {
                    task.runService()
                }
            }
        }
    }

    private fun runTasksSequentially(taskClassName: String = taskMap.keys.first()) {
        val taskToRun = taskMap[taskClassName] ?: return
        if (!taskToRun.shouldRunTask(_state.value)) return
        runTask(taskToRun) {
            determineNextTask(
                currentTaskClassName = taskClassName,
                state = _state.value,
            )?.run {
                runTasksSequentially(this)
            }
        }
    }

    private fun resumeTasks(lastTaskClassName: String) {
        determineNextTask(
            currentTaskClassName = lastTaskClassName,
            state = _state.value,
        )?.run {
            runTasksSequentially(this)
        }
    }

    fun onAction(action: RestoreSummaryAction) {
        when (action) {
            is RestoreSummaryAction.SetTaskMap -> {
                taskMap = action.taskMap.filterKeys { it != null }.mapKeys { it.key!! }
            }
            is RestoreSummaryAction.StartRestore -> {
                runTasksSequentially()
            }
            is RestoreSummaryAction.ProceedWithContacts -> {
                _state.update {
                    it.copy(
                        contactsUserActionState = UserActionState.ACTION_PROCEED
                    )
                }
                resumeTasks(ShowDialogForContactsTask::class.simpleName!!)
            }
            is RestoreSummaryAction.SkipContacts -> {
                _state.update {
                    it.copy(
                        contactsUserActionState = UserActionState.ACTION_CANCELLED
                    )
                }
                resumeTasks(ShowDialogForContactsTask::class.simpleName!!)
            }
            is RestoreSummaryAction.ProceedWithSms -> {
                _state.update {
                    it.copy(
                        smsUserActionState = UserActionState.ACTION_PROCEED
                    )
                }
                resumeTasks(ShowDialogForSms::class.simpleName!!)
            }
            is RestoreSummaryAction.SkipSms -> {
                _state.update {
                    it.copy(
                        smsUserActionState = UserActionState.ACTION_CANCELLED
                    )
                }
                resumeTasks(ShowDialogForSms::class.simpleName!!)
            }
        }
    }
}