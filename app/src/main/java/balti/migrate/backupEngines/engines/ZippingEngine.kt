package balti.migrate.backupEngines.engines

import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.backupEngines.containers.ZipAppBatch
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_ENGINE_INIT
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_FILE_LIST
import balti.migrate.utilities.CommonToolKotlin.Companion.WARNING_FILELIST_MAKE
import balti.migrate.utilities.ToolsNoContext
import java.io.*
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZippingEngine(private val jobcode: Int, bd: BackupIntentData,
                    private val miscExtras : ArrayList<File> = ArrayList(0),
                    private val miscFiles : ArrayList<File> = ArrayList(0),
                    private val zipName: String, private val zipBatch: ZipAppBatch? = null) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_ZIP_PROGRESS) {

    private val zippedFiles = ArrayList<String>(0)
    private val errors = ArrayList<String>(0)
    private val warnings = ArrayList<String>(0)

    private fun filterExisting(files: ArrayList<File>): ArrayList<File>{
        var c = 0
        while (c < files.size) {
            files[c].let {
                if (!it.exists()) files.remove(it)
                else c++
            }
        }
        return files
    }

    override fun doInBackground(vararg params: Any?): Any {

        try {

            val fileList = File(actualDestination, FILE_FILE_LIST)
            val allFiles = ArrayList<File>(0)
            val fileListEntries = ArrayList<File>(0)

            try {

                val title = getTitle(R.string.making_file_list)
                resetBroadcast(true, title)

                allFiles.addAll(filterExisting(miscFiles))

                zipBatch?.let {
                    it.zipPackets.forEach { p ->
                        fileListEntries.addAll(filterExisting(p.files))
                    }
                }

                fileListEntries.addAll(filterExisting(miscExtras))

                allFiles.addAll(fileListEntries)

                BufferedWriter(FileWriter(fileList)).run {
                    fileListEntries.forEach {
                        write("${it.name}\n")
                    }
                    close()
                }

                ToolsNoContext.copyFile(fileList, engineContext.cacheDir.absolutePath)

            } catch (e: Exception) {
                e.printStackTrace()
                warnings.add("$WARNING_FILELIST_MAKE: ${e.message}")
            }

            try {

                val title = getTitle(R.string.zipping_all_files)
                resetBroadcast(false, title)

                val directory = File(actualDestination)
                val zipFile = File(directory, "$zipName.zip")

                if (zipFile.exists()) zipFile.delete()
                if (fileList.exists()) allFiles.add(fileList)

                val zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))

                for (i in allFiles.indices) {

                    val file = allFiles[i]

                    if (BackupServiceKotlin.cancelAll) break

                    val relativeFilePath = file.absolutePath.substring(directory.absolutePath.length + 1).let {
                        if (it.endsWith("/") && file.isFile) it.substring(0, it.length - 1)
                        else if (file.isDirectory && !it.endsWith("/")) "$it/"
                        else it
                    }

                    val zipEntry: ZipEntry

                    if (file.isDirectory) {
                        zipEntry = ZipEntry(file.absolutePath)

                        zipOutputStream.putNextEntry(zipEntry)
                        zipOutputStream.closeEntry()
                    } else {
                        zipEntry = ZipEntry(file.absolutePath)

                        val bufferedInputStream = BufferedInputStream(FileInputStream(file))
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

                        val fileInputStream = FileInputStream(file)
                        buffer = ByteArray(4096)

                        while (true) {
                            read = fileInputStream.read(buffer)
                            if (read > 0)
                                zipOutputStream.write(buffer, 0, read)
                            else break
                        }

                        zipOutputStream.closeEntry()
                        fileInputStream.close()
                        file.delete()

                        broadcastProgress("", "zipped: ${file.name}", true,
                                commonTools.getPercentage((i + 1), allFiles.size))
                    }

                    zippedFiles.add(relativeFilePath)
                }

                zipOutputStream.close()

            } catch (e: Exception) {
                e.printStackTrace()
                errors.add("$ERR_ZIP_TRY_CATCH: ${e.message}")
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_ZIP_ENGINE_INIT: ${e.message}")
        }

        return 0
    }

    override fun postExecuteFunction() {
        onEngineTaskComplete.onComplete(jobcode, errors, warnings, zippedFiles)
    }
}