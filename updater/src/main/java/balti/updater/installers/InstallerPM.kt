package balti.updater.installers

import android.content.Intent
import android.os.Build
import balti.updater.Tools
import balti.updater.Updater
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import java.io.File

internal class InstallerPM(private val downloadedApk: File) {

    private val tempFile = File(Updater.context.externalCacheDir, "install.apk")

    suspend fun startInstall(): String {
        return try {
            withContext(IO) {
                tempFile.delete()
                downloadedApk.inputStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            withContext(Main){
                Updater.context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    setDataAndType(Tools().getUri(tempFile), "application/vnd.android.package-archive")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                })
                ""
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            e.message.toString()
        }

    }

}