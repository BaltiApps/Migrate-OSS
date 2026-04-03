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

class AppRestoreEngine(
    private val applicationContext: Context,
    private val superuserUtils: SuperuserUtils,
): RestoreEngine<AppData> {

    private lateinit var readLocation: String
    private lateinit var suShell: Process

    override fun setLocation(location: String) {
        super.setLocation(location)
        readLocation = location
    }

    override fun checkPermission(): Boolean {
        return true
    }

    override fun restoreDataItems(items: List<AppData>): Flow<Progress> {
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
                        itemId = "",
                        progressType = Progress.ProgressType.APP_RESTORE,
                        percentage = 0.0,
                        logs = "Unpack error - ${unpackResult1.exceptionOrNull()?.message}; ${unpackResult2.exceptionOrNull()?.message}; ${unpackResult3.exceptionOrNull()?.message}",
                        isFailure = true
                    )
                )
                return@callbackFlow
            }

            items.forEachIndexed { index, appData ->
                Timber.d("Run script for : ${appData.appName} - ${appData.packageName}")
                val percentage = getPercentage(index + 1, items.size)

                if (appData.shouldBackupApk) {
                    superuserUtils.runScript(
                        scriptPath = scriptLocationApkRestore,
                        parentSuperuserShell = suShell,
                        endMarker = AppBackupConstants.END_MARKER,
                        onProgress = { log ->
                            trySend(
                                Progress(
                                    itemId = appData.packageName,
                                    progressType = Progress.ProgressType.APP_RESTORE,
                                    percentage = percentage,
                                    logs = log
                                )
                            )
                        },
                        onError = { error ->
                            trySend(
                                Progress(
                                    itemId = appData.packageName,
                                    progressType = Progress.ProgressType.APP_RESTORE,
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
                                    itemId = appData.packageName,
                                    progressType = Progress.ProgressType.APP_RESTORE,
                                    percentage = percentage,
                                    logs = log
                                )
                            )
                        },
                        onError = { error ->
                            trySend(
                                Progress(
                                    itemId = appData.packageName,
                                    progressType = Progress.ProgressType.APP_RESTORE,
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
                            "true",
                            AppBackupConstants.END_MARKER,
                        )
                    )
                }

                if (appData.shouldBackupPermissions && appData.grantedPermissionList.isNotEmpty()) {
                    superuserUtils.runScript(
                        scriptPath = scriptLocationPermRestore,
                        parentSuperuserShell = suShell,
                        endMarker = AppBackupConstants.END_MARKER,
                        onProgress = { log ->
                            trySend(
                                Progress(
                                    itemId = appData.packageName,
                                    progressType = Progress.ProgressType.APP_RESTORE,
                                    percentage = percentage,
                                    logs = log
                                )
                            )
                        },
                        onError = { error ->
                            trySend(
                                Progress(
                                    itemId = appData.packageName,
                                    progressType = Progress.ProgressType.APP_RESTORE,
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
            }

            Timber.d("Close callbackFlow")
            close()
        }.flowOn(Dispatchers.IO)
    }

    override fun onRestoreOver() {
        super.onRestoreOver()
        superuserUtils.closeSuperuserShell(suShell)
    }
}