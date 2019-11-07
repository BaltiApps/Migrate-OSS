package balti.migrate.backupEngines.engines

import balti.migrate.AppInstance.Companion.MAX_WORKING_SIZE
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_FL_ITEM_UNAVAILABLE
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_FL_UNAVAILABLE
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_ITEM_UNAVAILABLE
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_TOO_BIG
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_VERIFICATION_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_FILE_LIST
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_FILELIST_IN_ZIP_VERIFICATION
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipVerificationEngine(private val jobcode: Int,
                            private val bd: BackupIntentData,
                            private val zipList: ArrayList<String>,
                            private val zipFile: File) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION) {

    private val verificationErrors by lazy { ArrayList<String>(0) }

    override fun doInBackground(vararg params: Any?): Any {

        try {
            val contents = ArrayList<String>(0)
            val appDirectories = ArrayList<String>(0)

            val checkFileListContents = sharedPreferences.getBoolean(PREF_FILELIST_IN_ZIP_VERIFICATION, true)

            val title = getTitle(R.string.verifying_zip)

            resetBroadcast(true, title)

            val zip = ZipFile(zipFile)
            val e = zip.entries()

            val fileSize = zipFile.length()/1024
            if (fileSize > MAX_WORKING_SIZE){
                verificationErrors.add("$ERR_ZIP_TOO_BIG${bd.errorTag}: ${commonTools.getHumanReadableStorageSpace(fileSize)}")
                return 0
            }

            var subTask = engineContext.getString(R.string.listing_zip_file)
            broadcastProgress(subTask, subTask, false)

            var fileListEntry : ZipEntry? = null

            while (e.hasMoreElements()) {

                if (BackupServiceKotlin.cancelAll) break

                val entry = e.nextElement()
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

                commonTools.tryIt {
                    if (entry.name == FILE_FILE_LIST) {
                        fileListEntry = entry as ZipEntry
                    }
                }
            }

            resetBroadcast(true, title)

            subTask = "${engineContext.getString(R.string.comparing_zip_contents)}(${zipList.size}/${contents.size})"
            broadcastProgress(subTask, subTask, false)

            for (i in 0 until zipList.size){

                val zipItem = zipList[i]
                if (BackupServiceKotlin.cancelAll) break

                if (!contents.contains(zipItem)) {
                    verificationErrors.add("$ERR_ZIP_ITEM_UNAVAILABLE${bd.errorTag}: $zipItem")
                }
            }

            if (checkFileListContents) {

                if (fileListEntry == null)
                    verificationErrors.add("$ERR_ZIP_FL_UNAVAILABLE${bd.errorTag}")
                else {
                    BufferedReader(InputStreamReader(zip.getInputStream(fileListEntry))).readLines().forEach {
                        (if (it.endsWith(".app_sys")) "${it.substring(0, it.lastIndexOf('.'))}.app" else it).run {
                            if (this.trim() != "") {
                                if (this.endsWith(".app")) {
                                    if (!appDirectories.contains(this))
                                        verificationErrors.add("$ERR_ZIP_FL_ITEM_UNAVAILABLE${bd.errorTag}: $this")
                                }
                                else {
                                    if (!contents.contains(this))
                                        verificationErrors.add("$ERR_ZIP_FL_ITEM_UNAVAILABLE${bd.errorTag}: $this")
                                }
                            }
                        }
                    }
                }
            }

        }
        catch (e: Exception){
            e.printStackTrace()
            verificationErrors.add("$ERR_ZIP_VERIFICATION_TRY_CATCH${bd.errorTag}: ${e.message}")
        }

        return 0
    }

    override fun postExecuteFunction() {
        onBackupComplete.onBackupComplete(jobcode, verificationErrors.size == 0, verificationErrors)
    }

}