package balti.migrate.backup.ui.screens.backupName

import android.content.Context
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.StatFs
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import balti.migrate.R
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.data.sources.UsbStorageReceiver
import balti.migrate.common.data.sources.fileSystem.TransferUtils
import balti.migrate.common.utils.getDefaultBackupName
import baltiapps.migrate.domain.backup.model.BackupLocation
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.usecase.GetRequiredSpaceForBackupUseCase
import baltiapps.migrate.domain.common.sources.Preferences
import baltiapps.migrate.domain.restore.usecase.CalculateStagedAppsSizesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class BackupNameViewModel(
    private val applicationContext: Context,
    private val preferences: Preferences,
    private val calculateStagedAppsSizesUseCase: CalculateStagedAppsSizesUseCase,
    private val backupDataRepository: BackupDataRepository,
    private val getRequiredSpaceForBackupUseCase: GetRequiredSpaceForBackupUseCase,
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

    private val _errorMessage = Channel<String>()
    val errorMessage = _errorMessage.receiveAsFlow()

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
                    isLocationAccessible = isSavedSafLocationAccessible && locationString.isNotBlank() && totalBytes > 0,
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
                if (_state.value.backupName.isBlank()) {
                    _errorMessage.trySend(applicationContext.getString(R.string.set_a_name_first))
                } else if (!_state.value.isLocationAccessible) {
                    _errorMessage.trySend(applicationContext.getString(R.string.setup_a_valid_location))
                } else if (!backupDataRepository.shouldBackupAnything()) {
                    _errorMessage.trySend(applicationContext.getString(R.string.no_data_to_backup))
                } else {
                    startBackup(action.startBackupMethod)
                }
            }
            is BackupNameAction.CancelSpaceCalculation -> {
                _state.update { it.copy(isSpaceCalculationCancelled = true) }
            }
            BackupNameAction.DismissNoSpaceDialog -> {
                _state.update { it.copy(shouldShowNoSpaceDialog = false) }
            }
            BackupNameAction.ShowAppSizesDialog -> {
                _state.update { it.copy(shouldShowAppSizesDialog = true) }
            }
            BackupNameAction.DismissAppSizesDialog -> {
                _state.update { it.copy(shouldShowAppSizesDialog = false) }
            }
        }
    }

    fun getAppName(packageName: String): String {
        return backupDataRepository.stagedApps
            .find { it._id == packageName }
            ?.toListItem()
            ?.appName
            ?: packageName
    }

    private fun startBackup(startBackupMethod: (BackupLocation) -> Unit) {
        viewModelScope.launch {
            try {
                if (backupDataRepository.shouldBackupApps()) {
                    _state.update {
                        it.copy(
                            isScanningAppSizes = true,
                            isSpaceCalculationCancelled = false,
                        )
                    }
                    calculateStagedAppsSizesUseCase.invoke().onCompletion {
                        _state.update {
                            it.copy(
                                appSizeScanProgress = it.appSizeScanProgress?.copy(percentage = 1.0),
                                appSizes = backupDataRepository.stagedAppSizes,
                            )
                        }
                    }.collect { progress ->
                        _state.update { it.copy(appSizeScanProgress = progress) }
                        if (_state.value.isSpaceCalculationCancelled) {
                            this.coroutineContext.cancel()
                        }
                    }
                    _state.update { it.copy(isScanningAppSizes = false) }

                    val requiredSpace = getRequiredSpaceForBackupUseCase.invoke()
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
            } catch (e: CancellationException) {
                Timber.d("Backup cancelled")
                _state.update {
                    it.copy(isScanningAppSizes = false)
                }
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.trySend(e.message.toString())
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("Unregister UsbReceiver")
        applicationContext.unregisterReceiver(usbReceiver)
    }
}
