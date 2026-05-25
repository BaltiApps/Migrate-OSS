package baltiapps.migrate.domain.restore.sources

interface AppVersionInfoFetcher {
    fun getInstalledAppVersionCode(packageName: String): Long
}
