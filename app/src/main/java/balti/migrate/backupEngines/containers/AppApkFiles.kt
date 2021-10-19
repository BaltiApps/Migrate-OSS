package balti.migrate.backupEngines.containers

/**
 * A container to store string list of APK files for an app. Stores the base apk and split apk names.
 * A list of such objects should significantly speed up creating raw file list in in
 * [balti.migrate.backupEngines.engines.UpdaterScriptMakerEngine.createRawList] as files do not need to be read again.
 *
 * @param packageName Package name of the app for whom the APK files are being recorded.
 * @param apkFileList List of backed up APK files (base APK and split APKs if any).
 */
data class AppApkFiles(val packageName: String, val apkFileList: List<String>) {
}