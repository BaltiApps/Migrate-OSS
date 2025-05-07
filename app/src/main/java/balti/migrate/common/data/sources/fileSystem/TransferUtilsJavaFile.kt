package balti.migrate.common.data.sources.fileSystem

import balti.migrate.common.data.model.JavaFile
import timber.log.Timber
import java.io.File

class TransferUtilsJavaFile {
    fun transferJavaFileToJavaFile(
        source: JavaFile,
        destinationDirectory: JavaFile,
        deleteSource: Boolean,
        relativeFilePathFilter: (String) -> Boolean = { true },
    ): Boolean {
        Timber.i("TJJ - Transfer JavaFile -> JavaFile")
        Timber.i("TJJ - source path - ${source.path}")
        Timber.i("TJJ - dest. path - ${destinationDirectory.path}")

        source.file.walkTopDown().forEach { file ->
            Timber.i("TJJ - file to copy - ${file.absolutePath}")

            val relDirPath = TransferUtils.relativeDirectoryPath(
                source = source,
                currentFile = file,
            )

            val relativeFilePath = "$relDirPath/${file.name}"

            Timber.i("TJJ - relative dir path - $relDirPath")
            Timber.i("TJJ - relative file path - $relativeFilePath")

            try {
                Timber.i("TJJ - attempt transfer")
                if (relativeFilePathFilter(relativeFilePath)) {
                    val targetDir = File(destinationDirectory.file, relDirPath)
                    targetDir.mkdirs()

                    Timber.i("TJJ - create relative directory - ${targetDir.absolutePath}")

                    val targetFile = File(targetDir, file.name)

                    Timber.i("TJJ - copy to ${targetFile.absolutePath}")
                    file.copyTo(targetFile, overwrite = true)
                    Timber.i("TJJ - copy to ${targetFile.absolutePath} success")

                    if (deleteSource) {
                        file.delete().apply {
                            Timber.i("TJJ - deleted file ${file.absolutePath} - success - $this")
                        }
                    }
                } else {
                    Timber.i("TJJ - not copying file, relative path \"$relativeFilePath\" did not qualify")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Timber.e("TJJ - exception - ${e.message}")
                return false
            }
        }

        Timber.i("TJJ - finished all transfers")
        return true
    }
}