package balti.migrate.restore.data.sources.apps

import android.content.Context
import balti.migrate.R
import balti.migrate.common.data.model.AppData
import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.AppBackupConstants
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.GenericReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

class AppRestoreEngine(
    private val applicationContext: Context,
    private val superuserUtils: SuperuserUtils,
): GenericReader<List<AppData>> {

    private lateinit var readLocation: String
    private lateinit var suShell: Process

    override fun setup(readLocation: String) {
        this.readLocation = readLocation
    }

    override fun read(
        data: List<AppData>,
        onComplete: () -> Unit
    ): Flow<Progress> {
        return callbackFlow {
            suShell = superuserUtils.getSuperuserShell()

            val scriptLocationApkRestore = "${applicationContext.cacheDir}/restore_apks.sh"
            val scriptLocationDataRestore = "${applicationContext.cacheDir}/restore_data.sh"
            val scriptLocationPermRestore = "${applicationContext.cacheDir}/restore_permissions.sh"

            val unpackResult1 =
                superuserUtils.unpackScript(R.raw.restore_apks, scriptLocationApkRestore)
            val unpackResult2 =
                superuserUtils.unpackScript(R.raw.restore_data, scriptLocationDataRestore)
            val unpackResult3 =
                superuserUtils.unpackScript(R.raw.restore_permissions, scriptLocationPermRestore)

            if (unpackResult1.isFailure || unpackResult2.isFailure || unpackResult3.isFailure) {
                trySend(
                    Progress(
                        progressType = Progress.ProgressType.APP_BACKUP,
                        percentage = 0.0,
                        logs = "Unpack error - ${unpackResult1.exceptionOrNull()?.message}; ${unpackResult2.exceptionOrNull()?.message}; ${unpackResult3.exceptionOrNull()?.message}",
                        isFailure = true
                    )
                )
                return@callbackFlow
            }

            data.forEachIndexed { index, appData ->
                Timber.d("Run script for : ${appData.appName} - ${appData.packageName}")
                val percentage = getPercentage(index + 1, data.size)

                if (appData.shouldBackupApk) {
                    superuserUtils.runScript(
                        scriptPath = scriptLocationApkRestore,
                        parentSuperuserShell = suShell,
                        endMarker = AppBackupConstants.END_MARKER,
                        onProgress = { log ->
                            trySend(
                                Progress(
                                    progressType = Progress.ProgressType.APP_RESTORE,
                                    percentage = percentage,
                                    logs = log
                                )
                            )
                        },
                        onError = { error ->
                            trySend(
                                Progress(
                                    progressType = Progress.ProgressType.APP_BACKUP,
                                    percentage = percentage,
                                    logs = error,
                                    isFailure = true,
                                )
                            )
                        },
                        args = arrayOf(
                            appData.appName,
                            appData.packageName,
                            "${readLocation}/${appData.packageName}.app",
                            appData.installerName,
                            "0",  // TODO: find a way to send the user
                            AppBackupConstants.NULL_MARKER,
                            AppBackupConstants.END_MARKER,
                        )
                    )
                }

                if (appData.shouldBackupData) {
                    superuserUtils.runScript(
                        scriptPath = scriptLocationDataRestore,
                        parentSuperuserShell = suShell,
                        endMarker = AppBackupConstants.END_MARKER,
                        onProgress = { log ->
                            trySend(
                                Progress(
                                    progressType = Progress.ProgressType.APP_RESTORE,
                                    percentage = percentage,
                                    logs = log
                                )
                            )
                        },
                        onError = { error ->
                            trySend(
                                Progress(
                                    progressType = Progress.ProgressType.APP_BACKUP,
                                    percentage = percentage,
                                    logs = error,
                                    isFailure = true,
                                )
                            )
                        },
                        args = arrayOf(
                            appData.appName,
                            appData.packageName,
                            "0",  // TODO: find a way to send the user
                            appData.grantedPermissionList.joinToString(" "),
                            AppBackupConstants.END_MARKER,
                        )
                    )
                }

                if (appData.shouldBackupPermissions) {
                    superuserUtils.runScript(
                        scriptPath = scriptLocationPermRestore,
                        parentSuperuserShell = suShell,
                        endMarker = AppBackupConstants.END_MARKER,
                        onProgress = { log ->
                            trySend(
                                Progress(
                                    progressType = Progress.ProgressType.APP_RESTORE,
                                    percentage = percentage,
                                    logs = log
                                )
                            )
                        },
                        onError = { error ->
                            trySend(
                                Progress(
                                    progressType = Progress.ProgressType.APP_BACKUP,
                                    percentage = percentage,
                                    logs = error,
                                    isFailure = true,
                                )
                            )
                        },
                        args = arrayOf(
                            appData.appName,
                            appData.packageName,
                            "${readLocation}/${appData.packageName}.tar.gz",
                            "0",  // TODO: find a way to send the user
                            AppBackupConstants.END_MARKER,
                        )
                    )
                }
            }

            Timber.d("Close callbackFlow")
            close()
            awaitClose {
                onComplete()
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun close() {
        superuserUtils.closeSuperuserShell(suShell)
    }
}