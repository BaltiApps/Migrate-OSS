package balti.updater.installers

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.FileOutputStream

internal class InstallerSU(private val downloadedApk: File) {

    suspend fun startInstall(): String {
        return try {
            withContext(IO) {
                val process = Runtime.getRuntime().exec("su")
                val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
                writer.write("pm install -r -d -t ${downloadedApk.absolutePath}\n")
                writer.write("exit\n")
                writer.flush()

                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                var errorMessage = ""

                var line: String?
                while (true) {
                    line = errorReader.readLine()
                    if (line != null) errorMessage = errorMessage + line + "\n"
                    else break
                }

                process.waitFor()
                errorMessage.trim()
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            e.message.toString()
        }
    }

}