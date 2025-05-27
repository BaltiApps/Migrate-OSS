package balti.migrate.restore.ui.screens.restoreSummary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.data.model.ContactData
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.sources.fileSystem.TextWriterImpl
import baltiapps.migrate.domain.INTERNAL_ROUGH_WORK_DIRECTORY
import baltiapps.migrate.domain.PermissionConstants
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.usecase.ExportContactsForRestoreUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RestoreSummaryViewModel(
    private val applicationContext: Context,
    private val restoreDataRepository: RestoreDataRepository,
    private val exportContactsForRestoreUseCase: ExportContactsForRestoreUseCase,
    private val contextSource: ContextSource,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RestoreSummaryState(
            isInitialized = false,
            countContacts = 0,
            countCallLogs = 0,
            countSms = 0,
            contactSummaryState = RestoreSummaryItemState.UNKNOWN,
            smsSummaryState = RestoreSummaryItemState.UNKNOWN,
        )
    )
    val state = _state.asStateFlow()

    companion object {
        private const val VCF_FILE_NAME = "contacts.vcf"
    }

    val vcfFile: JavaFile = JavaFile("${applicationContext.filesDir}/$INTERNAL_ROUGH_WORK_DIRECTORY/$VCF_FILE_NAME")

    private lateinit var runService: () -> Unit

    private val textWriter = TextWriterImpl()

    val smsPermission = PermissionConstants.DEFAULT_SMS_APP

    init {
        val contactCount = restoreDataRepository.stagedContacts.size
        val smsCount = restoreDataRepository.stagedSms.size
        val callLogCount = restoreDataRepository.stagedCallLogs.size
        _state.update {
            it.copy(
                isInitialized = true,
                countContacts = contactCount,
                countCallLogs = callLogCount,
                countSms = smsCount,
                contactSummaryState = if (contactCount > 0) RestoreSummaryItemState.WAITING else RestoreSummaryItemState.UNKNOWN,
                smsSummaryState = if (smsCount > 0) RestoreSummaryItemState.WAITING else RestoreSummaryItemState.UNKNOWN,
            )
        }
    }

    private fun runRestore() {
        viewModelScope.launch {
            if (_state.value.contactSummaryState == RestoreSummaryItemState.WAITING) {
                exportContacts()
            } else if (_state.value.smsSummaryState == RestoreSummaryItemState.WAITING) {
                _state.update {
                    it.copy(smsSummaryState = RestoreSummaryItemState.REQUEST_USER_INPUT)
                }
            } else {
                runService()
            }
        }
    }

    private fun exportContacts() = viewModelScope.launch {
        exportContactsForRestoreUseCase.invoke(
            file = vcfFile,
            textWriter = textWriter,
            getContactContent = { (it as? ContactData)?.vcfContent ?: "" }
        ).onStart {
            _state.update {
                it.copy(contactSummaryState = RestoreSummaryItemState.PROCESSING)
            }
        }.onCompletion {
            _state.update {
                it.copy(
                    contactSummaryState = RestoreSummaryItemState.REQUEST_USER_INPUT,
                    contactsExportProgress = it.contactsExportProgress.copy(percentage = 1.0),
                )
            }
        }.collect { exportProgress ->
            _state.update {
                it.copy(
                    contactSummaryState = RestoreSummaryItemState.PROCESSING,
                    contactsExportProgress = exportProgress
                )
            }
        }
    }

    fun onAction(action: RestoreSummaryAction) {
        when (action) {
            is RestoreSummaryAction.StartRestore -> {
                this.runService = action.runService
                runRestore()
            }
            is RestoreSummaryAction.OnUserProceedContactImport -> {
                _state.update {
                    it.copy(contactSummaryState = RestoreSummaryItemState.ON_USER_INPUT_POSITIVE)
                }
            }
            is RestoreSummaryAction.SkipContacts -> {
                _state.update {
                    it.copy(contactSummaryState = RestoreSummaryItemState.ON_USER_INPUT_NEGATIVE)
                }
                runRestore()
            }
            is RestoreSummaryAction.OnContactImported -> {
                _state.update {
                    it.copy(contactSummaryState = RestoreSummaryItemState.DONE)
                }
                runRestore()
            }
            is RestoreSummaryAction.OnUserProceedSetDefaultSmsApp -> {
                _state.update {
                    it.copy(smsSummaryState = RestoreSummaryItemState.ON_USER_INPUT_POSITIVE)
                }
            }
            is RestoreSummaryAction.SkipSms -> {
                _state.update {
                    it.copy(smsSummaryState = RestoreSummaryItemState.ON_USER_INPUT_NEGATIVE)
                }
                runRestore()
            }
            is RestoreSummaryAction.OnDefaultSmsAppSet -> {
                val isDefault = contextSource.checkPermission(smsPermission)
                if (isDefault) {
                    _state.update {
                        it.copy(smsSummaryState = RestoreSummaryItemState.DONE)
                    }
                } else {
                    _state.update {
                        it.copy(smsSummaryState = RestoreSummaryItemState.CANCELLED)
                    }
                }
                runRestore()
            }
        }
    }
}