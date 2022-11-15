package balti.migrate.backupEngines.engines

import android.os.Build
import balti.filex.FileX
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.BuildConfig
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin_new.Companion.flasherOnly
import balti.migrate.backupEngines.ParentBackupClass_new
import balti.migrate.backupEngines.containers.ZipBatch
import balti.migrate.backupEngines.utils.BackupUtils
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.ProgressType.PROGRESS_TYPE_UPDATER_SCRIPT
import balti.migrate.utilities.CommonToolsKotlin.Companion.DIR_MANUAL_CONFIGS
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_UPDATER_CONFIG_FILE
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_UPDATER_EXTRACT
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_UPDATER_MOVE_AUX
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_UPDATER_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_WRITING_RAW_LIST
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_WRITING_RAW_LIST_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_BUILDPROP_MANUAL
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_FILE_LIST
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_MIGRATE_CACHE_MANUAL
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_PACKAGE_DATA
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_RAW_LIST
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_SYSTEM_MANUAL
import balti.migrate.utilities.CommonToolsKotlin.Companion.KB_DIVISION_SIZE
import balti.migrate.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE_DEFAULT
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_MANUAL_BUILDPROP
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_MANUAL_MIGRATE_CACHE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_MANUAL_SYSTEM
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_NEW_ICON_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.THIS_VERSION
import balti.module.baltitoolbox.functions.FileHandlers
import balti.module.baltitoolbox.functions.FileHandlers.unpackAssetToInternal
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc.iterateBufferedReader
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import java.io.*
import java.util.*


// extract helper, busybox, update-binary, mount_script.sh, prep.sh, helper_unpacking_script.sh, verify.sh

class UpdaterScriptMakerEngine(
    override val partTag: String,
    private val zipBatch: ZipBatch,
    private val timeStamp: String
) : ParentBackupClass_new(PROGRESS_TYPE_UPDATER_SCRIPT) {

    override val className: String = "UpdaterScriptMakerEngine"

    private val backupUtils by lazy { BackupUtils() }

    override fun preExecute() {
        super.preExecute()
        appAuxFilesDir.run { deleteRecursively(); mkdirs() }
    }

    /**
     * This function is used to unpack things like MigrateHelper.apk, update-binary
     * and various assets to make a ZIP flashable via TWRP.
     *
     * @param assetName The Actual raw name of the asset packed with the apk of this app.
     * @param unpackName The final name of the unpacked asset.
     * @param unpackSubDirectory Optional path under which [appAuxFilesDir] under which
     * the asset is to be unpacked. Example: update-binary needs to be under a directory structure:
     * `META-INF/com/google/android/`
     */
    private fun extractToBackup(assetName: String, unpackName: String = assetName, unpackSubDirectory: String = ""){

        if (cancelBackup) return

        val assetFile = FileX.new(unpackAssetToInternal(assetName, unpackName, FileHandlers.INTERNAL_TYPE.INTERNAL_CACHE), true)
        val targetFile = FileX.new("${pathForAuxFiles}/$unpackSubDirectory", unpackName, true)
        targetFile.parentFile?.mkdirs()
        var err = ""

        if (assetFile.exists())
            err = try {
                assetFile.renameTo(targetFile)
                assetFile.delete()
                ""
            }catch (e: Exception){
                e.printStackTrace()
                e.message.toString()
            }
        else errors.add("$ERR_UPDATER_EXTRACT${partTag}: $assetName could not be unpacked")

        if (!targetFile.exists())
            errors.add("$ERR_UPDATER_EXTRACT${partTag}: $assetName could not be moved: $err")

    }

    private fun makeUpdaterScript() {

        val title = getTitle(R.string.making_updater_script)

        resetBroadcast(true, title)

        val updaterScriptPath = FileX.new("$pathForAuxFiles/META-INF/com/google/android/", true)
        updaterScriptPath.mkdirs()

        val updaterScriptFile = FileX.new(updaterScriptPath.path, "updater-script", true)
        updaterScriptFile.createNewFile(overwriteIfExists = true)
        updaterScriptFile.refreshFile()

        BufferedWriter(OutputStreamWriter(updaterScriptFile.outputStream())).let { updater_writer ->

            updater_writer.write("show_progress(0, 0);\n")
            updater_writer.write("ui_print(\" \");\n")
            updater_writer.write("ui_print(\"---------------------------------\");\n")
            updater_writer.write("ui_print(\"      ${getStringFromRes(R.string.app_name)} Flash package      \");\n")
            updater_writer.write("ui_print(\"      Version ${BuildConfig.SHORT_VERSION_NAME} - ${BuildConfig.RELEASE_STATE}       \");\n")
            updater_writer.write("ui_print(\"---------------------------------\");\n")

            zipBatch.zipName.let {
                if (it != "") {
                    updater_writer.write("ui_print(\"*** $it ***\");\n")
                    updater_writer.write("ui_print(\"---------------------------------\");\n")
                }
            }

            // extract scripts and other files
            fun extractFile(fileName: String) {
                updater_writer.write("package_extract_file(\"$fileName\", \"/tmp/$fileName\");\n")
            }
            extractFile("busybox")
            extractFile("mount_script.sh")
            extractFile("prep.sh")
            extractFile(FILE_PACKAGE_DATA)
            extractFile("helper_unpacking_script.sh")
            extractFile("verify.sh")
            extractFile(FILE_FILE_LIST)
            extractFile(FILE_RAW_LIST)
            extractFile("mover.sh")
            updater_writer.write("package_extract_dir(\"$DIR_MANUAL_CONFIGS\", \"/tmp/$DIR_MANUAL_CONFIGS\");\n")

            // set permission to scripts
            fun set777Permission(fileName: String) {
                updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"/tmp/$fileName\");\n")
            }
            set777Permission("busybox")
            set777Permission("mount_script.sh")
            set777Permission("prep.sh")
            set777Permission(FILE_PACKAGE_DATA)
            set777Permission("helper_unpacking_script.sh")
            set777Permission("verify.sh")
            set777Permission(FILE_FILE_LIST)
            set777Permission(FILE_RAW_LIST)
            set777Permission("mover.sh")
            set777Permission(DIR_MANUAL_CONFIGS)

            // mount partitions
            updater_writer.write("ui_print(\" \");\n")
            updater_writer.write("ui_print(\"Mounting partition...\");\n")
            updater_writer.write("run_program(\"/tmp/mount_script.sh\", \"m\", \"$DIR_MANUAL_CONFIGS\");\n")

            // exit if mount failed
            updater_writer.write("ifelse(is_mounted(\"/data\"), ui_print(\"Mounted data!\"), abort(\"Mount failed data! Exiting...\"));\n")
            updater_writer.write("ifelse((is_mounted(\"/system\") || is_mounted(\"/system_root\")), ui_print(\"Mounted system!\"), " +
                    "ui_print(\"Mount failed system! Migrate helper will not be automatically installed!\"));\n")

            // run prep.sh
            //updater_writer.write("run_program(\"/tmp/prep.sh\", \"$MIGRATE_CACHE_DEFAULT\", \"$timeStamp\", \"$FILE_PACKAGE_DATA\", \"$DATA_TEMP\", \"$DIR_MANUAL_CONFIGS\");\n")
            updater_writer.write("run_program(\"/tmp/prep.sh\", \"$MIGRATE_CACHE_DEFAULT\", \"$timeStamp\", \"$FILE_PACKAGE_DATA\", \"$DIR_MANUAL_CONFIGS\", \"${zipBatch.zipName}\", \"$FILE_RAW_LIST\", \"$FILE_FILE_LIST\");\n")

            // data will be unmounted if prep.sh aborted unsuccessfully
            updater_writer.write("ifelse(is_mounted(\"/data\"), ui_print(\"Parameters checked!\") && sleep(2s), abort(\"Exiting...\"));\n")
            updater_writer.write("ui_print(\" \");\n")

            // ready to restore to migrate cache
            updater_writer.write("ui_print(\"Restoring to Migrate cache...\");\n")
            updater_writer.write("ui_print(\" \");\n")

            // extract to temp
            fun extractItToTemp(fileName: String, isFile: Boolean = true){
                //updater_writer.write("package_extract_${if (isFile) "file" else "dir"}(\"$fileName\", \"$DATA_TEMP/$fileName\");\n")
                updater_writer.write("package_extract_${if (isFile) "file" else "dir"}(\"$fileName\", \"$MIGRATE_CACHE_DEFAULT/$fileName\");\n")
            }

            // extract app files
            val packets = zipBatch.appPackets
            val size = packets.size

            var subtask = getStringFromRes(R.string.writing_app_info)

            for (c in packets.indices) {

                if (cancelBackup) {
                    updater_writer.close()
                    break
                }

                val packet = packets[c].appPacket_z
                val packageName = packet.packageName

                broadcastProgress(subtask, "${getStringFromRes(R.string.app)}: ${packet.appName}", false)

                if (packet.APP || packet.DATA || packet.PERMISSION) {
                    updater_writer.write("ui_print(\"${packet.appName} (${c + 1}/$size)\");\n")
                }

                if (packet.APP) {
                    //val apkPath = packet.apkPath

                    //if (!apkPath.startsWith("/data")) {
                    if (packet.isSystem) {
                        updater_writer.write("package_extract_dir(\"$packageName.app\", \"/tmp/$packageName.app\");\n")
                        updater_writer.write("package_extract_file(\"$packageName.sh\", \"/tmp/$packageName.sh\");\n")
                        updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"/tmp/$packageName.sh\");\n")
                        updater_writer.write("run_program(\"/tmp/$packageName.sh\");\n")
                    }
                    else extractItToTemp("$packageName.app", false)
                }

                if (packet.DATA)
                    updater_writer.write("package_extract_file(\"$packageName.tar.gz\", \"/data/data/$packageName.tar.gz\");\n")

                if (packet.PERMISSION)
                    extractItToTemp("$packageName.perm")

                extractItToTemp("$packageName.json")
                if (getPrefBoolean(PREF_NEW_ICON_METHOD, true)) extractItToTemp("$packageName.png")
                else extractItToTemp("$packageName.icon")

                var pString = String.format(Locale.ENGLISH, "%.4f", (c + 1) * 1.0 / size)
                pString = pString.replace(",", ".")
                updater_writer.write("set_progress($pString);\n")
            }

            subtask = getStringFromRes(R.string.writing_extra_info)

            resetBroadcast(true, title)

            zipBatch.extraPackets.run {
                if (isNotEmpty()) {

                    // extract extras
                    updater_writer.write("ui_print(\" \");\n")
                    updater_writer.write("ui_print(\"Extracting extras...\");\n")

                    forEach {

                        broadcastProgress(subtask, "${getStringFromRes(R.string.extras)}: ${it.displayName}", false)

                        updater_writer.write("ui_print(\"${it.displayName}\");\n")
                        it.extraFiles.forEach { f ->
                            extractItToTemp(f.first)
                        }
                    }
                }
            }

            // move everything
            //updater_writer.write("run_program(\"/tmp/mover.sh\", \"$DATA_TEMP\", \"$MIGRATE_CACHE_DEFAULT\", \"$DIR_MANUAL_CONFIGS\");\n")
            updater_writer.write("run_program(\"/tmp/mover.sh\", \"$MIGRATE_CACHE_DEFAULT\", \"$DIR_MANUAL_CONFIGS\");\n")
            updater_writer.write("ui_print(\" \");\n")

            // unpack helper
            updater_writer.write("ui_print(\"Unpacking helper\");\n")
            updater_writer.write("package_extract_dir(\"system\", \"/tmp/system\");\n")
            updater_writer.write("run_program(\"/tmp/helper_unpacking_script.sh\", \"/tmp/system/\", \"$THIS_VERSION\", \"$DIR_MANUAL_CONFIGS\");\n")

            updater_writer.write("set_progress(1.0000);\n")

            // verification
            //updater_writer.write("run_program(\"/tmp/verify.sh\", \"$FILE_FILE_LIST\", \"$timeStamp\", \"$MIGRATE_CACHE_DEFAULT\", \"$DATA_TEMP\", \"$DIR_MANUAL_CONFIGS\");\n")
            updater_writer.write("run_program(\"/tmp/verify.sh\", \"$FILE_FILE_LIST\", \"$timeStamp\", \"$MIGRATE_CACHE_DEFAULT\", \"$DIR_MANUAL_CONFIGS\");\n")

            // un-mount partitions
            updater_writer.write("ui_print(\" \");\n")
            updater_writer.write("ui_print(\"Unmounting partition...\");\n")
            updater_writer.write("run_program(\"/tmp/mount_script.sh\", \"u\", \"$DIR_MANUAL_CONFIGS\");\n")

            updater_writer.write("ui_print(\" \");\n")
            updater_writer.write("ui_print(\"Finished!\");\n")
            updater_writer.write("ui_print(\" \");\n")
            updater_writer.write("ui_print(\"Files have been restored to Migrate cache.\");\n")
            updater_writer.write("ui_print(\"=================================\");\n")
            updater_writer.write("ui_print(\"PLEASE ROOT YOUR ROM WITH MAGISK.\");\n")
            updater_writer.write("ui_print(\"Reboot -> Wait 1 minute for notification\");\n")
            updater_writer.write("ui_print(\"Tap notification from Migrate helper to continue restore.\");\n")
            updater_writer.write("ui_print(\"---------------------------------\");\n")
            updater_writer.write("ui_print(\"If Migrate helper is not found...\");\n")
            updater_writer.write("ui_print(\"Please install it from Google Play\");\n")
            updater_writer.write("ui_print(\"Or: Internal Storage/Migrate/helper.apk\");\n")
            updater_writer.write("ui_print(\"=================================\");\n")
            updater_writer.write("ui_print(\"Telegram: https://t.me/migrateApp\");\n")
            updater_writer.write("ui_print(\"=================================\");\n")
            updater_writer.write("ui_print(\" \");\n")

            updater_writer.close()
        }

    }

    private fun makePackageData() {

        val packageData = FileX.new(pathForAuxFiles, FILE_PACKAGE_DATA, true)
        var contents = ""

        contents += "backup_name ${zipBatch.zipName}\n"
        contents += "timestamp $timeStamp\n"
        contents += "device " + Build.DEVICE + "\n"
        contents += "sdk " + Build.VERSION.SDK_INT + "\n"
        contents += "cpu_abi " + Build.SUPPORTED_ABIS[0] + "\n"
        contents += "data_required_size ${zipBatch.batchDataSize / KB_DIVISION_SIZE}\n"
        contents += "system_required_size ${zipBatch.batchSystemSize / KB_DIVISION_SIZE}\n"
        contents += "zip_expected_size ${zipBatch.zipFullSize / KB_DIVISION_SIZE}\n"
        contents += "migrate_version " + "${getStringFromRes(R.string.app_name)}_" +
                "${BuildConfig.SHORT_VERSION_NAME}_${BuildConfig.RELEASE_STATE}\n"

        try {
            packageData.writeOneLine(contents)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun getBusyboxAssetName(): String {
        val cpuAbi = Build.SUPPORTED_ABIS[0]
        return if (cpuAbi == "x86" || cpuAbi == "x86_64") "busybox-x86" else "busybox"
    }

    private fun writeManualConfig(fileName: String, value: String){

        if (cancelBackup) return

        try {
            FileX.new(pathForAuxFiles, DIR_MANUAL_CONFIGS, true).run {
                mkdirs()
                FileX.new(this.path, fileName, true).writeOneLine(getPrefString(value, ""))
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            warnings.add("$ERR_UPDATER_CONFIG_FILE${partTag}: $fileName - ${e.message}")
        }
    }

    /**
     * This function creates a list of all the files in a batch to be zipped together.
     * This includes updater-script, MigrateHelper APK, and all other files to make the zip flashable.
     * Also for verification in [ZipVerificationEngine]
     */
    private suspend fun createRawList(){

        val title = getTitle(R.string.recording_raw_list)
        resetBroadcast(true, title)

        /**
         *  This file will be shipped inside the zip.
         *  This file is also used during zipping process in [ZippingEngine.getAllFiles].
         *  File is formatted as:
         *  =================
         *  <backup_name>
         *  =================
         *  ./<file1>
         *  ./<dir1>/<dir2>/<file2>
         *  ./system/app/MigrateHelper/MigrateHelper.apk
         *  ...
         */
        val rawList = FileX.new("$fileXDestination/${zipBatch.zipName}", FILE_RAW_LIST)

        /**
         * This file is a copy of the original raw file list created above.
         * This file will also be included while reporting logs via [balti.migrate.utilities.CommonToolsKotlin.reportLogs]
         */
        val extRawList = FileX.new(CACHE_DIR, "${FILE_RAW_LIST}_${zipBatch.zipName}.txt", true)

        try {
            heavyTask {
                val suProcess = Runtime.getRuntime().exec("su")
                suProcess?.let {
                    val suInputStream = BufferedWriter(OutputStreamWriter(it.outputStream))
                    val errorStream = BufferedReader(InputStreamReader(it.errorStream))

                    suInputStream.write("walk_dir () {\n")
                    suInputStream.write("    for pathname in \"\$1\"/*; do\n")
                    suInputStream.write("        if [ -d \"\$pathname\" ]; then\n")
                    suInputStream.write("            walk_dir \"\$pathname\"\n")
                    suInputStream.write("        else\n")
                    suInputStream.write("            printf '%s\\n' \"\$pathname\"\n")
                    suInputStream.write("        fi\n")
                    suInputStream.write("    done\n")
                    suInputStream.write("}\n")

                    suInputStream.write("touch ${rawList.canonicalPath}\n")

                    val displayName: String = zipBatch.zipName.let { zn ->
                        if (zn.isBlank()) backupName
                        else "$backupName - $zn"
                    }

                    suInputStream.write("echo \"=================\" > ${rawList.canonicalPath}\n")
                    suInputStream.write("echo \"$displayName\" >> ${rawList.canonicalPath}\n")
                    suInputStream.write("echo \"=================\" >> ${rawList.canonicalPath}\n")

                    val batchRootLocation = FileX.new(fileXDestination, zipBatch.zipName)

                    suInputStream.write("cd ${batchRootLocation.canonicalPath}\n")
                    suInputStream.write("walk_dir . >> ${rawList.canonicalPath}\n")
                    suInputStream.write("cat ${rawList.canonicalPath} > ${extRawList.canonicalPath}\n")
                    suInputStream.write("exit\n")
                    suInputStream.flush()

                    iterateBufferedReader(errorStream, { errorLine ->
                        errors.add("$ERR_WRITING_RAW_LIST${partTag}: ${errorLine}")
                        return@iterateBufferedReader false
                    })
                }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            warnings.add("$ERR_WRITING_RAW_LIST_CATCH${partTag}: ${e.message}")
        }
    }

    override suspend fun backgroundProcessing(): Any? {

        try {

            val title = getTitle(R.string.making_zip_flashable)

            resetBroadcast(true, title)

            makePackageData()

            if (!flasherOnly) {
                getStringFromRes(R.string.unpacking_to_make_flashable).let { broadcastProgress(it, it, false) }
                extractToBackup(getBusyboxAssetName(), "busybox")
                extractToBackup("update-binary", unpackSubDirectory = "META-INF/com/google/android/")
                extractToBackup("mount_script.sh")
                extractToBackup("prep.sh")
                extractToBackup("mover.sh")
                extractToBackup("helper_unpacking_script.sh")
                extractToBackup("verify.sh")
                extractToBackup("MigrateHelper.apk")

                writeManualConfig(FILE_MIGRATE_CACHE_MANUAL, PREF_MANUAL_MIGRATE_CACHE)
                writeManualConfig(FILE_SYSTEM_MANUAL, PREF_MANUAL_SYSTEM)
                writeManualConfig(FILE_BUILDPROP_MANUAL, PREF_MANUAL_BUILDPROP)
            }

            if (!cancelBackup && !flasherOnly) makeUpdaterScript()

            /**
             * Finally move all file under [appAuxFilesDir] to actual backup location.
             */
            if (!cancelBackup) {
                backupUtils.moveAuxFilesToBackupLocation(
                    pathForAuxFiles,
                    "${rootLocation.canonicalPath}/${zipBatch.zipName}"
                ).forEach {
                    errors.add("$ERR_UPDATER_MOVE_AUX${partTag}: $it")
                }
            }

            if (!cancelBackup) createRawList()
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_UPDATER_TRY_CATCH${partTag}: ${e.message}")
        }

        return null
    }

}