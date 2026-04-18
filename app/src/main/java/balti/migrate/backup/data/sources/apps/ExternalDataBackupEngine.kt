package balti.migrate.backup.data.sources.apps

import android.content.Context
import balti.migrate.R
import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.AppBackupConstants
import baltiapps.migrate.domain.backup.sources.BackupEngine
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

class ExternalDataBackupEngine(
    private val applicationContext: Context,
    private val superuserUtils: SuperuserUtils,
) : BackupEngine<DataItem<AppListItem>> {

    private lateinit var writeLocation: String
    private lateinit var suShell: Process

    override fun setLocation(location: String) {
        super.setLocation(location)
        this.writeLocation = location
    }

    val ignorableErrors = listOf(
        "removing leading '/' from member names"
    )

    override fun backupDataItems(dataItems: List<DataItem<AppListItem>>): Flow<Progress> {
        return callbackFlow {
            suShell = superuserUtils.getSuperuserShell()

            val scriptLocation = "${applicationContext.cacheDir}/backup_external.sh"
            val unpackResult = superuserUtils.unpackScript(R.raw.backup_external, scriptLocation)

            if (unpackResult.isFailure) {
                val errorMessage = unpackResult.exceptionOrNull()?.message ?: "Unpack error"
                trySend(
                    Progress(
                        itemId = "",
                        progressType = Progress.ProgressType.EXTERNAL_DATA_BACKUP,
                        percentage = 0.0,
                        logs = errorMessage,
                        isFailure = true
                    )
                )
                return@callbackFlow
            }

            val eligibleItems = dataItems.filter {
                it.toListItem().isExternalDataSelected || it.toListItem().isExternalMediaSelected
            }

            eligibleItems.forEachIndexed { index, item ->
                val listItem = item.toListItem()
                val packageName = item._id
                val appName = listItem.appName
                Timber.d("Backup external data for: $appName - $packageName")
                val percentage = getPercentage(index + 1, eligibleItems.size)

                val externalDataPath = if (listItem.isExternalDataSelected) {
                    "/sdcard/Android/data/$packageName"
                } else {
                    AppBackupConstants.NULL_MARKER
                }
                val externalMediaPath = if (listItem.isExternalMediaSelected) {
                    "/sdcard/Android/media/$packageName"
                } else {
                    AppBackupConstants.NULL_MARKER
                }

                superuserUtils.runScript(
                    scriptPath = scriptLocation,
                    parentSuperuserShell = suShell,
                    endMarker = AppBackupConstants.END_MARKER,
                    onProgress = { log ->
                        trySend(
                            Progress(
                                itemId = packageName,
                                progressType = Progress.ProgressType.EXTERNAL_DATA_BACKUP,
                                percentage = percentage,
                                displayText = "$appName - External",
                                logs = log
                            )
                        )
                    },
                    onError = { error ->
                        trySend(
                            Progress(
                                itemId = packageName,
                                progressType = Progress.ProgressType.EXTERNAL_DATA_BACKUP,
                                percentage = percentage,
                                logs = error,
                                isFailure = error !in ignorableErrors
                            )
                        )
                    },
                    args = arrayOf(
                        writeLocation,
                        appName,
                        packageName,
                        externalDataPath,
                        externalMediaPath,
                        AppBackupConstants.NULL_MARKER,
                        AppBackupConstants.END_MARKER,
                    )
                )

                Timber.d("External backup finished for: $appName - $packageName")
            }

            Timber.d("Close ExternalDataBackupEngine callbackFlow")
            close()
        }.flowOn(Dispatchers.IO)
    }

    override fun onBackupOver() {
        superuserUtils.closeSuperuserShell(suShell)
    }
}
