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

            val scriptLocation = "${applicationContext.cacheDir}/backup_app_and_data.sh"

            val unpackResult =
                superuserUtils.unpackScript(R.raw.backup_app_and_data, scriptLocation)

            if (unpackResult.isFailure) {
                trySend(
                    Progress(
                        progressType = Progress.ProgressType.APP_BACKUP,
                        percentage = 0.0,
                        logs = unpackResult.exceptionOrNull()?.message ?: "Unpack error",
                        isFailure = true
                    )
                )
                return@callbackFlow
            }

            data.forEachIndexed { index, appData ->
                Timber.d("Run script for : ${appData.appName} - ${appData.packageName}")
                val percentage = getPercentage(index + 1, data.size)

                superuserUtils.runScript(
                    scriptPath = scriptLocation,
                    parentSuperuserShell = suShell,
                    endMarker = AppBackupConstants.END_MARKER,
                    onProgress = { log ->
                        trySend(
                            Progress(
                                progressType = Progress.ProgressType.APP_BACKUP,
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
                                isFailure = error !in ignorableErrors
                            )
                        )
                    },
                    args = arrayOf(
                        writeLocation,
                        appData.appName,
                        appData.packageName,
                        if (appData.shouldBackupApk) appData.apkPathBase else AppBackupConstants.NULL_MARKER,
                        if (appData.shouldBackupData) appData.dataPath else AppBackupConstants.NULL_MARKER,
                        "true",
                        AppBackupConstants.NULL_MARKER,
                        AppBackupConstants.END_MARKER,
                    )
                )

                Timber.d("Script finished for : ${appData.appName} - ${appData.packageName}")
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