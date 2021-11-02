package balti.migrate.backupEngines.engines

import balti.filex.FileX
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.BackupServiceKotlin_new
import balti.migrate.backupEngines.ParentBackupClass_new
import balti.migrate.backupEngines.containers.ZipBatch
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.ProgressType.PROGRESS_TYPE_ZIPPING
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_NO_RAW_LIST
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_PARSING_RAW_LIST
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_ZIP_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_RAW_LIST
import balti.migrate.utilities.CommonToolsKotlin.Companion.WARNING_FILE_LIST_COPY
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc.getPercentage
import java.io.BufferedInputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZippingEngine(override val partTag: String,
                    private val zipBatch: ZipBatch) : ParentBackupClass_new(PROGRESS_TYPE_ZIPPING) {

    override val className: String = "ZippingEngine"

    private lateinit var fileListCopied: FileX
    private lateinit var actualZipFile: FileX

    private val readSubDirectories = ArrayList<String>(0)
    private val readFiles = ArrayList<String>(0)

    private val fileListFile by lazy { FileX.new("${fileXDestination}/${zipBatch.zipName}", FILE_RAW_LIST) }

    /**
     * Read rawList.txt file to get all the directories and files in the sub-directory.
     * Return `true` if no error.
     */
    private fun getAllFiles(): Boolean {

        val tempSubDirectories = ArrayList<String>(0)
        val tempFiles = ArrayList<String>(0)

        fileListFile.run {
            if (!exists()) {
                errors.add("${ERR_NO_RAW_LIST}: For $partTag, ${zipBatch.zipName}")
                return false
            }
            var c = 0
            forEachLine {

                if (BackupServiceKotlin.cancelAll) return@forEachLine

                ++c
                /**
                 * First 3 lines are useless. Hence skip them.
                 * To see the format of rawList.txt, check [UpdaterScriptMakerEngine.createRawList].
                 * Also each file name starts with ./<path/name>.
                 * Hence ignore the first 2 characters.
                 */
                try {
                    if (c > 3) {
                        var formattedPath = it.substring(2)
                        formattedPath = formattedPath.let { p ->
                            if (p.endsWith('/')) p.dropLast(1)
                            else p
                        }
                        formattedPath.run {
                            if (contains('/')) {
                                tempSubDirectories.add(substring(0, lastIndexOf('/')))
                                tempFiles.add(substring(lastIndexOf('/') + 1))
                            } else tempFiles.add(this)
                        }
                    }
                }
                catch (e: Exception){
                    e.printStackTrace()
                    errors.add("${ERR_PARSING_RAW_LIST}${partTag}: Line: \'${it}\', Error: ${e.message}")
                }
            }

            /**
             * If any error occurs, return false.
             */
            if (errors.isNotEmpty()) return false
        }

        /**
         * Add non-duplicate entries.
         * Example:
         * dir1/dir2/file1
         * dir1/dir2/file2
         * Both will have the same sub-directory: dir1/dir2. But we only need to add them once.
         */
        readSubDirectories.addAll(tempSubDirectories.distinct())
        readFiles.addAll(tempFiles.distinct())

        return true
    }

    override suspend fun backgroundProcessing(): Any? {

        try {

            val title = getTitle(R.string.zipping_all_files)
            var subTask = ""

            resetBroadcast(true, title)

            /**
             * Copy the fileList.txt to internal storage for later comparison.
             * Note that fileList.txt is not same as rawList.txt.
             *
             * Difference between rawList and fileList:
             *
             * - rawList.txt contains all the file names including backup files,
             *   updater-script, various other shell scripts, MigrateHelper.apk etc.
             * - fileList.txt contains only names of app backup files and extras.
             * - rawList.txt is used for creating the zip.
             * - fileList.txt is used in verification to check if the important backup
             *   files are present in the final zip.
             */
            fileListFile.let {

                subTask = getStringFromRes(R.string.copying_file_list)
                broadcastProgress(subTask, subTask, false)

                if (it.exists()) {
                    try {
                        val extFileList = FileX.new(CACHE_DIR, "${it.name}_${partTag}", true)
                        it.copyTo(extFileList)
                        fileListCopied = extFileList
                    }
                    catch (e: Exception){
                        e.printStackTrace()
                        warnings.add("$WARNING_FILE_LIST_COPY${partTag}: ${e.message.toString()}")
                    }
                }
                else errors.add("$WARNING_FILE_LIST_COPY${partTag}: ${getStringFromRes(R.string.file_list_not_in_zip_batch)}")
            }

            /**
             * Start creating the zip.
             */
            heavyTask {

                /**
                 * [fileXDestination] = path + backup name.
                 * For non-traditional FileX, path = ""
                 *
                 * If single zip backup, all files will be generated in a folder under backup name.
                 * But zip needs to be created one level above this backup name.
                 * Hence if [fileXDestination] = "/sdcard/Migrate/Full_Backup_blah"
                 * Zip = "/sdcard/Migrate/Full_Backup_blah.zip"
                 * FOR THIS CASE: The qualifying condition would be [ZipBatch.zipName] == ""
                 *
                 * If multi zip backup, backup files would be under different directories.
                 * "/sdcard/Migrate/Full_Backup_blah/App_Zip_1_of_3"
                 * "/sdcard/Migrate/Full_Backup_blah/App_Zip_2_of_3"
                 * "/sdcard/Migrate/Full_Backup_blah/App_Zip_3_of_3"
                 * Zips need to be created outside these directories but under the "Full_Backup_blah" directory.
                 * Hence zips will be:
                 * "/sdcard/Migrate/Full_Backup_blah/App_Zip_1_of_3.zip"
                 * "/sdcard/Migrate/Full_Backup_blah/App_Zip_2_of_3.zip"
                 * "/sdcard/Migrate/Full_Backup_blah/App_Zip_3_of_3.zip"
                 *
                 * If for some reason [fileXDestination] is blank, use backup name itself.
                 */
                actualZipFile =
                    when {
                        zipBatch.zipName.isNotBlank() -> FileX.new(fileXDestination, "${zipBatch.zipName}.zip")
                        fileXDestination.isNotBlank() -> FileX.new("$fileXDestination.zip")
                        else -> FileX.new("${BackupServiceKotlin_new.backupName}.zip")
                    }

                if (!getAllFiles() || BackupServiceKotlin.cancelAll) return@heavyTask

                actualZipFile.createNewFile(overwriteIfExists = true)
                actualZipFile.exists()

                val zipOutputStream = ZipOutputStream(actualZipFile.outputStream())

                /**
                 * First create subdirectories
                 */

                subTask = getStringFromRes(R.string.adding_directories)
                broadcastProgress(subTask, subTask, false)

                for (dir in readSubDirectories){
                    val dirAdjustedPath = "${dir}/"

                    broadcastProgress(subTask, dirAdjustedPath, false)

                    val zipEntry = ZipEntry(dirAdjustedPath)
                    zipOutputStream.putNextEntry(zipEntry)
                    zipOutputStream.closeEntry()
                }

                /**
                 * Then add the files.
                 * This will take time. Reset broadcast to make it determinate.
                 */

                resetBroadcast(false, title)
                subTask = getStringFromRes(R.string.adding_files_to_zip)
                broadcastProgress(subTask, subTask, false)

                for ((i, filePath) in readFiles.withIndex()){
                    val file = FileX.new(filePath)
                    val zipEntry = ZipEntry(filePath)

                    file.exists()

                    val bufferedInputStream = BufferedInputStream(file.inputStream())
                    var buffer = ByteArray(4096)
                    var read: Int

                    val crc32 = CRC32()

                    while (true) {
                        read = bufferedInputStream.read(buffer)
                        if (read > 0)
                            crc32.update(buffer, 0, read)
                        else break
                    }

                    bufferedInputStream.close()

                    zipEntry.size = file.length()
                    zipEntry.compressedSize = file.length()
                    zipEntry.crc = crc32.value
                    zipEntry.method = ZipEntry.STORED

                    zipOutputStream.putNextEntry(zipEntry)

                    val fileInputStream = file.inputStream()
                    buffer = ByteArray(4096)

                    while (true) {
                        read = fileInputStream!!.read(buffer)
                        if (read > 0)
                            zipOutputStream.write(buffer, 0, read)
                        else break
                    }

                    zipOutputStream.closeEntry()
                    fileInputStream.close()
                    file.delete()

                    broadcastProgress(
                        subTask, "zipped: ${file.name}", true,
                        getPercentage((i + 1), readFiles.size)
                    )

                }

                /**
                 * Finally close the zip output stream and delete the directory.
                 */

                zipOutputStream.close()
                val directory = FileX.new("${fileXDestination}/${zipBatch.zipName}")
                directory.let { if (CommonToolsKotlin.isDeletable(it)) it.deleteRecursively() }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_ZIP_TRY_CATCH${partTag}: ${e.message}")
        }

        return Pair(
            if (this::actualZipFile.isInitialized) actualZipFile else null,
            if (this::fileListCopied.isInitialized) fileListCopied else null
        )
    }
}