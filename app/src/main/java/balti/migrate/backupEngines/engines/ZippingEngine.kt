package balti.migrate.backupEngines.engines

import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.containers.ZipAppBatch
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_ZIP_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_PROGRESS
import balti.migrate.utilities.CommonToolsKotlin.Companion.WARNING_FILE_LIST_COPY
import balti.module.baltitoolbox.functions.Misc.getPercentage
import balti.module.baltitoolbox.functions.Misc.tryIt
import java.io.BufferedInputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZippingEngine(private val jobcode: Int,
                    private val bd: BackupIntentData,
                    private val zipBatch: ZipAppBatch) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_ZIP_PROGRESS) {

    private val zippedFiles = ArrayList<String>(0)
    private val zipErrors = ArrayList<String>(0)
    private val warnings = ArrayList<String>(0)
    private lateinit var fileListCopied: FileX

    private fun getAllFiles(directory: FileX, allFiles: ArrayList<FileX> = ArrayList(0)): ArrayList<FileX>{
        if (!directory.isDirectory) return arrayListOf(directory)
        else {
            tryIt {
                for (f in directory.listFiles()!!) {
                    allFiles.add(f)
                    if (f.isDirectory) getAllFiles(f, allFiles)
                }
            }
        }
        return allFiles
    }

    override suspend fun doInBackground(arg: Any?): Any? {

        try {

            val title = getTitle(R.string.zipping_all_files)

            resetBroadcast(false, title)

            val directory = FileX.new(actualDestination)
            val zipFile = FileX.new("$actualDestination.zip")

            // copy the fileList to internal storage for later comparison

            zipBatch.fileList?.let {
                if (it.exists()) {
                    val copyRes = try {
                        it.copyTo(FileX.new(CACHE_DIR, "${it.name}_${zipBatch.partName}", true))
                        ""
                    }
                    catch (e: Exception){
                        e.printStackTrace()
                        e.message.toString()
                    }
                    if (copyRes != "")
                        warnings.add("$WARNING_FILE_LIST_COPY${bd.batchErrorTag}: $copyRes")
                    else fileListCopied = FileX.new(CACHE_DIR, "${it.name}_${zipBatch.partName}", true)
                }
                else warnings.add("$WARNING_FILE_LIST_COPY${bd.batchErrorTag}: ${engineContext.getString(R.string.file_list_not_in_zip_batch)}")
            }

            heavyTask {
                zipFile.createNewFile(overwriteIfExists = true)
                zipFile.refreshFile()

                val files = getAllFiles(directory)
                val zipOutputStream = ZipOutputStream(zipFile.outputStream())

                for (i in 0 until files.size) {

                    val file = files[i]

                    if (BackupServiceKotlin.cancelAll) break

                    val endPath = if (FileXInit.isTraditional) file.canonicalPath.substring(directory.canonicalPath.length + 1)
                    else file.path.substring(directory.path.length + 1)

                    val relativeFilePath = endPath.let {
                        if (it.endsWith("/") && file.isFile) it.substring(0, it.length - 1)
                        else if (file.isDirectory && !it.endsWith("/")) "$it/"
                        else it
                    }

                    val zipEntry: ZipEntry

                    if (file.isDirectory) {
                        zipEntry = ZipEntry(relativeFilePath)

                        zipOutputStream.putNextEntry(zipEntry)
                        zipOutputStream.closeEntry()
                    } else {
                        zipEntry = ZipEntry(relativeFilePath)

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

                        broadcastProgress("", "zipped: ${file.name}", true,
                                getPercentage((i + 1), files.size))
                    }
                    zippedFiles.add(relativeFilePath)
                }

                zipOutputStream.close()
                directory.let { if (CommonToolsKotlin.isDeletable(it)) it.deleteRecursively() }
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            zipErrors.add("$ERR_ZIP_TRY_CATCH${bd.batchErrorTag}: ${e.message}")
        }

        return 0
    }

    override fun postExecuteFunction() {
        onEngineTaskComplete.onComplete(jobcode, zipErrors, warnings,
                arrayOf(zippedFiles, if(this::fileListCopied.isInitialized) fileListCopied else null, actualDestination))
    }
}