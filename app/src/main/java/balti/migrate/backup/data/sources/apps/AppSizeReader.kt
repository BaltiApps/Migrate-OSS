package balti.migrate.backup.data.sources.apps

import android.content.Context
import balti.migrate.R
import balti.migrate.common.data.model.AppData
import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.AppBackupConstants
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.AppSizeInfo
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.sources.fileSystem.GenericReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

class AppSizeReader(
    private val applicationContext: Context,
    private val superuserUtils: SuperuserUtils,
): GenericReader<List<DataItem<AppListItem>>, List<AppSizeInfo>> {

    private lateinit var suShell: Process

    override fun setup(readLocation: String) {
        // Not used as this reader calculates sizes from the system
    }

    override fun read(
        data: List<DataItem<AppListItem>>,
        onComplete: (List<AppSizeInfo>) -> Unit
    ): Flow<Progress> {
        return callbackFlow {
            suShell = superuserUtils.getSuperuserShell()
            val sizes = mutableListOf<AppSizeInfo>()

            val scriptLocation = "${applicationContext.cacheDir}/get_app_consumed_space.sh"
            val unpackResult = superuserUtils.unpackScript(R.raw.get_app_consumed_space, scriptLocation)

            if (unpackResult.isFailure) {
                trySend(
                    Progress(
                        progressType = Progress.ProgressType.APP_SIZE_READ,
                        percentage = 0.0,
                        logs = "Unpack error - ${unpackResult.exceptionOrNull()?.message}",
                        isFailure = true
                    )
                )
                close()
                return@callbackFlow
            }

            val packageNames = data.map { it._id }

            data.forEachIndexed { index, dataItem ->
                val appData = dataItem as? AppData ?: return@forEachIndexed
                Timber.d("Run script for : ${appData.appName} - ${appData.packageName}")
                val percentage = getPercentage(index + 1, data.size)

                superuserUtils.runScript(
                    scriptPath = scriptLocation,
                    parentSuperuserShell = suShell,
                    endMarker = AppBackupConstants.END_MARKER,
                    onProgress = { log ->
                        // Expected format: package_name:APK:bytes:DATA:bytes
                        log.split(":")
                            .takeIf { it.size == 5 && it[1] == "APK" && it[3] == "DATA" }
                            ?.let { (packageName, _, apkStr, _, dataStr) ->
                                if (packageName in packageNames) {
                                    sizes.add(
                                        AppSizeInfo(
                                            packageName = packageName,
                                            bytesApk = apkStr.toLongOrNull() ?: 0L,
                                            bytesData = dataStr.toLongOrNull() ?: 0L,
                                        )
                                    )
                                }
                            }
                        trySend(
                            Progress(
                                progressType = Progress.ProgressType.APP_SIZE_READ,
                                percentage = percentage,
                                logs = log,
                                displayText = appData.appName
                            )
                        )
                    },
                    onError = { error ->
                        trySend(
                            Progress(
                                progressType = Progress.ProgressType.APP_SIZE_READ,
                                percentage = percentage,
                                logs = error,
                            )
                        )
                    },
                    args = arrayOf(
                        appData.packageName,
                        if (appData.shouldBackupApk) appData.apkPathBase else AppBackupConstants.NULL_MARKER,
                        if (appData.shouldBackupData) appData.dataPath else AppBackupConstants.NULL_MARKER,
                        AppBackupConstants.NULL_MARKER,
                        AppBackupConstants.END_MARKER,
                    )
                )
            }

            Timber.d("Close callbackFlow")
            close()
            awaitClose {
                onComplete(sizes)
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun close() {
        if (::suShell.isInitialized) {
            superuserUtils.closeSuperuserShell(suShell)
        }
    }
}
