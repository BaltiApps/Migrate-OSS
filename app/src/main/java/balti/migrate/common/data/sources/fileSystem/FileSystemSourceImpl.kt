package balti.migrate.common.data.sources.fileSystem

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
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

    override fun moveDirectory(source: GenericFile, destination: GenericFile): Boolean {
        return when {
            source is JavaFile && destination is JavaFile -> {
                source.file.renameTo(destination.file)
            }
            source is JavaFile && destination is MediaStoreDownloadFile -> {
                transferJavaFileToMediaStoreDownloads(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = true
                )
            }
            source is MediaStoreDownloadFile && destination is JavaFile -> {
                transferMediaStoreDownloadsToJavaFile(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = true
                )
            }
            else -> throw UnknownFileTypeException(
                message = "Unknown move - Source type - ${source::class.java} and destination type - ${destination::class.java}"
            )
        }
    }

    override fun copyDirectory(source: GenericFile, destination: GenericFile): Boolean {
        return when {
            source is JavaFile && destination is JavaFile -> {
                source.file.copyRecursively(destination.file, overwrite = true)
            }
            source is JavaFile && destination is MediaStoreDownloadFile -> {
                transferJavaFileToMediaStoreDownloads(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = false
                )
            }
            source is MediaStoreDownloadFile && destination is JavaFile -> {
                transferMediaStoreDownloadsToJavaFile(
                    source = source,
                    destinationDirectory = destination,
                    deleteSource = false
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
    ): Boolean {

        val sourcePath = source.file.takeIf { it.exists() }?.absolutePath ?: return false
        // Example - /root/dirA

        source.file.walkTopDown().filter { it.isFile }.forEach { file ->

            val filePath = file.absolutePath // Example - /root/dirA/dirB/file1

            val fileRelativePath = filePath.removePrefix(sourcePath)
            // Example - /dirB/file1

            val targetRelativePath = destinationDirectory.path + // Example - Download/Migrate/02-May
                    fileRelativePath                             // /dirB/file1
                        .removeSuffix(file.name)                 // /dirB/
                        .removeSuffix("/")                 // /dirB
            // targetRelativePath - Download/Migrate/02-May/dirB

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, targetRelativePath)
            }

            val resolver = applicationContext.contentResolver

            try {
                val uri = resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                uri?.let {
                    resolver.openOutputStream(it)?.use { out ->
                        file.inputStream().use { input -> input.copyTo(out) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        if (deleteSource) {
            source.file.deleteRecursively()
        }

        return true
    }

    private fun transferMediaStoreDownloadsToJavaFile(
        source: MediaStoreDownloadFile,
        destinationDirectory: JavaFile,
        deleteSource: Boolean,
    ): Boolean {
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME
        )
        val selection = "${MediaStore.Downloads.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(source.path)

        destinationDirectory.file.mkdirs()

        val resolver = applicationContext.contentResolver

        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->

            while (cursor.moveToNext()) {
                val id = dbUtils.getCursorData<Long>(cursor, MediaStore.Downloads._ID)
                val name = dbUtils.getCursorData<String>(cursor, MediaStore.Downloads.DISPLAY_NAME)

                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                val targetFile = File(destinationDirectory.file, sanitizeFilename(name))

                try {
                    resolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (deleteSource) {
                        resolver.delete(uri, null, null)
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

}