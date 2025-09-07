package balti.migrate.backup.data.sources.apps

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import balti.migrate.common.data.model.AppData
import baltiapps.migrate.domain.backup.sources.DataSource
import baltiapps.migrate.domain.common.getPercentage
import baltiapps.migrate.domain.common.model.Progress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AppListSource(
    private val context: Context,
): DataSource<AppData> {
    override fun checkPermission(): Boolean = true

    @SuppressLint("QueryPermissionsNeeded")
    override suspend fun getData(onFinishedLoading: (List<AppData>) -> Unit): Flow<Progress> {
        val dataList = mutableListOf<AppData>()

        return flow {

            val pm = context.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            val total = apps.size

            apps.forEachIndexed { index, app ->
                val appPackageInfo =
                    pm.getPackageInfo(app.packageName, PackageManager.GET_META_DATA)
                val packageInfoForPermissions =
                    pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)

                val appName = pm.getApplicationLabel(app).toString()
                val packageName = app.packageName

                val appData = AppData(
                    packageName = packageName,
                    appName = appName,
                    appIcon = pm.getApplicationIcon(app),

                    versionCode = PackageInfoCompat.getLongVersionCode(appPackageInfo),
                    versionName = appPackageInfo.versionName ?: "",

                    apkPath = app.sourceDir,
                    dataPath = app.dataDir,

                    grantedPermissionList = packageInfoForPermissions.run {
                        requestedPermissions?.mapIndexedNotNull { index, perm ->
                            perm.takeIf {
                                requestedPermissionsFlags?.get(index)
                                    ?.and(PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                            }
                        } ?: emptyList()
                    },

                    uid = app.uid,
                    gid = app.uid,

                    isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isUpdatedSystemApp = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,

                    logInfo = "$appName : ($packageName)"
                )

                dataList.add(appData)

                emit(
                    Progress(
                        progressType = Progress.ProgressType.APP_LIST_READ,
                        percentage = getPercentage(index + 1, total),
                        logs = appData.logInfo,
                    )
                )
            }

            onFinishedLoading(dataList)

        }.flowOn(Dispatchers.Default)
    }
}