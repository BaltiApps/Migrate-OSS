package balti.migrate.backupEngines.engines

import balti.migrate.AppInstance.Companion.MAX_WORKING_SIZE
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_TOO_BIG
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_VERIFICATION_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_FILELIST_IN_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolKotlin.Companion.WARNING_ZIP_FILELIST_ITEM_UNAVAILABLE
import balti.migrate.utilities.CommonToolKotlin.Companion.WARNING_ZIP_FILELIST_UNAVAILABLE
import balti.migrate.utilities.CommonToolKotlin.Companion.WARNING_ZIP_FILELIST_VERIFICATION
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.zip.ZipFile

class ZipVerificationEngine(private val jobcode: Int,
                            private val bd: BackupIntentData,
                            private val zipList: ArrayList<String>,
                            private val zipFile: File,
                            private val fileListForComparison: File? = null) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION) {

    private val verificationErrors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }

    override fun doInBackground(vararg params: Any?): Any {

        try {
            val contents = ArrayList<String>(0)
            val appDirectories = ArrayList<String>(0)

            val checkFileListContents = sharedPreferences.getBoolean(PREF_FILELIST_IN_ZIP_VERIFICATION, true)

            val title = getTitle(R.string.verifying_zip)

            resetBroadcast(true, title)

            val zip = ZipFile(zipFile)
            val enumeration = zip.entries()

            val fileSize = zipFile.length()/1024
            if (fileSize > MAX_WORKING_SIZE){
                verificationErrors.add("$ERR_ZIP_TOO_BIG: ${commonTools.getHumanReadableStorageSpace(fileSize)} (${fileSize}B) " +
                        "${engineContext.getString(R.string.allowed)}: ${commonTools.getHumanReadableStorageSpace(MAX_WORKING_SIZE)} (${MAX_WORKING_SIZE}B)")
                return 0
            }

            var subTask = engineContext.getString(R.string.listing_zip_file)
            broadcastProgress(subTask, subTask, false)

            while (enumeration.hasMoreElements()) {

                if (BackupServiceKotlin.cancelAll) break

                val entry = enumeration.nextElement()
                contents.add(entry.name)

                if (checkFileListContents && entry.name.contains('/')) {
                    entry.name.let { it ->
                        it.substring(0, it.indexOf('/')).let {
                            if (it.endsWith(".app") && !appDirectories.contains(it)) {
                                appDirectories.add(it)
                            }
                        }
                    }
                }
            }

            resetBroadcast(true, title)

            for (i in 0 until zipList.size){

                val zipItem = zipList[i]
                if (BackupServiceKotlin.cancelAll) break

                if (!contents.contains(zipItem)) {
                    warnings.add("$WARNING_ZIP_FILELIST_VERIFICATION${bd.batchErrorTag}: $zipItem")
                }
            }

            subTask = "${engineContext.getString(R.string.compared_zip_contents)}(${zipList.size}/${contents.size})"
            broadcastProgress(subTask, subTask, false)

            if (checkFileListContents) {

                if (fileListForComparison == null || !fileListForComparison.exists())
                    warnings.add("$WARNING_ZIP_FILELIST_UNAVAILABLE${bd.batchErrorTag}")
                else {

                    var filesCompared = 0
                    var filesPresent = 0

                    BufferedReader(FileReader(fileListForComparison)).readLines().forEach {
                        (if (it.endsWith(".app_sys")) "${it.substring(0, it.lastIndexOf('.'))}.app" else it).run {

                            if (this.trim() != "" && !this.endsWith(".db-wal")) {
                                // ignore wal and shm files

                                filesCompared++

                                if (this.endsWith(".app")) {
                                    if (!appDirectories.contains(this))
                                        warnings.add("$WARNING_ZIP_FILELIST_ITEM_UNAVAILABLE${bd.batchErrorTag}: $this")
                                    else filesPresent++
                                } else {
                                    if (!contents.contains(this))
                                        warnings.add("$WARNING_ZIP_FILELIST_ITEM_UNAVAILABLE${bd.batchErrorTag}: $this")
                                    else filesPresent++
                                }
                            }
                        }
                    }

                    subTask = "${engineContext.getString(R.string.compared_fileList_contents)}($filesPresent/$filesCompared)"
                    broadcastProgress(subTask, subTask, false)
                }
            }

        }
        catch (e: Exception){
            e.printStackTrace()
            verificationErrors.add("$ERR_ZIP_VERIFICATION_TRY_CATCH${bd.batchErrorTag}: ${e.message}")
        }

        return 0
    }

    override fun postExecuteFunction() {
        fileListForComparison?.run { delete() }
        onEngineTaskComplete.onComplete(jobcode, verificationErrors, warnings)
    }

}