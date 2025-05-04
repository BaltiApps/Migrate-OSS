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
        val resolver = applicationContext.contentResolver

        source.file.walkTopDown().filter { it.isFile }.forEach { file ->

            val relDirPath = relativeDirectoryPath(
                source = source,
                currentFile = file,
            )

            val relativeFilePath = "$relDirPath/${file.name}"

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
                    uri?.let {
                        resolver.openOutputStream(it)?.use { out ->
                            file.inputStream().use { input -> input.copyTo(out) }
                        }
                    }
                    if (deleteSource) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
        val resolver = applicationContext.contentResolver

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

            while (cursor.moveToNext()) {
                val id = dbUtils.getCursorData<Long>(cursor, MediaStore.Downloads._ID)
                val name = dbUtils.getCursorData<String>(cursor, MediaStore.Downloads.DISPLAY_NAME)

                val relDirPath = relativeDirectoryPath(
                    source = source,
                    cursor = cursor,
                )

                val relativeFilePath = "$relDirPath/$name"

                try {
                    if (relativeFilePathFilter(relativeFilePath)) {
                        val targetFileParent = File(destinationDirectory.file, relDirPath)
                        targetFileParent.mkdirs()

                        val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                        val targetFile = File(targetFileParent, sanitizeFilename(name))

                        resolver.openInputStream(uri)?.use { input ->
                            targetFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        if (deleteSource) {
                            resolver.delete(uri, null, null)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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