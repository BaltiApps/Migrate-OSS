package balti.migrate.utilities

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ToolsNoContext {

    companion object{

        fun applyNamingCorrectionForShell(name: String) =
                name.replace("(", "\\(").replace(")", "\\)").replace(" ", "\\ ")

        fun getDirLength(file: File): Long {
            return if (file.exists()) {
                if (!file.isDirectory) file.length()
                else {
                    val files = file.listFiles()
                    var sum = 0L
                    for (f in files) sum += getDirLength(f)
                    sum
                }
            } else 0
        }

        fun copyFile(sourceFile: File, destinationAddress: String, differentName: String? = null): String {

            if (!(sourceFile.exists() && sourceFile.canRead())) return "source does not exist: ${sourceFile.absolutePath}"
            File(destinationAddress).run {
                mkdirs()
                if (canWrite()) return "cannot write on destination: $destinationAddress"
            }

            val destinationFile = File(destinationAddress, differentName ?: sourceFile.name)
            if (destinationFile.exists()) destinationFile.delete()

            var read: Int
            val buffer = ByteArray(4096)

            return try {
                val inputStream = sourceFile.inputStream()
                val writer = FileOutputStream(destinationFile)
                while (true) {
                    read = inputStream.read(buffer)
                    if (read > 0) writer.write(buffer, 0, read)
                    else break
                }
                writer.close()
                destinationFile.setExecutable(true)
                return ""
            } catch (e: IOException){
                e.printStackTrace()
                e.message.toString()
            }

        }

        fun moveFile(sourceFile: File, destinationAddress: String, differentName: String? = null): String {
            val r = copyFile(sourceFile, destinationAddress, differentName)
            sourceFile.delete()
            return r
        }
    }

}