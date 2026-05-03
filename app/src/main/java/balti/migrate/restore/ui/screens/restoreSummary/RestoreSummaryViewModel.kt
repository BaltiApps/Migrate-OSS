package balti.migrate.restore.ui.screens.restoreSummary

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.R
import balti.migrate.common.data.model.ContactData
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.sources.fileSystem.TextWriterImpl
import balti.migrate.common.utils.PermissionUtils
import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.INTERNAL_ROUGH_WORK_DIRECTORY
import baltiapps.migrate.domain.PermissionConstants
import baltiapps.migrate.domain.common.sources.ContextSource
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.sources.InternalStorageSpaceReader
import baltiapps.migrate.domain.restore.usecase.ExportContactsForRestoreUseCase
import baltiapps.migrate.domain.restore.usecase.GetRequiredSpaceForRestoreUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RestoreSummaryViewModel(
    private val applicationContext: Context,
    private val restoreDataRepository: RestoreDataRepository,
    private val exportContactsForRestoreUseCase: ExportContactsForRestoreUseCase,
    private val superuserUtils: SuperuserUtils,
    private val contextSource: ContextSource,
    private val internalStorageSpaceReader: InternalStorageSpaceReader,
    private val getRequiredSpaceForRestoreUseCase: GetRequiredSpaceForRestoreUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RestoreSummaryState(
            isInitialized = false,
            countContacts = 0,
            countCallLogs = 0,
            countSms = 0,
            countApps = 0,
            contactSummaryState = RestoreSummaryItemState.UNKNOWN,
            smsSummaryState = RestoreSummaryItemState.UNKNOWN,
            appsSummaryState = RestoreSummaryItemState.UNKNOWN,
            notificationSummaryState = RestoreSummaryItemState.UNKNOWN,
        )
    )
    val state = _state.asStateFlow()

    private val _errorMessage = Channel<String>()
    val errorMessage = _errorMessage.receiveAsFlow()

    companion object {
        private const val VCF_FILE_NAME = "contacts.vcf"
    }

    val vcfFile: JavaFile = JavaFile("${applicationContext.filesDir}/$INTERNAL_ROUGH_WORK_DIRECTORY/$VCF_FILE_NAME")

    private lateinit var runService: () -> Unit
    private var externalDataWarningAcknowledged = false

    private val textWriter = TextWriterImpl()

    val smsPermission = PermissionConstants.DEFAULT_SMS_APP

    init {
        val contactCount = restoreDataRepository.stagedContacts.size
        val smsCount = restoreDataRepository.stagedSms.size
        val callLogCount = restoreDataRepository.stagedCallLogs.size
        val appCount = restoreDataRepository.stagedApps.size
        val externalDataAppCount = restoreDataRepository.stagedApps.count {
            it.toListItem().isExternalDataSelected || it.toListItem().isExternalMediaSelected
        }
        _state.update {
            it.copy(
                isInitialized = true,
                countContacts = contactCount,
                countCallLogs = callLogCount,
                countSms = smsCount,
                countApps = appCount,
                countExternalDataApps = externalDataAppCount,
                contactSummaryState = if (contactCount > 0) RestoreSummaryItemState.WAITING else RestoreSummaryItemState.UNKNOWN,
                smsSummaryState = if (smsCount > 0) RestoreSummaryItemState.WAITING else RestoreSummaryItemState.UNKNOWN,
                appsSummaryState = if (appCount > 0) RestoreSummaryItemState.WAITING else RestoreSummaryItemState.UNKNOWN,
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
                if (_state.value.appsSummaryState == RestoreSummaryItemState.WAITING) {
                    superuserUtils.checkSuperuserPermission().run {
                        _state.update {
                            it.copy(
                                appsSummaryState =
                                    if (this.isSuccess) RestoreSummaryItemState.DONE
                                    else RestoreSummaryItemState.CANCELLED
                            )
                        }
                        delay(200)
                    }
                }

                val notificationPermission = PermissionUtils.notificationsPermission
                if (
                    notificationPermission != null &&
                    !contextSource.checkPermission(notificationPermission) &&
                    _state.value.notificationSummaryState == RestoreSummaryItemState.UNKNOWN
                ) {
                    _state.update {
                        it.copy(notificationSummaryState = RestoreSummaryItemState.REQUEST_USER_INPUT)
                    }
                } else {
                    val spaceInfo = internalStorageSpaceReader.getSpaceInfo()
                    val requiredSpace = getRequiredSpaceForRestoreUseCase.invoke()
                    if (requiredSpace > spaceInfo.bytesFree) {
                        _state.update {
                            it.copy(
                                requiredSpaceBytes = requiredSpace,
                                availableSpaceBytes = spaceInfo.bytesFree,
                                shouldShowNoSpaceDialog = true,
                                appSizeInfos = restoreDataRepository.stagedAppSizes,
                            )
                        }
                        return@launch
                    }
                    if (restoreDataRepository.shouldRestoreExternalData() && !externalDataWarningAcknowledged) {
                        _state.update { it.copy(shouldShowExternalDataWarningDialog = true) }
                    } else {
                        runService()
                    }
                }
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
                if (!restoreDataRepository.shouldRestoreAnything()) {
                    _errorMessage.trySend(applicationContext.getString(R.string.no_data_to_restore))
                } else {
                    this.runService = action.runService
                    runRestore()
                }
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
            is RestoreSummaryAction.OnNotificationPermissionResult -> {
                _state.update {
                    it.copy(
                        notificationSummaryState =
                            if (action.isGranted) RestoreSummaryItemState.DONE
                            else RestoreSummaryItemState.CANCELLED
                    )
                }
                runRestore()
            }
            is RestoreSummaryAction.DismissExternalDataWarningDialog -> {
                _state.update { it.copy(shouldShowExternalDataWarningDialog = false) }
            }
            is RestoreSummaryAction.ProceedExternalDataWarningDialog -> {
                _state.update { it.copy(shouldShowExternalDataWarningDialog = false) }
                externalDataWarningAcknowledged = true
                runRestore()
            }
            is RestoreSummaryAction.DismissNoSpaceDialog -> {
                _state.update { it.copy(shouldShowNoSpaceDialog = false) }
            }
            is RestoreSummaryAction.ShowAppSizesDialog -> {
                _state.update { it.copy(shouldShowAppSizesDialog = true) }
            }
            is RestoreSummaryAction.DismissAppSizesDialog -> {
                _state.update { it.copy(shouldShowAppSizesDialog = false) }
            }
        }
    }

    fun getAppName(packageName: String): String {
        return restoreDataRepository.stagedApps
            .find { it._id == packageName }
            ?.toListItem()
            ?.appName
            ?: packageName
    }
}
