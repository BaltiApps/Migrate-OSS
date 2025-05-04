package balti.migrate.common.data.sources.fileSystem

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.exceptions.UnknownFileTypeException
import timber.log.Timber
import java.io.File

class FileSystemSourceImpl(
    private val applicationContext: Context,
    private val dbUtils: DBUtils,
) : FileSystemSource() {

    override fun createDirectory(directory: GenericFile): Boolean {
        return when(directory) {
            is JavaFile -> {
                directory.file.mkdirs()
                directory.file.canWrite()
            }
            is MediaStoreDownloadFile -> createNoMediaFile(directory)
            else -> false
        }
    }

    override fun moveDirectory(
        source: GenericFile,
        destination: GenericFile,
        relativeFilePathFilter: (String) -> Boolean,
    ): Boolean {
        return when {
            source is JavaFile && destination is JavaFile -> {
                source.file.renameTo(destination.file)
            }
            source is JavaFile && destination is MediaStoreDownloadFile -> {
                transferJavaFileToMediaStoreDownloads(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = true,
                    relativeFilePathFilter = relativeFilePathFilter,
                )
            }
            source is MediaStoreDownloadFile && destination is JavaFile -> {
                transferMediaStoreDownloadsToJavaFile(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = true,
                    relativeFilePathFilter = relativeFilePathFilter,
                )
            }
            else -> throw UnknownFileTypeException(
                message = "Unknown move - Source type - ${source::class.java} and destination type - ${destination::class.java}"
            )
        }
    }

    override fun copyDirectory(
        source: GenericFile,
        destination: GenericFile,
        relativeFilePathFilter: (String) -> Boolean,
    ): Boolean {
        return when {
            source is JavaFile && destination is JavaFile -> {
                source.file.copyRecursively(destination.file, overwrite = true)
            }
            source is JavaFile && destination is MediaStoreDownloadFile -> {
                transferJavaFileToMediaStoreDownloads(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = false,
                    relativeFilePathFilter = relativeFilePathFilter,
                )
            }
            source is MediaStoreDownloadFile && destination is JavaFile -> {
                transferMediaStoreDownloadsToJavaFile(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = false,
                    relativeFilePathFilter = relativeFilePathFilter,
                )
            }
            else -> throw UnknownFileTypeException(
                message = "Unknown copy - Source type - ${source::class.java} and destination type - ${destination::class.java}"
            )
        }
    }

    private fun createNoMediaFile(file: MediaStoreDownloadFile): Boolean {

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, ".nomedia")
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, file.path)
        }

        return try {
            val uri = applicationContext.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )
            uri?.let {
                applicationContext.contentResolver.openOutputStream(it)?.use {}
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun transferJavaFileToMediaStoreDownloads(
        source: JavaFile,
        destinationDirectory: MediaStoreDownloadFile,
        deleteSource: Boolean,
        relativeFilePathFilter: (String) -> Boolean = { true },
    ): Boolean {
        Timber.i("TJM - Transfer JavaFile -> MediaStoreDownloadFile")
        val resolver = applicationContext.contentResolver

        Timber.i("TJM - source path - ${source.path}")
        Timber.i("TJM - dest. path - ${destinationDirectory.path}")

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

    private fun transferMediaStoreDownloadsToJavaFile(
        source: MediaStoreDownloadFile,
        destinationDirectory: JavaFile,
        deleteSource: Boolean,
        relativeFilePathFilter: (String) -> Boolean = { true },
    ): Boolean {
        Timber.i("TMJ - Transfer MediaStoreDownloadFile -> JavaFile")
        val resolver = applicationContext.contentResolver

        Timber.i("TMJ - source path - ${source.path}")
        Timber.i("TMJ - dest. path - ${destinationDirectory.path}")

        val sourcePath = source.path.trimEnd('/')

        val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("$sourcePath/%")

        destinationDirectory.file.mkdirs()

        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            null,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->

            Timber.i("TJM - cursor count - ${cursor.count}")

            while (cursor.moveToNext()) {
                val id = dbUtils.getCursorData<Long>(cursor, MediaStore.Downloads._ID)
                val name = dbUtils.getCursorData<String>(cursor, MediaStore.Downloads.DISPLAY_NAME)

                val relDirPath = relativeDirectoryPath(
                    source = source,
                    cursor = cursor,
                )

                val relativeFilePath = "$relDirPath/$name"

                Timber.i("TMJ - relative dir path - $relDirPath")
                Timber.i("TMJ - relative file path - $relativeFilePath")

                try {
                    if (relativeFilePathFilter(relativeFilePath)) {
                        val targetFileParent = File(destinationDirectory.file, relDirPath)
                        targetFileParent.mkdirs()

                        val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                        val targetFile = File(targetFileParent, sanitizeFilename(name))

                        Timber.i("TMJ - path to copy to - ${targetFile.absolutePath}")
                        Timber.i("TMJ - copy from $uri")

                        resolver.openInputStream(uri)?.use { input ->
                            targetFile.outputStream().use { output -> input.copyTo(output) }
                        }

                        Timber.i("TMJ - copy from $uri success")
                        if (deleteSource) {
                            Timber.i("TMJ - delete $uri")
                            resolver.delete(uri, null, null)
                        }
                    } else {
                        Timber.i("TMJ - not copying file, relative path \"$relativeFilePath\" did not qualify")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Timber.e("TMJ - exception - ${e.message}")
                    return false
                }
            }
        }

        return true
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
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

    /**
     * Example 1:
     *
     * source - Download/Migrate/03-May
     * cursor - Download/Migrate/03-May/dirB/f1.txt
     *   relative path - Download/Migrate/03-May/dirB/
     *   name - f1.txt (not used here)
     *
     * Example 2:
     *
     * source - Download/Migrate/03-May
     * cursor - Download/Migrate/03-May/f1.txt
     *   relative path - Download/Migrate/03-May/
     *   name - f1.txt (not used here)
     *
     * Output - (blank)
     */
    private fun relativeDirectoryPath(source: MediaStoreDownloadFile, cursor: Cursor): String {
        val relPath = kotlin.runCatching {
            dbUtils.getCursorData<String>(cursor, MediaStore.Downloads.RELATIVE_PATH)
        }.getOrNull() ?: return ""
        return relPath
            .substringAfter(source.path)
            .trimEnd('/')
    }

}