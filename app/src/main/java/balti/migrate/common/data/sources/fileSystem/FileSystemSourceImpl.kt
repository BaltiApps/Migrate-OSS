package balti.migrate.common.data.sources.fileSystem

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.exceptions.UnknownFileTypeException

class FileSystemSourceImpl(
    private val applicationContext: Context,
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
                copyJavaFileToMediaStoreDownloads(source, destination).also {
                    if (it) {
                        source.file.deleteRecursively()
                    }
                }
            }
            else -> throw UnknownFileTypeException(
                message = "Unknown move - Source type - ${source::class.java} and destination type - ${destination::class.java}"
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

    private fun copyJavaFileToMediaStoreDownloads(
        source: JavaFile,
        destinationDirectory: MediaStoreDownloadFile,
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
        return true
    }

}