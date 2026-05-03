package balti.migrate.restore.data.sources.apps

import android.content.Context
import balti.migrate.R
import balti.migrate.common.data.model.AppData
import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.AppBackupConstants
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.restore.sources.RestoreEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

class ExternalDataRestoreEngine(
    private val applicationContext: Context,
    private val superuserUtils: SuperuserUtils,
) : RestoreEngine<AppData> {

    private lateinit var readLocation: String
    private lateinit var suShell: Process

    override fun setLocation(location: String) {
        super.setLocation(location)
        readLocation = location
    }

    val ignorableErrors = listOf(
        "removing leading '/' from member names"
    )

    override fun checkPermission(): Boolean = true

    override fun restoreDataItems(items: List<AppData>): Flow<Progress> {
        return callbackFlow {
            suShell = superuserUtils.getSuperuserShell()

            val scriptLocation = "${applicationContext.cacheDir}/restore_external.sh"
            val unpackResult = superuserUtils.unpackScript(R.raw.restore_external, scriptLocation)

            if (unpackResult.isFailure) {
                val errorMessage = unpackResult.exceptionOrNull()?.message ?: "Unpack error"
                trySend(
                    Progress(
                        itemId = "",
                        progressType = Progress.ProgressType.EXTERNAL_DATA_RESTORE,
                        percentage = 0.0,
                        logs = errorMessage,
                        isFailure = true
                    )
                )
                return@callbackFlow
            }

            val eligibleItems = items.filter {
                it.shouldBackupExternalData || it.shouldBackupExternalMedia
            }

            eligibleItems.forEachIndexed { index, item ->
                val packageName = item._id
                val appName = item.appName
                Timber.d("Restore external data for: $appName - $packageName")
                val percentage = getPercentage(index + 1, eligibleItems.size)

                val extDataTar = if (item.shouldBackupExternalData) {
                    "$readLocation/${packageName}.ext.data.tar.gz"
                } else {
                    AppBackupConstants.NULL_MARKER
                }
                val extMediaTar = if (item.shouldBackupExternalMedia) {
                    "$readLocation/${packageName}.ext.media.tar.gz"
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
                                progressType = Progress.ProgressType.EXTERNAL_DATA_RESTORE,
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
                                progressType = Progress.ProgressType.EXTERNAL_DATA_RESTORE,
                                percentage = percentage,
                                logs = error,
                                isFailure = error !in ignorableErrors
                            )
                        )
                    },
                    args = arrayOf(
                        appName,
                        packageName,
                        extDataTar,
                        extMediaTar,
                        "0",  // TODO: find a way to send the user
                        AppBackupConstants.NULL_MARKER,
                        AppBackupConstants.END_MARKER,
                    )
                )

                Timber.d("External restore finished for: $appName - $packageName")
            }

            Timber.d("Close ExternalDataRestoreEngine callbackFlow")
            close()
        }.flowOn(Dispatchers.IO)
    }

    override fun onRestoreOver() {
        superuserUtils.closeSuperuserShell(suShell)
    }
}
