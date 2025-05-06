package balti.migrate.common.data.sources.fileSystem

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import timber.log.Timber
import java.io.File

class JavaFileUtils(
    private val applicationContext: Context,
) {
    fun transferJavaFileToMediaStoreDownloads(
        source: JavaFile,
        destinationDirectory: MediaStoreDownloadFile,
        deleteSource: Boolean,
        relativeFilePathFilter: (String) -> Boolean = { true },
    ): Boolean {
        Timber.i("TJM - Transfer JavaFile -> MediaStoreDownloadFile")
        Timber.i("TJM - source path - ${source.path}")
        Timber.i("TJM - dest. path - ${destinationDirectory.path}")

        val resolver = applicationContext.contentResolver

        source.file.walkTopDown().filter { it.isFile }.forEach { file ->

            Timber.i("TJM - file to copy - ${file.absolutePath}")

            val relDirPath = relativeDirectoryPath(
                source = source,
                currentFile = file,
            )

            val relativeFilePath = "$relDirPath/${file.name}"

            Timber.i("TJM - relative dir path - $relDirPath")
            Timber.i("TJM - relative file path - $relativeFilePath")

            try {
                if (relativeFilePathFilter(relativeFilePath)) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                        put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                        put(MediaStore.Downloads.RELATIVE_PATH, destinationDirectory.path + relDirPath)
                    }

                    val uri = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                    Timber.i("TJM - copy to $uri")

                    uri?.let {
                        resolver.openOutputStream(it)?.use { out ->
                            file.inputStream().use { input -> input.copyTo(out) }
                        }
                    }

                    Timber.i("TJM - copy to $uri success")

                    if (deleteSource) {
                        file.delete().apply {
                            Timber.i("TJM - deleted file ${file.absolutePath} - success - $this")
                        }
                    }
                } else {
                    Timber.i("TJM - not copying file, relative path \"$relativeFilePath\" did not qualify")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Timber.e("TJM - exception - ${e.message}")
                return false
            }
        }

        if (deleteSource) {
            source.file.delete()
        }

        return true
    }

    /**
     * Example 1:
     *
     * Source - /root/dirA
     * currentFile - /root/dirA/dirB/f1.txt
     *
     * Output - /dirB
     *
     * Example 2:
     *
     * Source - /root/dirA
     * currentFile - /root/dirA/f1.txt
     *
     * Output - (blank)
     */
    private fun relativeDirectoryPath(source: JavaFile, currentFile: File): String {
        return currentFile.absolutePath
            .substringAfter(source.file.absolutePath)
            .removeSuffix(currentFile.name)
            .trimEnd('/')
    }
}