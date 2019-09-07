package balti.migrate.backupEngines

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import balti.migrate.R
import balti.migrate.backupEngines.utils.BackupDependencyComponent
import balti.migrate.backupEngines.utils.DaggerBackupDependencyComponent
import balti.migrate.backupEngines.utils.OnBackupComplete
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PART_NUMBER
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TOTAL_PARTS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_ZIP_LOG
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

class ZippingEngine(private val jobcode: Int,
                    private val bd: BackupIntentData) : AsyncTask<Any, Any, Any>() {

    @Inject lateinit var engineContext: Context
    @Inject lateinit var sharedPrefs : SharedPreferences

    private val commonTools by lazy { CommonToolKotlin(engineContext) }
    private val madePartName by lazy { commonTools.getMadePartName(bd) }
    private val actualDestination by lazy { "${bd.destination}/${bd.backupName}" }

    private val onBackupComplete by lazy { engineContext as OnBackupComplete }

    private val backupDependencyComponent: BackupDependencyComponent
            by lazy { DaggerBackupDependencyComponent.create() }

    private val actualBroadcast by lazy {
        Intent(ACTION_BACKUP_PROGRESS).apply {
            putExtra(EXTRA_BACKUP_NAME, bd.backupName)
            putExtra(EXTRA_PROGRESS_TYPE, EXTRA_PROGRESS_TYPE_ZIP_PROGRESS)
            putExtra(EXTRA_TOTAL_PARTS, bd.totalParts)
            putExtra(EXTRA_PART_NUMBER, bd.partNumber)
            putExtra(EXTRA_PROGRESS_PERCENTAGE, 0)
        }
    }

    private val zippedFiles = ArrayList<String>(0)
    private val zipErrors = ArrayList<String>(0)

    private var isBackupCancelled = false

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

    override fun onPreExecute() {
        super.onPreExecute()
        backupDependencyComponent.inject(this)
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

            var c = 0

            for (file in files){
                if (isBackupCancelled) break

                val relativeFilePath = file.absolutePath.substring(directory.absolutePath.length + 1).apply {
                    if (this.endsWith("/")) this.substring(0, this.length - 1)
                }

                val zipEntry: ZipEntry

                if (file.isDirectory){
                    zipEntry = ZipEntry("$relativeFilePath/")

                    zipOutputStream.putNextEntry(zipEntry)
                    zipOutputStream.closeEntry()
                    c++

                    continue
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
                }

                c++

                actualBroadcast.apply {
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_ZIP_LOG, "zipped: " + file.name)
                    putExtra(EXTRA_PROGRESS_PERCENTAGE, commonTools.getPercentage(c, files.size).let {
                        if (it == 100) 99
                        else it
                    })
                }

                commonTools.LBM?.sendBroadcast(actualBroadcast)

                zippedFiles.add(relativeFilePath)
            }

            zipOutputStream.close()
            commonTools.dirDelete(directory.absolutePath)

            if (c < files.size)
                zipErrors.add("ZIP_ERROR${bd.errorTag}: ${engineContext.getString(R.string.incompleteZip)}")

        }
        catch (e: Exception){
            if (!isBackupCancelled) {
                e.printStackTrace()
                zipErrors.add("ZIP_ERROR${bd.errorTag}: ${e.message}")
            }
        }

        return 0
    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        if (zipErrors.size == 0)
            onBackupComplete.onBackupComplete(jobcode, true, zippedFiles)
        else onBackupComplete.onBackupComplete(jobcode, false, zipErrors)
    }

    override fun onCancelled() {
        super.onCancelled()
        isBackupCancelled = true
    }
}