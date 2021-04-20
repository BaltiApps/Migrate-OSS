package balti.updater.downloader

import androidx.lifecycle.MutableLiveData
import balti.updater.Constants.Companion.FILE_UPDATE_APK
import balti.updater.R
import balti.updater.Tools
import balti.updater.Updater.Companion.context
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class DownloadJob {

    private val updateFile by lazy { File(context.filesDir, FILE_UPDATE_APK) }

    val progress = MutableLiveData<Int>()
    val lengthOfFile = MutableLiveData<Int>()

    private suspend fun updateProgress(rawProgress: Int){
        withContext(Main) {
            lengthOfFile.value.let {
                progress.value = if (it != null)((rawProgress*100.0)/it).toInt()
                else rawProgress
            }
        }
    }

    private suspend fun setSize(size: Int){
        withContext(Main){
            lengthOfFile.value = size
        }
    }

    private suspend fun preExecute() {
        withContext(Main) {
            progress.value = 0
            lengthOfFile.value = 1
            updateFile.delete()
        }
    }

    private suspend fun doInBackground(url: URL): String {
        return try {
            withContext(IO) {
                val connection = url.openConnection()
                connection.connect()

                val size = connection.contentLength
                setSize(size)

                val input = BufferedInputStream(url.openStream(), 8192)
                val output = FileOutputStream(updateFile)

                val buffer = ByteArray(1024)
                var total = 0

                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    else {
                        output.write(buffer, 0, count)
                        total += count
                        updateProgress(total)
                    }
                }

                output.flush()
                output.close()
                input.close()
                ""
            }
        }
        catch(e: Exception) {
            e.message ?: context.getString(R.string.download_error_null)
        }
    }

    suspend fun execute(url: String): String{
        return withContext(IO) {
            preExecute()
            Tools().validateError(doInBackground(URL(url)))
        }
    }

}