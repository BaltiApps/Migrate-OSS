package balti.migrate.backupEngines.engines

import balti.filex.FileX
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.R
import balti.migrate.backupEngines.ParentBackupClass_new
import balti.migrate.backupEngines.containers.ZipBatch
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.ProgressType.PROGRESS_TYPE_CLEANING
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_FILE_MISSED_TO_ZIP
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_MISSED_FILES_CREATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_MISSED_FILES_CREATION_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_ZIP_MISSED_FILES
import balti.migrate.utilities.CommonToolsKotlin.Companion.WARNING_CLEANING_UP
import balti.migrate.utilities.CommonToolsKotlin.Companion.WARNING_CLEANING_UP_TRY_CATCH
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc.iterateBufferedReader
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class CleaningEngine(private val allBatches: ArrayList<ZipBatch>,
                     private val isBackupSuccess: Boolean,
                     private val busyBoxBinaryPath: String,
): ParentBackupClass_new(PROGRESS_TYPE_CLEANING, false) {

    override val className: String = "CleaningEngine"

    private val missedFiles by lazy { FileX.new(CACHE_DIR, FILE_ZIP_MISSED_FILES, true) }
    private var anyFileMissed = false

    /**
     * Create a file - [missedFiles], with list of all the files found which were not zipped.
     * This file should be empty, as [ZippingEngine] should have zipped every file
     * leaving only blank directories.
     */
    private fun createMissedFilesList(){

        val title = getTitle(R.string.cleaning_up)
        resetBroadcast(true, title)
        broadcastProgress(getStringFromRes(R.string.checking_for_missed_files), "", false)

        try {
            val missedFiles = FileX.new(CACHE_DIR, FILE_ZIP_MISSED_FILES, true)

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

                suInputStream.write("cat /dev/null > ${missedFiles.canonicalPath}\n")

                suInputStream.write("cd ${rootLocation.canonicalPath}\n")

                allBatches.forEach { zipBatch ->
                    if (zipBatch.zipName.isNotBlank())
                        suInputStream.write("walk_dir \"${zipBatch.zipName}\" >> ${missedFiles.canonicalPath}\n")
                    else
                        suInputStream.write("walk_dir . >> ${missedFiles.canonicalPath}\n")
                }
                suInputStream.write("exit\n")
                suInputStream.flush()

                iterateBufferedReader(errorStream, { errorLine ->
                    errors.add("$ERR_MISSED_FILES_CREATION: $errorLine")
                    return@iterateBufferedReader false
                })
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_MISSED_FILES_CREATION_CATCH: ${e.message}")
        }
    }

    /**
     * Read [missedFiles]. Any content in it is a file not present in zip.
     */
    private fun readMissedFiles(){
        /** Empty directories are also recorded in the file, but they end with a slash-star */
        missedFiles.readLines().map { it.trim() }.filter { !it.endsWith("/*") }.forEach {
            if (it.isNotBlank()) {
                writeLog("Missed file: $it")
                errors.add("$ERR_FILE_MISSED_TO_ZIP: $it")
                anyFileMissed = true
            }
        }
    }

    private fun deleteFilesAndSubDirectories(){

        val title = getTitle(R.string.cleaning_up)
        resetBroadcast(true, title)

        try {
            val suProcess = Runtime.getRuntime().exec("su")
            suProcess?.let { p ->
                val suInputStream = BufferedWriter(OutputStreamWriter(p.outputStream))
                val errorStream = BufferedReader(InputStreamReader(p.errorStream))

                suInputStream.run {

                    /**
                     * CASE 1: Backup is successful and no file is missed.
                     * Retain the zips.
                     * All directories will be empty, hence delete them by `rm -rf`.
                     */
                    if (isBackupSuccess && !anyFileMissed) {
                        /**
                         * For single-zip backup:
                         * [allBatches] will have 1 batch. That batch will have blank `zipName`.
                         * All backup files will be under [fileXDestination].
                         * The zip will not be under [fileXDestination], but on the same level - `FileX.new("${fileXDestination}.zip")`
                         *
                         * For multi-zip backup:
                         * All batches will have a non-blank `zipName`.
                         * All backup files will be under their respective directories - [fileXDestination]/`zipName`.
                         * The zips will be just under [fileXDestination] - `FileX.new("${fileXDestination}/${zipName}.zip")`
                         *
                         * Now, we need to remove the directories, but not the zip.
                         * `batchRoot = FileX.new(fileXDestination, zipName)`
                         * Covers both the cases.
                         * For single-zip, `zipName` = "". `batchRoot = `FileX.new(fileXDestination)`
                         * For multi-zip, `batchRoot = `FileX.new(fileXDestination/zipName)`
                         * Deleting the `batchRoot` (for each zip batch) will delete the directories, without touching the zips.
                         */
                        writeLog("CASE 1: isBackupSuccess && !anyFileMissed")
                        broadcastProgress(getStringFromRes(R.string.deleting_empty_directories), "", false)
                        allBatches.forEach {
                            val batchRoot = FileX.new(fileXDestination, it.zipName)
                            writeLog("Command: " + "rm -rf ${batchRoot.canonicalPath}\n")
                            write("rm -rf ${batchRoot.canonicalPath}\n")
                        }
                    }

                    /**
                     * CASE 2: Backup was success, but some files were missed.
                     * Delete the empty directories only.
                     * Leave the zip files and the directories/sub-directories having any file.
                     *
                     * Using the command: `find [rootLocation] -depth -type d -exec rmdir {} \; 2>/dev/null`
                     * Found on (2nd answer): https://unix.stackexchange.com/questions/8430/how-to-remove-all-empty-directories-in-a-subtree
                     * This command will also remove the [rootLocation] if it turns out to be empty somehow.
                     */
                    else if (isBackupSuccess && anyFileMissed) {
                        writeLog("CASE 2: isBackupSuccess && anyFileMissed")
                        writeLog("Command: " + "$busyBoxBinaryPath find ${rootLocation.canonicalPath} -depth -type d -exec rmdir {} \\; 2>/dev/null")
                        broadcastProgress(getStringFromRes(R.string.deleting_empty_directories), "", false)
                        write("$busyBoxBinaryPath find ${rootLocation.canonicalPath} -depth -type d -exec rmdir {} \\; 2>/dev/null\n")
                    }

                    /**
                     * CASE 3: Backup failed. Delete everything from [rootLocation].
                     */
                    else if (!isBackupSuccess) {
                        /**
                         * For single-zip backup:
                         * Removing [fileXDestination] -> removes all files and sub-directories created.
                         * If zip is created, need to delete `FileX.new("${fileXDestination}.zip")`.
                         *
                         * For multi-zip backup:
                         * Removing [fileXDestination] -> Removes all zip batches along with any partially formed backup zip files.
                         */
                        writeLog("CASE 3: !isBackupSuccess")
                        broadcastProgress(getStringFromRes(R.string.deleting_backup), "", false)
                        write("rm -rf ${rootLocation.canonicalPath}\n")
                        FileX.new("${fileXDestination}.zip").let {
                            if (it.exists()) write("rm -rf ${it.canonicalPath} 2> /dev/null\n")
                        }
                    }

                }

                suInputStream.write("exit\n")
                suInputStream.flush()

                iterateBufferedReader(errorStream, { errorLine ->
                    warnings.add("$WARNING_CLEANING_UP: $errorLine")
                    return@iterateBufferedReader false
                })
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            warnings.add("$WARNING_CLEANING_UP_TRY_CATCH: ${e.message}")
        }
    }

    override suspend fun backgroundProcessing(): Any? {

        if (isBackupSuccess){
            createMissedFilesList()
            readMissedFiles()
        }

        deleteFilesAndSubDirectories()

        return null
    }
}