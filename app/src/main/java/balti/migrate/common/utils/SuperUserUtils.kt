package balti.migrate.common.utils

import baltiapps.migrate.domain.exceptions.SuException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class SuperUserUtils {
    suspend fun checkRootPermission(): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val suRequest = Runtime.getRuntime().exec("su")
                val writer = BufferedWriter(OutputStreamWriter(suRequest.outputStream))
                writer.write("exit\n")
                writer.flush()
                val errorReader = BufferedReader(InputStreamReader(suRequest.errorStream))
                val outputReader = BufferedReader(InputStreamReader(suRequest.inputStream))

                var line: String?
                var errorMessage = ""

                while (true) {
                    line = outputReader.readLine()
                    if (line != null) errorMessage = errorMessage + line + "\n"
                    else break
                }
                errorMessage += "Error:\n\n"
                while (true) {
                    line = errorReader.readLine()
                    if (line != null) errorMessage = errorMessage + line + "\n"
                    else break
                }

                suRequest.waitFor()
                val exitValue = suRequest.exitValue()

                if (exitValue == 0) {
                    Result.success(Unit)
                } else {
                    Result.failure(SuException(exitValue, errorMessage))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}