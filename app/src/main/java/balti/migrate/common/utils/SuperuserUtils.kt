package balti.migrate.common.utils

import baltiapps.migrate.domain.exceptions.SuperuserException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class SuperuserUtils {
    suspend fun checkSuperuserPermission(): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val process = Runtime.getRuntime().exec("su")
                val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
                writer.write("exit\n")
                writer.flush()
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val outputReader = BufferedReader(InputStreamReader(process.inputStream))

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

                process.waitFor()
                val exitValue = process.exitValue()

                if (exitValue == 0) {
                    Result.success(Unit)
                } else {
                    Result.failure(SuperuserException(exitValue, errorMessage))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}