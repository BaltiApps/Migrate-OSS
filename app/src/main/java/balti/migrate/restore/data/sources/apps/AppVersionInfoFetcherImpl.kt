package balti.migrate.restore.data.sources.apps

import android.content.Context
import android.content.pm.PackageManager
import baltiapps.migrate.domain.restore.sources.AppVersionInfoFetcher

class AppVersionInfoFetcherImpl(
    private val context: Context,
) : AppVersionInfoFetcher {
    @Suppress("DEPRECATION")
    override fun getInstalledAppVersionCode(packageName: String): Long {
        return try {
            context.packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
        } catch (e: PackageManager.NameNotFoundException) {
            -1
        }
    }
}
