package balti.migrate.common.data.sources.fileSystem

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.SafFile
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class TransferUtilsSafFile(
    private val applicationContext: Context,
) {
    fun createDirectory(
        directory: SafFile,
    ): Boolean {
        if (!TransferUtils.hasPermission(applicationContext, directory.uriToLocation)) {
            return false
        }

        val rootDir = DocumentFile.fromTreeUri(applicationContext, directory.uriToLocation)

        return when {
            rootDir == null -> false
            rootDir.findFile(directory.name) != null -> true
            else -> rootDir.createDirectory(directory.name) != null
        }
    }

    fun transferJavaFileToSafFile(
        source: JavaFile,
        destinationDirectory: SafFile,
        deleteSource: Boolean,
        relativeFilePathFilter: (String) -> Boolean = { true },
    ): Boolean {
        Timber.i("TJS - Transfer JavaFile -> SafFile")

        Timber.i("TJS - source path - ${source.path}")
        Timber.i("TJS - dest. path - ${destinationDirectory.path}")

        val destination =
            DocumentFile.fromTreeUri(applicationContext, destinationDirectory.uriToLocation)
                ?: return false

        try {
            Timber.i("TJS - attempt transfer")
            recursiveCopy(
                originalSource = source,
                currentFile = source.file,
                destination = destination,
                deleteSource = deleteSource,
                relativeFilePathFilter = relativeFilePathFilter,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.e("TJS - exception - ${e.message}")
            return false
        }
        Timber.i("TJS - finished all transfers")
        return true
    }

    private fun recursiveCopy(
        originalSource: JavaFile,
        currentFile: File,
        destination: DocumentFile,
        deleteSource: Boolean,
        relativeFilePathFilter: (String) -> Boolean,
    ) {
        val relDirPath = TransferUtils.relativeDirectoryPath(
            source = originalSource,
            currentFile = currentFile,
        )

        val relativeFilePath = "$relDirPath/${currentFile.name}"

        Timber.i("TJS - relative dir path - $relDirPath")
        Timber.i("TJS - relative file path - $relativeFilePath")

        if (!relativeFilePathFilter(relativeFilePath)) {
            Timber.i("TJS - not copying file, relative path \"$relativeFilePath\" did not qualify")
            return
        }

        if (currentFile.isDirectory) {
            Timber.i("TJS - copying directory - ${currentFile.absolutePath}")
            val newDestination = destination.findFile(currentFile.name)
                ?.takeIf { it.isDirectory }
                ?: destination.createDirectory(currentFile.name)!!
            val files = currentFile.listFiles()
            files?.forEach { file ->
                Timber.i("TJS - start recursion - ${file.absolutePath}")
                recursiveCopy(
                    originalSource = originalSource,
                    currentFile = file,
                    destination = newDestination,
                    deleteSource = deleteSource,
                    relativeFilePathFilter = relativeFilePathFilter,
                )
            }
        } else {
            Timber.i("TJS - copying file - ${currentFile.absolutePath}")
            Timber.i("TJS - copy to ${destination.uri}")
            val newFile = destination.createFile("application/octet-stream", currentFile.name)!!
            copyFile(currentFile, newFile.uri)
            Timber.i("TJS - copy to ${destination.uri} success")
            if (deleteSource) {
                Timber.i("TJS - delete ${currentFile.absolutePath}")
                currentFile.delete()
            }
        }
    }

    private fun copyFile(source: File, destinationUri: Uri) {
        applicationContext.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            FileInputStream(source).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    fun transferSafFileToJavaFile(
        source: SafFile,
        destinationDirectory: JavaFile,
        deleteSource: Boolean,
        relativeFilePathFilter: (String) -> Boolean = { true },
    ): Boolean {
        Timber.i("TSJ - Transfer SafFile -> JavaFile")

        Timber.i("TSJ - source path - ${source.uriToLocation}")
        Timber.i("TSJ - dest. path - ${destinationDirectory.path}")

        val sourceDoc = DocumentFile.fromTreeUri(applicationContext, source.uriToLocation)
            ?: return false

        try {
            Timber.i("TSJ - attempt transfer")
            recursiveCopy(
                source = sourceDoc,
                destination = destinationDirectory.file,
                originalDestination = destinationDirectory.file,
                deleteSource = deleteSource,
                relativeFilePathFilter = relativeFilePathFilter,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.e("TSJ - exception - ${e.message}")
            return false
        }

        Timber.i("TSJ - finished all transfers")
        return true
    }

    private fun recursiveCopy(
        source: DocumentFile,
        destination: File,
        originalDestination: File,
        deleteSource: Boolean,
        relativeFilePathFilter: (String) -> Boolean,
    ) {
        if (source.isDirectory) {
            Timber.i("TSJ - recursing for directory - ${source.uri}")
            for (child in source.listFiles()) {
                val fileName = child.name ?: continue
                val newDestination = File(destination, fileName)
                Timber.i("TSJ - new destination - ${newDestination.absolutePath}")
                recursiveCopy(
                    source = child,
                    destination = newDestination,
                    originalDestination = originalDestination,
                    deleteSource = deleteSource,
                    relativeFilePathFilter = relativeFilePathFilter,
                )
            }
        } else {
            Timber.i("TSJ - copying file - ${source.uri}")

            val relDirPath = TransferUtils.relativeDirectoryPath(
                parent = originalDestination,
                child = destination,
            )

            val relativeFilePath = "$relDirPath/${source.name}"

            Timber.i("TSJ - relative dir path - $relDirPath")
            Timber.i("TSJ - relative file path - $relativeFilePath")

            if (!relativeFilePathFilter(relativeFilePath)) {
                Timber.i("TSJ - not copying, did not qualify: $relativeFilePath")
                return
            }

            Timber.i("TSJ - copy to - ${destination.absolutePath}, relative dir - $relDirPath")

            File(originalDestination, relDirPath).mkdirs()

            copyFile(source.uri, destination)
            if (deleteSource) {
                Timber.i("TSJ - deleting source - ${source.uri}")
                source.delete()
            }
        }
    }

    private fun copyFile(sourceUri: Uri, destination: File) {
        applicationContext.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }
}