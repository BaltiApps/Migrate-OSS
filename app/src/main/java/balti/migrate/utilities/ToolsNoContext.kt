package balti.migrate.utilities

import java.io.File

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
    }

}