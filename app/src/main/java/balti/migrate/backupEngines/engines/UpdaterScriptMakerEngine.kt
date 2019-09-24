package balti.migrate.backupEngines.engines

import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.extraBackupsActivity.apps.containers.AppBatch
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_UPDATER_EXTRACT
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_UPDATER_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_UPDATER_SCRIPT
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_FILE_LIST
import balti.migrate.utilities.CommonToolKotlin.Companion.TEMP_DIR_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.THIS_VERSION
import java.io.*
import java.util.*
import kotlin.collections.ArrayList

// extract helper, busybox, update-binary, mount_script.sh, prep.sh, helper_unpacking_script.sh, verify.sh

class UpdaterScriptMakerEngine(private val jobcode: Int, private val bd: BackupIntentData,
                               private val appBatch: AppBatch,
                               private val timeStamp: String,
                               private val contactsFileName: String?,
                               private val smsFileName: String?,
                               private val callsFileName: String?,
                               private val settingsFileName: String?,
                               private val wifiFileName: String?) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_UPDATER_SCRIPT) {

    private val pm by lazy { engineContext.packageManager }

    private val errors by lazy { ArrayList<String>(0) }

    private fun moveFile(source: File, destination: File): String {
        return try {
            val bufferedInputStream = BufferedInputStream(FileInputStream(source))
            val fileOutputStream = FileOutputStream(destination)

            val buffer = ByteArray(2048)
            var read: Int

            while (true) {
                read = bufferedInputStream.read(buffer)
                if (read > 0) fileOutputStream.write(buffer, 0, read)
                else break
            }
            fileOutputStream.close()
            source.delete()
            ""
        } catch (e: Exception) {
            e.printStackTrace()
            e.message.toString()
        }

    }

    private fun extractToBackup(fileName: String, targetPath: String){
        val assetFile = File(commonTools.unpackAssetToInternal(fileName, fileName, false))
        val targetFile = File(targetPath, fileName)
        var err = ""

        File(targetPath).mkdirs()

        if (assetFile.exists())
            err = moveFile(assetFile, targetFile)
        else errors.add("$ERR_UPDATER_EXTRACT${bd.errorTag}: $fileName could not be unpacked")

        if (!targetFile.exists())
            errors.add("$ERR_UPDATER_EXTRACT${bd.errorTag}: $fileName could not be moved: $err")
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

            if (madePartName != "") {
                updater_writer.write("ui_print(\"*** $madePartName ***\");\n")
                updater_writer.write("ui_print(\"---------------------------------\");\n")
            }

            // extract scripts and other files
            fun extractFile(fileName: String) {
                updater_writer.write("package_extract_file(\"$fileName\", \"/tmp/$fileName\");\n")
            }
            extractFile("busybox")
            extractFile("mount_script.sh")
            extractFile("prep.sh")
            extractFile("package-data.txt")
            extractFile("helper_unpacking_script.sh")
            extractFile("verify.sh")
            extractFile(FILE_FILE_LIST)

            // set permission to scripts
            fun set777Permission(fileName: String) {
                updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"/tmp/$fileName\");\n")
            }
            set777Permission("busybox")
            set777Permission("mount_script.sh")
            set777Permission("prep.sh")
            set777Permission("package-data.txt")
            set777Permission("helper_unpacking_script.sh")
            set777Permission("verify.sh")
            set777Permission(FILE_FILE_LIST)

            // mount partitions
            updater_writer.write("ui_print(\" \");\n")
            updater_writer.write("ui_print(\"Mounting partition...\");\n")
            updater_writer.write("run_program(\"/tmp/mount_script.sh\", \"m\");\n")

            // exit if mount failed
            updater_writer.write("ifelse(is_mounted(\"/data\"), ui_print(\"Mounted data!\"), abort(\"Mount failed data! Exiting...\"));\n")
            updater_writer.write("ifelse((is_mounted(\"/system\") || is_mounted(\"/system_root\")), ui_print(\"Mounted system!\"), abort(\"Mount failed system! Exiting...\"));\n")

            // run prep.sh
            updater_writer.write("run_program(\"/tmp/prep.sh\", \"$TEMP_DIR_NAME\", \"$timeStamp\");\n")

            // data will be unmounted if prep.sh aborted unsuccessfully
            updater_writer.write("ifelse(is_mounted(\"/data\"), ui_print(\"Parameters checked!\") && sleep(2s), abort(\"Exiting...\"));\n")
            updater_writer.write("ui_print(\" \");\n")

            // ready to restore to migrate cache
            updater_writer.write("ui_print(\"Restoring to Migrate cache...\");\n")
            updater_writer.write("ui_print(\" \");\n")

            // extract app files
            val size = appBatch.appPackets.size
            for (c in 0 until appBatch.appPackets.size) {

                if (BackupServiceKotlin.cancelAll) {
                    updater_writer.close()
                    break
                }

                val packet = appBatch.appPackets[c]
                val packageName = packet.PACKAGE_INFO.packageName

                if (packet.APP) {
                    val appName = pm.getApplicationLabel(packet.PACKAGE_INFO.applicationInfo)
                    var apkPath = packet.PACKAGE_INFO.applicationInfo.sourceDir
                    apkPath = apkPath.substring(0, apkPath.lastIndexOf('/'))

                    updater_writer.write("ui_print(\"$appName (${c + 1}/$size)\");\n")

                    if (apkPath.startsWith("/system")) {
                        updater_writer.write("package_extract_dir(\"$packageName.app\", \"/tmp/$packageName.app\");\n")
                        updater_writer.write("package_extract_file(\"$packageName.sh\", \"/tmp/$packageName.sh\");\n")
                        updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"/tmp/$packageName.sh\");\n")
                        updater_writer.write("run_program(\"/tmp/$packageName.sh\");\n")
                    } else
                        updater_writer.write("package_extract_dir(\"$packageName.app\", \"$TEMP_DIR_NAME/$packageName.app\");\n")
                }

                if (packet.DATA)
                    updater_writer.write("package_extract_dir(\"$packageName.tar.gz\", \"/data/data/$packageName.tar.gz\");\n")

                if (packet.PERMISSION)
                    updater_writer.write("package_extract_file(\"$packageName.perm\", \"$TEMP_DIR_NAME/$packageName.perm\");\n")

                updater_writer.write("package_extract_file(\"$packageName.icon\", \"$TEMP_DIR_NAME/$packageName.icon\");\n")
                updater_writer.write("package_extract_file(\"$packageName.json\", \"$TEMP_DIR_NAME/$packageName.json\");\n")

                var pString = String.format(Locale.ENGLISH, "%.4f", (c + 1) * 1.0 / size)
                pString = pString.replace(",", ".")
                updater_writer.write("set_progress($pString);\n")
            }


            // extract extras
            updater_writer.write("ui_print(\" \");\n")
            contactsFileName?.let {
                updater_writer.write("ui_print(\"Extracting contacts: $it\");\n")
                updater_writer.write("package_extract_file(\"$it\", \"$TEMP_DIR_NAME/$it\");\n")
            }
            smsFileName?.let {
                updater_writer.write("ui_print(\"Extracting sms: $it\");\n")
                updater_writer.write("package_extract_file(\"$it\", \"$TEMP_DIR_NAME/$it\");\n")
            }
            callsFileName?.let {
                updater_writer.write("ui_print(\"Extracting call logs: $it\");\n")
                updater_writer.write("package_extract_file(\"$it\", \"$TEMP_DIR_NAME/$it\");\n")
            }
            settingsFileName?.let {
                updater_writer.write("ui_print(\"Extracting dpi data: $it\");\n")
                updater_writer.write("package_extract_file(\"$it\", \"$TEMP_DIR_NAME/$it\");\n")
            }
            wifiFileName?.let {
                updater_writer.write("ui_print(\"Extracting keyboard data: $it\");\n")
                updater_writer.write("package_extract_file(\"$it\", \"$TEMP_DIR_NAME/$it\");\n")
            }
            updater_writer.write("ui_print(\" \");\n")

            // unpack helper
            updater_writer.write("ui_print(\"Unpacking helper\");\n")
            updater_writer.write("package_extract_dir(\"helper\", \"/tmp/helper\");\n")
            updater_writer.write("run_program(\"/tmp/helper_unpacking_script.sh\", \"/tmp/helper\", \"$THIS_VERSION\");\n")

            updater_writer.write("set_progress(1.0000);\n")

            // verification
            updater_writer.write("run_program(\"/tmp/verify.sh\", \"$FILE_FILE_LIST\", \"$TEMP_DIR_NAME\");\n")

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
            updater_writer.write("ui_print(\"For any issues please contact our telegram group\");\n")
            updater_writer.write("ui_print(\"https://t.me/migrateApp\");\n")
            updater_writer.write("ui_print(\"---------------------------------\");\n")
            updater_writer.write("ui_print(\" \");\n")

            updater_writer.close()
        }

    }

    override fun doInBackground(vararg params: Any?): Any {

        try {

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.making_updater_script) + " : " + madePartName
            else engineContext.getString(R.string.making_updater_script)

            resetBroadcast(true, title)

            extractToBackup("busybox", actualDestination)
            extractToBackup("update-binary", "$actualDestination/META-INF/com/google/android/")
            extractToBackup("mount_script.sh", actualDestination)
            extractToBackup("prep.sh", actualDestination)
            extractToBackup("helper_unpacking_script.sh", actualDestination)
            extractToBackup("verify.sh", actualDestination)
            extractToBackup("MigrateHelper.apk", "$actualDestination/system/app/helper/")

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