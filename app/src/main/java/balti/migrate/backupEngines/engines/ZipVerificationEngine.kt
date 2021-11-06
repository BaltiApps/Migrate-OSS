package balti.migrate.backupEngines.engines

import balti.filex.FileX
import balti.migrate.AppInstance.Companion.MAX_WORKING_SIZE
import balti.migrate.R
import balti.migrate.backupEngines.ParentBackupClass_new
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.ProgressType.PROGRESS_TYPE_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_ZIP_FILELIST_ITEM_UNAVAILABLE
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_ZIP_FILELIST_UNAVAILABLE
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_ZIP_NULL_IN_VERIFICATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_ZIP_SIZE_ZERO
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_ZIP_TOO_BIG
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_ZIP_VERIFICATION_TRY_CATCH
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc.getHumanReadableStorageSpace
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipVerificationEngine(private val zipFile: FileX?,
                            override val partTag: String,
                            private val flasherOnly: Boolean = false,
                            private val fileListForComparison: FileX? = null) : ParentBackupClass_new(PROGRESS_TYPE_ZIP_VERIFICATION) {

    override val className: String = "ZipVerificationEngine"

    private val zipContents = ArrayList<String>(0)
    private val appDirectories = ArrayList<String>(0)

    override suspend fun backgroundProcessing(): Any? {

        if (zipFile == null) {
            errors.add("${ERR_ZIP_NULL_IN_VERIFICATION}: Part - $partTag")
            return null
        }

        try {

            val title = getTitle(R.string.verifying_zip)

            resetBroadcast(true, title)

            val fileSize = if (zipFile.exists()) zipFile.length() else 0

            if (fileSize == 0L) {
                errors.add("$ERR_ZIP_SIZE_ZERO${partTag}: Exists: ${zipFile.exists()}, Size: $fileSize")
                return null
            }

            /**
             * A string containing zip file's size and,
             * if [flasherOnly] = `false` then maximum allowed size.
             */
            val fileSizeString = "${getStringFromRes(R.string.zip_size)}: ${getHumanReadableStorageSpace(fileSize)} (${fileSize} B) " +
                    if (flasherOnly) "" else "${getStringFromRes(R.string.allowed)}: ${getHumanReadableStorageSpace(MAX_WORKING_SIZE)} (${MAX_WORKING_SIZE} B)"

            if (!flasherOnly && fileSize > MAX_WORKING_SIZE){
                errors.add("$ERR_ZIP_TOO_BIG${partTag}: $fileSizeString")
                return null
            }

            var subTask = getStringFromRes(R.string.listing_zip_file)
            broadcastProgress(subTask, "${subTask}\n${fileSizeString}", false)

            // old code
            /*while (enumeration.hasMoreElements()) {

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
            }*/

            /**
             * Read all files/directories just inside the zip (root of the zip).
             * This does not check for files/subdirectories inside directories just under the root of the zip.
             */
            val zipInputStream = ZipInputStream(zipFile.inputStream())
            var zEntryCheck: ZipEntry? = zipInputStream.nextEntry
            while (zEntryCheck != null) {

                if (cancelBackup) break

                val entry = zEntryCheck
                val entryName = entry.name

                if (entryName.contains('/')) {
                    val nameWithNoEndingSlash = entryName.dropLast(1)
                    nameWithNoEndingSlash.run {
                        /**
                         * Check if it is an app directory and add it to [appDirectories].
                         */
                        if (endsWith(".app") && !appDirectories.contains(this)) {
                            appDirectories.add(this)
                        }
                    }
                    zipContents.add(nameWithNoEndingSlash)
                }
                else {
                    zipContents.add(entryName)
                }

                zEntryCheck = zipInputStream.nextEntry
            }

            /*for (i in 0 until zipList.size){

                val zipItem = zipList[i]
                if (BackupServiceKotlin.cancelAll) break

                if (!contents.contains(zipItem)) {
                    warnings.add("$WARNING_ZIP_FILELIST_VERIFICATION${bd.batchErrorTag}: $zipItem")
                }
            }*/

            subTask = getStringFromRes(R.string.compared_zip_contents)
            broadcastProgress(subTask, subTask, false)

            if (fileListForComparison == null || !fileListForComparison.exists())
                errors.add("$ERR_ZIP_FILELIST_UNAVAILABLE${partTag}")
            else {

                /**
                 * Count for all the file names present in [fileListForComparison].
                 * Some files like .db-wal and .db-shm are, however, not counted.
                 */
                var filesCompared = 0

                /**
                 * Count for all the entries of fileList.txt which were actually found
                 * to be present inside the zip.
                 * i.e. the entry was present in [zipContents] or [appDirectories].
                 */
                var filesPresent = 0

                fileListForComparison.forEachLine {
                    (if (it.endsWith(".app_sys")) "${it.substring(0, it.lastIndexOf('.'))}.app" else it).trim().run {

                        if (!this.endsWith(".db-wal") && !this.endsWith(".db-shm")) {
                            // ignore wal and shm files

                            filesCompared++

                            if (this.endsWith(".app")) {
                                if (!appDirectories.contains(this))
                                    errors.add("$ERR_ZIP_FILELIST_ITEM_UNAVAILABLE${partTag}: $this")
                                else filesPresent++
                            } else {
                                if (!zipContents.contains(this))
                                    errors.add("$ERR_ZIP_FILELIST_ITEM_UNAVAILABLE${partTag}: $this")
                                else filesPresent++
                            }
                        }
                    }
                }

                subTask = "${getStringFromRes(R.string.compared_fileList_contents)}($filesPresent/$filesCompared)"
                broadcastProgress(subTask, subTask, false)
            }

        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_ZIP_VERIFICATION_TRY_CATCH${partTag}: ${e.message}")
        }

        return null
    }

}