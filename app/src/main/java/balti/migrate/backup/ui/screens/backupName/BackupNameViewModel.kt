package balti.migrate.backup.ui.screens.backupName

import android.content.Context
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.StatFs
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.data.sources.UsbStorageReceiver
import balti.migrate.common.data.sources.fileSystem.TransferUtils
import balti.migrate.common.utils.getDefaultBackupName
import baltiapps.migrate.domain.backup.model.BackupLocation
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.common.sources.Preferences
import baltiapps.migrate.domain.common.usecase.GetRequiredSpaceUseCase
import baltiapps.migrate.domain.restore.usecase.CalculateStagedAppsSizesUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class BackupNameViewModel(
    private val applicationContext: Context,
    private val preferences: Preferences,
    private val calculateStagedAppsSizesUseCase: CalculateStagedAppsSizesUseCase,
    private val backupDataRepository: BackupDataRepository,
    private val getRequiredSpaceUseCase: GetRequiredSpaceUseCase,
) : ViewModel() {

    private val savedSafLocationString: String
        get() = preferences.getCustomLocationParameter()
    private val savedSafLocationUri: Uri
        get() = savedSafLocationString.toUri()
    private val isSavedSafLocationAccessible: Boolean
        get() = TransferUtils.hasPermission(applicationContext, savedSafLocationUri)

    private fun getLocationString(): String {
        return if (savedSafLocationString.isNotBlank()) {
            TransferUtils.getUriFilePath(savedSafLocationUri)
        } else "${TransferUtils.internalStoragePath}/${MediaStoreDownloadFile.EXPORT_PATH_PREFIX}"
    }

    private val _state = MutableStateFlow(
        BackupNameState(
            backupName = getDefaultBackupName(),
            isSaf = null,
            safUriString = null,
            locationString = "",
            isLocationAccessible = false,
        )
    )
    val state = _state.asStateFlow()

    private val usbReceiver by lazy {
        UsbStorageReceiver(
            onAttached = {
                viewModelScope.launch {
                    delay(3000)
                    updateState()
                }
            },
            onDetached = { updateState() },
        )
    }

    private fun updateState() {
        viewModelScope.launch {

            val locationString = getLocationString()

            Timber.d("Is location available - $locationString")

            var totalBytes = 0L
            var availableBytes = 0L

            try {
                val stat = if (savedSafLocationString.isNotBlank()) {
                    TransferUtils.getStatFsForSafUri(savedSafLocationUri, applicationContext)
                } else if (locationString.isNotBlank()) {
                    Timber.d("Using location string for StatFs: $locationString")
                    StatFs(locationString)
                } else null

                totalBytes = stat?.totalBytes ?: 0
                availableBytes = stat?.availableBytes ?: 0

            } catch (e: Exception) {
                e.printStackTrace()
            }

            Timber.d("locationString: $locationString")
            Timber.d("Total space - ${totalBytes}, Available space - $availableBytes")

            _state.update {
                it.copy(
                    isSaf = savedSafLocationString.isNotBlank(),
                    safUriString = savedSafLocationString,
                    locationString = locationString,
                    isLocationAccessible = isSavedSafLocationAccessible && totalBytes > 0,
                    totalSpaceBytes = totalBytes,
                    availableSpaceBytes = availableBytes,
                )
            }
        }
    }

    init {
        updateState()
        val usbIntentFilters = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        applicationContext.registerReceiver(usbReceiver, usbIntentFilters)
    }

    fun onAction(action: BackupNameAction) {
        when (action) {
            is BackupNameAction.NameChanged -> {
                _state.update {
                    it.copy(backupName = action.name)
                }
            }
            is BackupNameAction.OnSafLocationSelected -> {
                if (action.uriString == null) {
                    return
                } else {
                    preferences.setCustomLocationParameter(action.uriString)
                    updateState()
                }
            }
            is BackupNameAction.StartBackup -> {
                startBackup(action.startBackupMethod)
            }
            BackupNameAction.DismissNoSpaceDialog -> {
                _state.update { it.copy(shouldShowNoSpaceDialog = false) }
            }
        }
    }

    private fun startBackup(startBackupMethod: (BackupLocation) -> Unit) {
        viewModelScope.launch {
            if (backupDataRepository.shouldBackupApps()) {
                _state.update { it.copy(isScanningAppSizes = true) }
                calculateStagedAppsSizesUseCase().onCompletion {
                    _state.update {
                        it.copy(
                            appSizeScanProgress = it.appSizeScanProgress?.copy(percentage = 1.0),
                            appSizes = backupDataRepository.stagedAppSizeMap,
                        )
                    }
                }.collect { progress ->
                    _state.update { it.copy(appSizeScanProgress = progress) }
                }
                _state.update { it.copy(isScanningAppSizes = false) }

                val requiredSpace = getRequiredSpaceUseCase.invoke(_state.value.appSizes)
                val availableSpace = _state.value.availableSpaceBytes

                Timber.d("Required space for backup: $requiredSpace, available: $availableSpace")

                if (requiredSpace > availableSpace) {
                    _state.update {
                        it.copy(
                            requiredSpaceBytes = requiredSpace,
                            shouldShowNoSpaceDialog = true,
                        )
                    }
                    return@launch
                }
            }
            startBackupMethod(
                BackupLocation(
                    backupName = state.value.backupName,
                    backupUriString = state.value.safUriString,
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("Unregister UsbReceiver")
        applicationContext.unregisterReceiver(usbReceiver)
    }
}
