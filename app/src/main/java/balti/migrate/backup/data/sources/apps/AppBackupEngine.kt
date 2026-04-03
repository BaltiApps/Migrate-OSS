package balti.migrate.backup.data.sources.apps

import android.content.Context
import balti.migrate.R
import balti.migrate.common.data.model.AppData
import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.AppBackupConstants
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.GenericWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

class AppBackupEngine(
    private val applicationContext: Context,
    private val superuserUtils: SuperuserUtils,
) : GenericWriter<List<AppData>> {

    private lateinit var writeLocation: String
    private lateinit var suShell: Process

    override fun setup(writeLocation: String) {
        this.writeLocation = writeLocation
    }

    val ignorableErrors = listOf(
        "removing leading '/' from member names"
    )

    override fun write(
        data: List<AppData>,
        onComplete: () -> Unit
    ): Flow<Progress> {
        return callbackFlow {
            suShell = superuserUtils.getSuperuserShell()

            val apkScriptLocation = "${applicationContext.cacheDir}/backup_apk.sh"
            val dataScriptLocation = "${applicationContext.cacheDir}/backup_data.sh"

            val unpackApkResult =
                superuserUtils.unpackScript(R.raw.backup_apk, apkScriptLocation)
            val unpackDataResult =
                superuserUtils.unpackScript(R.raw.backup_data, dataScriptLocation)

            if (unpackApkResult.isFailure || unpackDataResult.isFailure) {
                val errorMessage = unpackApkResult.exceptionOrNull()?.message
                    ?: unpackDataResult.exceptionOrNull()?.message
                    ?: "Unpack error"
                trySend(
                    Progress(
                        itemId = "",
                        progressType = Progress.ProgressType.APP_BACKUP,
                        percentage = 0.0,
                        logs = errorMessage,
                        isFailure = true
                    )
                )
                return@callbackFlow
            }

            data.forEachIndexed { index, appData ->
                Timber.d("Run scripts for : ${appData.appName} - ${appData.packageName}")
                val percentage = getPercentage(index + 1, data.size)

                if (appData.shouldBackupApk) {
                    superuserUtils.runScript(
                        scriptPath = apkScriptLocation,
                        parentSuperuserShell = suShell,
                        endMarker = AppBackupConstants.END_MARKER,
                        onProgress = { log ->
                            trySend(
                                Progress(
                                    itemId = appData.packageName,
                                    progressType = Progress.ProgressType.APP_BACKUP,
                                    percentage = percentage,
                                    displayText = "${appData.appName} - APK",
                                    logs = log
                                )
                            )
                        },
                        onError = { error ->
                            trySend(
                                Progress(
                                    itemId = appData.packageName,
                                    progressType = Progress.ProgressType.APP_BACKUP,
                                    percentage = percentage,
                                    logs = error,
                                    isFailure = error !in ignorableErrors
                                )
                            )
                        },
                        args = arrayOf(
                            writeLocation,
                            appData.appName,
                            appData.packageName,
                            appData.apkPathBase,
                            AppBackupConstants.END_MARKER,
                        )
                    )
                }

                if (appData.shouldBackupData) {
                    superuserUtils.runScript(
                        scriptPath = dataScriptLocation,
                        parentSuperuserShell = suShell,
                        endMarker = AppBackupConstants.END_MARKER,
                        onProgress = { log ->
                            trySend(
                                Progress(
                                    itemId = appData.packageName,
                                    progressType = Progress.ProgressType.APP_BACKUP,
                                    percentage = percentage,
                                    displayText = "${appData.appName} - DATA",
                                    logs = log
                                )
                            )
                        },
                        onError = { error ->
                            trySend(
                                Progress(
                                    itemId = appData.packageName,
                                    progressType = Progress.ProgressType.APP_BACKUP,
                                    percentage = percentage,
                                    logs = error,
                                    isFailure = error !in ignorableErrors
                                )
                            )
                        },
                        args = arrayOf(
                            writeLocation,
                            appData.appName,
                            appData.packageName,
                            appData.dataPath,
                            "true",
                            AppBackupConstants.END_MARKER,
                        )
                    )
                }

                Timber.d("Scripts finished for : ${appData.appName} - ${appData.packageName}")
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