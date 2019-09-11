package balti.migrate.backupEngines.engines

import balti.migrate.R
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.utilities.CommonToolKotlin.Companion.ERR_ZIP_TRY_CATCH
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_ZIP_LOG
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZippingEngine(private val jobcode: Int,
                    private val bd: BackupIntentData) : ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_ZIP_PROGRESS) {

    private val zippedFiles = ArrayList<String>(0)
    private val zipErrors = ArrayList<String>(0)

    private fun getAllFiles(directory: File, allFiles: ArrayList<File>): ArrayList<File>{
        if (!directory.isDirectory) return arrayListOf(directory)
        else {
            for (f in directory.listFiles()){
                allFiles.add(f)
                if (f.isDirectory) getAllFiles(f, allFiles)
            }
        }
        return allFiles
    }

    override fun doInBackground(vararg params: Any?): Any {

        try {

            val title = if (bd.totalParts > 1)
                engineContext.getString(R.string.combining) + " : " + madePartName
            else engineContext.getString(R.string.combining)

            val directory = File(actualDestination)
            val zipFile = File("$actualDestination.zip")

            actualBroadcast.apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ZIP_LOG, "")
                putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
            }
            commonTools.LBM?.sendBroadcast(actualBroadcast)

            if (zipFile.exists()) zipFile.delete()

            val files = getAllFiles(directory, ArrayList(0))

            val zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))

            for (i in 0 until files.size){

                val file = files[i]

                if (isBackupCancelled) break

                val relativeFilePath = file.absolutePath.substring(directory.absolutePath.length + 1).let {
                    if (it.endsWith("/") && file.isFile) it.substring(0, it.length - 1)
                    else if (file.isDirectory && !it.endsWith("/")) "$it/"
                    else it
                }

                val zipEntry: ZipEntry

                if (file.isDirectory){
                    zipEntry = ZipEntry(relativeFilePath)

                    zipOutputStream.putNextEntry(zipEntry)
                    zipOutputStream.closeEntry()
                }
                else {
                    zipEntry = ZipEntry(relativeFilePath)

                    val bufferedInputStream = BufferedInputStream(FileInputStream(file))
                    var buffer = ByteArray(4096)
                    var read: Int

                    val crc32 = CRC32()

                    while (true){
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

                    while (true){
                        read = fileInputStream.read(buffer)
                        if (read > 0)
                            zipOutputStream.write(buffer, 0, read)
                        else break
                    }

                    zipOutputStream.closeEntry()
                    fileInputStream.close()
                    file.delete()

                    actualBroadcast.apply {
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_ZIP_LOG, "zipped: " + file.name)
                        putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage((i+1), files.size).let {
                            if (it == 100) 99
                            else it
                        })
                    }

                    commonTools.LBM?.sendBroadcast(actualBroadcast)
                }

                zippedFiles.add(relativeFilePath)
            }

            zipOutputStream.close()
            commonTools.dirDelete(directory.absolutePath)

        }
        catch (e: Exception){
            e.printStackTrace()
            zipErrors.add("$ERR_ZIP_TRY_CATCH${bd.errorTag}: ${e.message}")
        }

        return 0
    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        if (zipErrors.size == 0)
            onBackupComplete.onBackupComplete(jobcode, true, zippedFiles)
        else onBackupComplete.onBackupComplete(jobcode, false, zipErrors)
    }
}