package balti.migrate.backupEngines.engines

import android.os.Build
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.containers.ZipAppBatch
import balti.migrate.extraBackupsActivity.apps.containers.AppBatch
import balti.migrate.utilities.CommonToolKotlin.Companion.DATA_TEMP
import balti.migrate.utilities.CommonToolKotlin.Companion.DIR_MANUAL_CONFIGS
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_UPDATER_EXTRACT
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_UPDATER_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_UPDATER_SCRIPT
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_FILE_LIST
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_MIGRATE_CACHE_MANUAL
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_PACKAGE_DATA
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_SYSTEM_MANUAL
import balti.migrate.utilities.CommonToolKotlin.Companion.MIGRATE_CACHE_DEFAULT
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
                               private val zipBatch: ZipAppBatch?,
                               private val timeStamp: String,
                               private val contactsFileName: String?,
                               private val smsFileName: String?,
                               private val callsFileName: String?,
                               private val settingsFileName: String?,
                               private val wifiFileName: String?,
                               private val partName: String = "") : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_UPDATER_SCRIPT) {

    //private val pm by lazy { engineContext.packageManager }

    private val miscFiles by lazy { ArrayList<File>(0) }
    private val errors by lazy { ArrayList<String>(0) }

    private fun extractToBackup(fileName: String, destPath: String, customDir: String? = null){
        val assetFile = File(commonTools.unpackAssetToInternal(fileName, fileName, false))
        val targetFile = File(destPath, fileName)
        var err = ""

        File(destPath).mkdirs()
        if (targetFile.exists()) targetFile.delete()

        if (assetFile.exists())
            err = ToolsNoContext.moveFile(assetFile, destPath)
        else errors.add("$ERR_UPDATER_EXTRACT: $fileName could not be unpacked")

        if (!targetFile.exists())
            errors.add("$ERR_UPDATER_EXTRACT: $fileName could not be moved: $err")
        else {
            if (customDir != null) miscFiles.add(File(customDir))
            else miscFiles.add(targetFile)
        }
    }

    private fun makeUpdaterScript() {

        val updaterScriptPath = File("$actualDestination/META-INF/com/google/android/")
        updaterScriptPath.mkdirs()

        BufferedWriter(FileWriter(File(updaterScriptPath, "updater-script"))).let { updater_writer ->

            updater_writer.write("show_progress(0, 0);\n")
            updater_writer.write("ui_print(\" \");\n")
            updater_writer.write("ui_print(\"---------------------------------\");\n")
            updater_writer.write("ui_print(\"      Migrate Flash package      \");\n")
            updater_writer.write("ui_print(\"---------------------------------\");\n")
            updater_writer.write("ui_print(\"${engineContext.getString(R.string.current_version_name)} - ${engineContext.getString(R.string.current_version_codename)} - ${engineContext.getString(R.string.release_state)}\");\n")

            if (partName != "") {
                updater_writer.write("ui_print(\"*** $partName ***\");\n")
                updater_writer.write("ui_print(\"---------------------------------\");\n")
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
            set777Permission("migrate/$FILE_MIGRATE_CACHE_MANUAL")
            set777Permission("migrate/$FILE_SYSTEM_MANUAL")
            set777Permission(DIR_MANUAL_CONFIGS)

            // mount partitions
            updater_writer.write("ui_print(\" \");\n")
            updater_writer.write("ui_print(\"Mounting partition...\");\n")
            updater_writer.write("run_program(\"/tmp/mount_script.sh\", \"m\");\n")

            // exit if mount failed
            updater_writer.write("ifelse(is_mounted(\"/data\"), ui_print(\"Mounted data!\"), abort(\"Mount failed data! Exiting...\"));\n")
            updater_writer.write("ifelse((is_mounted(\"/system\") || is_mounted(\"/system_root\")), ui_print(\"Mounted system!\"), " +
                    "ui_print(\"Mount failed system! Migrate helper will not be automatically installed!\"));\n")

            // run prep.sh
            updater_writer.write("run_program(\"/tmp/prep.sh\", \"$MIGRATE_CACHE_DEFAULT\", \"$timeStamp\", \"$FILE_PACKAGE_DATA\", \"$DATA_TEMP\", \"$DIR_MANUAL_CONFIGS\");\n")

            // data will be unmounted if prep.sh aborted unsuccessfully
            updater_writer.write("ifelse(is_mounted(\"/data\"), ui_print(\"Parameters checked!\") && sleep(2s), abort(\"Exiting...\"));\n")
            updater_writer.write("ui_print(\" \");\n")

            // ready to restore to migrate cache
            updater_writer.write("ui_print(\"Restoring to Migrate cache...\");\n")
            updater_writer.write("ui_print(\" \");\n")

            // extract to temp
            fun extractItToTemp(fileName: String, isFile: Boolean = true){
                updater_writer.write("package_extract_${if (isFile) "file" else "dir"}(\"$fileName\", \"$DATA_TEMP/$fileName\");\n")
            }

            // extract app files
            zipBatch?. run {
                val size = zipPackets.size
                for (c in zipPackets.indices) {

                    if (BackupServiceKotlin.cancelAll) {
                        updater_writer.close()
                        break
                    }

                    val packet = zipPackets[c].appPacket_z
                    val packageName = packet.packageName

                    if (packet.APP || packet.DATA || packet.PERMISSION) {
                        updater_writer.write("ui_print(\"${packet.appName} (${c + 1}/$size)\");\n")
                    }

                    if (packet.APP) {

                        if (!packet.apkPath.startsWith("/data")) {
                            updater_writer.write("package_extract_dir(\"$packageName.app\", \"/tmp/$packageName.app\");\n")
                            updater_writer.write("package_extract_file(\"$packageName.sh\", \"/tmp/$packageName.sh\");\n")
                            updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"/tmp/$packageName.sh\");\n")
                            updater_writer.write("run_program(\"/tmp/$packageName.sh\");\n")
                        } else extractItToTemp("$packageName.app", false)
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
            }

            // extract extras
            updater_writer.write("ui_print(\" \");\n")
            contactsFileName?.let {
                updater_writer.write("ui_print(\"Extracting contacts: $it\");\n")
                extractItToTemp(it)
            }
            smsFileName?.let {
                updater_writer.write("ui_print(\"Extracting sms: $it\");\n")
                extractItToTemp(it)
            }
            callsFileName?.let {
                updater_writer.write("ui_print(\"Extracting call logs: $it\");\n")
                extractItToTemp(it)
            }
            settingsFileName?.let {
                updater_writer.write("ui_print(\"Extracting settings: $it\");\n")
                extractItToTemp(it)
            }
            wifiFileName?.let {
                updater_writer.write("ui_print(\"Extracting keyboard data: $it\");\n")
                extractItToTemp(it)
            }

            // move everything
            updater_writer.write("run_program(\"/tmp/mover.sh\", \"$DATA_TEMP\", \"$MIGRATE_CACHE_DEFAULT\", \"$DIR_MANUAL_CONFIGS\");\n")
            updater_writer.write("ui_print(\" \");\n")

            // unpack helper
            updater_writer.write("ui_print(\"Unpacking helper\");\n")
            updater_writer.write("package_extract_dir(\"system\", \"/tmp/system\");\n")
            updater_writer.write("run_program(\"/tmp/helper_unpacking_script.sh\", \"/tmp/system/\", \"$THIS_VERSION\", \"$DIR_MANUAL_CONFIGS\");\n")

            updater_writer.write("set_progress(1.0000);\n")

            // verification
            updater_writer.write("run_program(\"/tmp/verify.sh\", \"$FILE_FILE_LIST\", \"$timeStamp\", \"$MIGRATE_CACHE_DEFAULT\", \"$DATA_TEMP\", \"$DIR_MANUAL_CONFIGS\");\n")

            // un-mount partitions
            updater_writer.write("ui_print(\" \");\n")
            updater_writer.write("ui_print(\"Unmounting partition...\");\n")
            updater_writer.write("run_program(\"/tmp/mount_script.sh\", \"u\");\n")

            updater_writer.write("ui_print(\" \");\n")
            updater_writer.write("ui_print(\"Finished!\");\n")
            updater_writer.write("ui_print(\" \");\n")
            updater_writer.write("ui_print(\"Files have been restored to Migrate cache.\");\n")
            updater_writer.write("ui_print(\"---------------------------------\");\n")
            updater_writer.write("ui_print(\"PLEASE ROOT YOUR ROM WITH MAGISK.\");\n")
            updater_writer.write("ui_print(\"YOU WILL BE PROMPTED TO CONTINUE RESTORE AFTER STARTUP!!\");\n")
            updater_writer.write("ui_print(\"---------------------------------\");\n")
            updater_writer.write("ui_print(\"For any issues during flashing:\");\n")
            updater_writer.write("ui_print(\"(DO NOT REBOOT FROM TWRP) Go to TWRP main menu -> Advanced -> Copy logs. \'recovery.log\' will be copied under Internal storage.\");\n")
            updater_writer.write("ui_print(\"Reach out to us on Telegram with the above file:\");\n")
            updater_writer.write("ui_print(\"https://t.me/migrateApp\");\n")
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
        contents += "data_required_size ${zipBatch?.batchDataSize ?: 0}\n"
        contents += "system_required_size ${zipBatch?.batchSystemSize ?: 0}\n"
        contents += "migrate_version " + engineContext.getString(R.string.current_version_name) + "\n"

        try {
            val writer = BufferedWriter(FileWriter(packageData))
            writer.write(contents)
            writer.close()
            miscFiles.add(packageData)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun doInBackground(vararg params: Any?): Any {

        try {

            val title = getTitle(R.string.making_updater_script)

            resetBroadcast(true, title)

            makePackageData()
            extractToBackup("busybox", actualDestination)
            extractToBackup("update-binary", "$actualDestination/META-INF/com/google/android/", "$actualDestination/META-INF")
            extractToBackup("mount_script.sh", actualDestination)
            extractToBackup("prep.sh", actualDestination)
            extractToBackup("mover.sh", actualDestination)
            extractToBackup("helper_unpacking_script.sh", actualDestination)
            extractToBackup("verify.sh", actualDestination)
            extractToBackup("MigrateHelper.apk", "$actualDestination/system/app/MigrateHelper/", "$actualDestination/system")

            makeUpdaterScript()
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_UPDATER_TRY_CATCH${bd.errorTag}: ${e.message}")
        }

        return 0
    }

    override fun postExecuteFunction() {
        onBackupComplete.onBackupComplete(jobcode, errors.size == 0, errors)
    }

}