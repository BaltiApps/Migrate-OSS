package balti.migrate.backupEngines.engines

import android.os.Build
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.containers.ZipAppBatch
//import balti.migrate.utilities.CommonToolKotlin.Companion.DATA_TEMP
import balti.migrate.utilities.CommonToolKotlin.Companion.DIR_MANUAL_CONFIGS
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_UPDATER_CONFIG_FILE
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_UPDATER_EXTRACT
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_UPDATER_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_UPDATER_SCRIPT
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_BUILDPROP_MANUAL
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_FILE_LIST
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_MIGRATE_CACHE_MANUAL
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_PACKAGE_DATA
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_SYSTEM_MANUAL
import balti.migrate.utilities.CommonToolKotlin.Companion.KB_DIVISION_SIZE
import balti.migrate.utilities.CommonToolKotlin.Companion.MIGRATE_CACHE_DEFAULT
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_MANUAL_BUILDPROP
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_MANUAL_MIGRATE_CACHE
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_MANUAL_SYSTEM
import balti.migrate.utilities.CommonToolKotlin.Companion.THIS_VERSION
import balti.migrate.utilities.ToolsNoContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

// extract helper, busybox, update-binary, mount_script.sh, prep.sh, helper_unpacking_script.sh, verify.sh

class UpdaterScriptMakerEngine(private val jobcode: Int, private val bd: BackupIntentData,
                               private val zipAppBatch: ZipAppBatch,
                               private val timeStamp: String) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_UPDATER_SCRIPT) {

    private val errors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }

    private fun extractToBackup(fileName: String, targetPath: String){
        val assetFile = File(commonTools.unpackAssetToInternal(fileName, fileName, false))
        val targetFile = File(targetPath, fileName)
        var err = ""

        File(targetPath).mkdirs()

        if (assetFile.exists())
            err = ToolsNoContext.moveFile(assetFile, targetPath)
        else errors.add("$ERR_UPDATER_EXTRACT${bd.batchErrorTag}: $fileName could not be unpacked")

        if (!targetFile.exists())
            errors.add("$ERR_UPDATER_EXTRACT${bd.batchErrorTag}: $fileName could not be moved: $err")
    }

    private fun makeUpdaterScript() {

        val updaterScriptPath = File("$actualDestination/META-INF/com/google/android/")
        updaterScriptPath.mkdirs()

        BufferedWriter(FileWriter(File(updaterScriptPath, "updater-script"))).let { updater_writer ->

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
            updater_writer.write("run_program(\"/tmp/prep.sh\", \"$MIGRATE_CACHE_DEFAULT\", \"$timeStamp\", \"$FILE_PACKAGE_DATA\", \"$DIR_MANUAL_CONFIGS\", \"${bd.backupName}\");\n")

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
            val packets = zipAppBatch.zipPackets
            val size = packets.size
            for (c in packets.indices) {

                if (BackupServiceKotlin.cancelAll) {
                    updater_writer.close()
                    break
                }

                val packet = packets[c].appPacket_z
                val packageName = packet.packageName

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
                extractItToTemp("$packageName.icon")

                var pString = String.format(Locale.ENGLISH, "%.4f", (c + 1) * 1.0 / size)
                pString = pString.replace(",", ".")
                updater_writer.write("set_progress($pString);\n")
            }

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
            updater_writer.write("ui_print(\"---------------------------------\");\n")
            updater_writer.write("ui_print(\"PLEASE ROOT YOUR ROM WITH MAGISK.\");\n")
            updater_writer.write("ui_print(\"YOU WILL BE PROMPTED TO CONTINUE RESTORE AFTER STARTUP!!\");\n")
            updater_writer.write("ui_print(\"---------------------------------\");\n")
            updater_writer.write("ui_print(\"Telegram: https://t.me/migrateApp\");\n")
            updater_writer.write("ui_print(\"---------------------------------\");\n")
            updater_writer.write("ui_print(\" \");\n")

            updater_writer.close()
        }

    }

    private fun makePackageData() {        // done

        val packageData = File(actualDestination, FILE_PACKAGE_DATA)
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
            val writer = BufferedWriter(FileWriter(packageData))
            writer.write(contents)
            writer.close()
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
            File(actualDestination, DIR_MANUAL_CONFIGS).run {
                mkdirs()
                BufferedWriter(FileWriter(File(this, fileName))).run {
                    sharedPreferences.getString(value, "")?.let { write(it) }
                    close()
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            warnings.add("$ERR_UPDATER_CONFIG_FILE${bd.batchErrorTag}: $fileName - ${e.message}")
        }
    }

    override fun doInBackground(vararg params: Any?): Any {

        try {

            val title = getTitle(R.string.making_updater_script)

            resetBroadcast(true, title)

            makePackageData()
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

            makeUpdaterScript()
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