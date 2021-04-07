package balti.migrate.backupEngines.engines

import android.os.Build
import balti.filex.FileX
import balti.filex.FileXInit
import balti.filex.Quad
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.BackupServiceKotlin.Companion.flasherOnly
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.containers.ZipAppBatch
import balti.migrate.utilities.CommonToolsKotlin.Companion.DIR_MANUAL_CONFIGS
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_UPDATER_CONFIG_FILE
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_UPDATER_EXTRACT
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_UPDATER_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_WRITING_RAW_LIST
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_UPDATER_SCRIPT
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
import balti.module.baltitoolbox.functions.Misc
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.*
import kotlin.collections.ArrayList

// extract helper, busybox, update-binary, mount_script.sh, prep.sh, helper_unpacking_script.sh, verify.sh

class UpdaterScriptMakerEngine(private val jobcode: Int, private val bd: BackupIntentData,
                               private val zipAppBatch: ZipAppBatch,
                               private val timeStamp: String) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_UPDATER_SCRIPT) {

    private val errors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }

    private val showNotification by lazy { !FileXInit.isTraditional }

    private fun extractToBackup(fileName: String, targetPath: String){
        val assetFile = FileX.new(unpackAssetToInternal(fileName, fileName, FileHandlers.INTERNAL_TYPE.INTERNAL_FILES), true)
        val targetFile = FileX.new(targetPath, fileName)
        var err = ""

        targetFile.createNewFile(makeDirectories = true, overwriteIfExists = true)
        targetFile.refreshFile()

        if (assetFile.exists())
            err = try {
                assetFile.copyTo(targetFile, overwrite = true)
                assetFile.delete()
                ""
            }catch (e: Exception){
                e.printStackTrace()
                e.message.toString()
            }
        else errors.add("$ERR_UPDATER_EXTRACT${bd.batchErrorTag}: $fileName could not be unpacked")

        if (!targetFile.exists())
            errors.add("$ERR_UPDATER_EXTRACT${bd.batchErrorTag}: $fileName could not be moved: $err")

    }

    private fun makeUpdaterScript() {

        val title = getTitle(R.string.making_updater_script)

        resetBroadcast(true, title)

        val updaterScriptPath = FileX.new("$actualDestination/META-INF/com/google/android/")
        updaterScriptPath.mkdirs()

        val updaterScriptFile = FileX.new(updaterScriptPath.path, "updater-script")
        updaterScriptFile.createNewFile(overwriteIfExists = true)
        updaterScriptFile.refreshFile()

        BufferedWriter(OutputStreamWriter(updaterScriptFile.outputStream())).let { updater_writer ->

            updater_writer.write("show_progress(0, 0);\n")
            updater_writer.write("ui_print(\" \");\n")
            updater_writer.write("ui_print(\"---------------------------------\");\n")
            updater_writer.write("ui_print(\"      ${engineContext.getString(R.string.app_name)} Flash package      \");\n")
            updater_writer.write("ui_print(\"      Version ${engineContext.getString(R.string.current_version_name)} - ${engineContext.getString(R.string.release_state)}       \");\n")
            updater_writer.write("ui_print(\"---------------------------------\");\n")

            zipAppBatch.partName.let {
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
            updater_writer.write("run_program(\"/tmp/prep.sh\", \"$MIGRATE_CACHE_DEFAULT\", \"$timeStamp\", \"$FILE_PACKAGE_DATA\", \"$DIR_MANUAL_CONFIGS\", \"${bd.backupName}\", \"$FILE_RAW_LIST\", \"$FILE_FILE_LIST\");\n")

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
            val packets = zipAppBatch.zipAppPackets
            val size = packets.size

            val subtask = getStringFromRes(R.string.writing_app_info)
            if (showNotification) {
                resetBroadcast(false, title)
            }

            for (c in packets.indices) {

                if (BackupServiceKotlin.cancelAll) {
                    updater_writer.close()
                    break
                }

                val packet = packets[c].appPacket_z
                val packageName = packet.packageName

                if (showNotification) {
                    val progress = Misc.getPercentage((c+1), size)
                    broadcastProgress(subtask, "$subtask: ${packet.appName}", true, progress)
                }

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

            resetBroadcast(true, title)

            zipAppBatch.extrasFiles.run {
                if (isNotEmpty()) {

                    // extract extras
                    updater_writer.write("ui_print(\" \");\n")
                    updater_writer.write("ui_print(\"Extracting extras...\");\n")

                    forEach {
                        updater_writer.write("ui_print(\"${it.name}\");\n")
                        extractItToTemp(it.name)
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

    private fun makePackageData() {        // done

        val packageData = FileX.new(actualDestination, FILE_PACKAGE_DATA)
        var contents = ""

        contents += "backup_name ${bd.backupName}\n"
        contents += "timestamp $timeStamp\n"
        contents += "device " + Build.DEVICE + "\n"
        contents += "sdk " + Build.VERSION.SDK_INT + "\n"
        contents += "cpu_abi " + Build.SUPPORTED_ABIS[0] + "\n"
        contents += "data_required_size ${zipAppBatch.batchDataSize / KB_DIVISION_SIZE}\n"
        contents += "system_required_size ${zipAppBatch.batchSystemSize / KB_DIVISION_SIZE}\n"
        contents += "zip_expected_size ${zipAppBatch.zipFullSize / KB_DIVISION_SIZE}\n"
        contents += "migrate_version " + "${engineContext.getString(R.string.app_name)}_" +
                "${engineContext.getString(R.string.current_version_name)}_" +
                engineContext.getString(R.string.release_state) + "\n"

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
        try {
            FileX.new(actualDestination, DIR_MANUAL_CONFIGS).run {
                mkdirs()
                FileX.new(this.path, fileName).writeOneLine(getPrefString(value, ""))
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            warnings.add("$ERR_UPDATER_CONFIG_FILE${bd.batchErrorTag}: $fileName - ${e.message}")
        }
    }

    override suspend fun doInBackground(arg: Any?): Any? {

        try {

            val title = getTitle(R.string.recording_raw_list)

            resetBroadcast(true, title)

            makePackageData()

            if (!flasherOnly) {
                getStringFromRes(R.string.unpacking_to_make_flashable).let { broadcastProgress(it, it, false) }
                extractToBackup("busybox", actualDestination)
                extractToBackup("update-binary", "$actualDestination/META-INF/com/google/android/")
                extractToBackup("mount_script.sh", actualDestination)
                extractToBackup("prep.sh", actualDestination)
                extractToBackup("mover.sh", actualDestination)
                extractToBackup("helper_unpacking_script.sh", actualDestination)
                extractToBackup("verify.sh", actualDestination)
                extractToBackup("MigrateHelper.apk", "$actualDestination/system/app/MigrateHelper/")
                extractToBackup(getBusyboxAssetName(), actualDestination)

                writeManualConfig(FILE_MIGRATE_CACHE_MANUAL, PREF_MANUAL_MIGRATE_CACHE)
                writeManualConfig(FILE_SYSTEM_MANUAL, PREF_MANUAL_SYSTEM)
                writeManualConfig(FILE_BUILDPROP_MANUAL, PREF_MANUAL_BUILDPROP)
            }


            val rawList = FileX.new(actualDestination, FILE_RAW_LIST)
            val subTask = getStringFromRes(R.string.recording_raw_list)
            try {
                if (showNotification) {
                    resetBroadcast(false, title)
                    subTask.let { broadcastProgress(it, it, true) }
                }
                else subTask.let { broadcastProgress(it, it, false) }
                heavyTask {
                    rawList.startWriting(object : FileX.Writer() {
                        override fun writeLines() {
                            write("\n")
                            write("=================\n")
                            write("${actualDestination}\n")
                            write("=================\n")
                            write("\n")

                            // old code
                            /*fun getRelativePath(file: FileX): String =
                                    file.path.let {
                                        it.substring(actualDestination.length)
                                    }

                            fun scanAllFiles(directory: FileX) {
                                if (directory.isFile) write("${getRelativePath(directory)}\n")
                                else directory.listFiles()?.let {
                                    for (f in it) {
                                        scanAllFiles(f)
                                    }
                                }
                            }

                            scanAllFiles(FileX.new(actualDestination))*/
                            if (showNotification) {
                                resetBroadcast(false, title)
                            }

                            val rootDir = FileX.new(actualDestination)
                            val all = rootDir.listEverythingInQuad() ?: listOf()
                            for (i in all.indices) {
                                if (showNotification) {
                                    val progress = Misc.getPercentage(i+1, all.size)
                                    broadcastProgress(subTask, "", true, progress)
                                }
                                val q = all[i]
                                if (q.second) {
                                    FileX.new(rootDir.path, q.first).list()?.forEach {
                                        writeLine("${q.first}/$it")
                                    }
                                }
                                else writeLine(q.first)
                            }
                        }
                    })
                }
            }
            catch (e: Exception){
                e.printStackTrace()
                warnings.add("$ERR_WRITING_RAW_LIST${bd.batchErrorTag}: ${e.message}")
            }

            tryIt {
                val extRawList = FileX.new(CACHE_DIR, FILE_RAW_LIST, true)

                extRawList.startWriting(object : FileX.Writer(){
                    override fun writeLines() {
                        if (rawList.exists())
                            rawList.readLines().forEach {
                                write("$it\n")
                            }
                    }
                }, true)
            }

            if (!flasherOnly) makeUpdaterScript()
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_UPDATER_TRY_CATCH${bd.batchErrorTag}: ${e.message}")
        }

        return 0
    }

    override fun postExecuteFunction() {
        onEngineTaskComplete.onComplete(jobcode, errors, warnings)
    }

}