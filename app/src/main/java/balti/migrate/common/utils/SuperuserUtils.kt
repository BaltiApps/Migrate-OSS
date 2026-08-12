package balti.migrate.common.utils

import android.content.Context
import androidx.annotation.RawRes
import baltiapps.migrate.domain.exceptions.SuperuserException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class SuperuserUtils(
    private val applicationContext: Context,
) {
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

    suspend fun runScript(
        scriptPath: String,
        parentSuperuserShell: Process,
        endMarker: String,
        onProgress: suspend (String) -> Unit,
        onError: suspend (String) -> Unit,
        vararg args : String,
    ) {
        withContext(Dispatchers.IO) {
            val writer = BufferedWriter(OutputStreamWriter(parentSuperuserShell.outputStream))
            val errorReader = BufferedReader(InputStreamReader(parentSuperuserShell.errorStream))
            val outputReader = BufferedReader(InputStreamReader(parentSuperuserShell.inputStream))

            val command = "sh $scriptPath ${args.joinToString(" ") { "\"$it\"" }}"

            Timber.i(command)

            writer.write("${command}\n")
            writer.flush()

            while (true) {
                val line = outputReader.readLine()
                Timber.v(line)
                if (line == endMarker) {
                    break
                } else {
                    onProgress(line)
                }
            }

            while (true) {
                val line = errorReader.readLine()
                Timber.e(line)
                if (line == endMarker) {
                    break
                } else {
                    onError(line)
                }
            }
        }
    }

    suspend fun runCommand(
        command: String,
        parentSuperuserShell: Process,
        onFinish: (success: Boolean, message: String) -> Unit,
    ) {
        val endMarker = "===end==="
        val output = StringBuilder()
        val error = StringBuilder()

        withContext(Dispatchers.IO) {
            val writer = BufferedWriter(OutputStreamWriter(parentSuperuserShell.outputStream))
            val errorReader = BufferedReader(InputStreamReader(parentSuperuserShell.errorStream))
            val outputReader = BufferedReader(InputStreamReader(parentSuperuserShell.inputStream))

            Timber.i(command)

            writer.write("${command}\n")
            writer.write("echo \"${endMarker}\"\n")
            writer.write("echo \"${endMarker}\" >&2\n")
            writer.write("exit\n")
            writer.flush()

            while (true) {
                val line = outputReader.readLine()
                Timber.v(line)
                if (line == endMarker) {
                    break
                } else {
                    output.appendLine(line)
                }
            }

            while (true) {
                val line = errorReader.readLine()
                Timber.e(line)
                if (line == endMarker) {
                    break
                } else {
                    error.appendLine(line)
                }
            }

            val success = error.isBlank()

            onFinish(success, if (success) output.toString() else error.toString())
        }
    }

    suspend fun unpackScript(
        @RawRes scriptRes: Int,
        scriptLocation: String,
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val inputStream = applicationContext.resources.openRawResource(scriptRes)
            val outputStream = FileOutputStream(File(scriptLocation))
            try {
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun getSuperuserShell(): Process {
        return Runtime.getRuntime().exec("su --mount-master")
    }

    fun getNonElevatedShell(): Process {
        return Runtime.getRuntime().exec("sh")
    }

    fun closeSuperuserShell(process: Process) {
        val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
        writer.write("exit\n")
        writer.flush()
        process.waitFor()
    }
}