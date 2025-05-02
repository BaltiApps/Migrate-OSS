package balti.migrate.common.data.sources.fileSystem

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import baltiapps.migrate.domain.common.model.Directory
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import java.io.File

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

    override fun createDirectory(dirPath: String): Boolean {
        val file = File(dirPath)
        file.mkdirs()
        return file.canWrite()
    }

    override fun createDirectory(directory: Directory): Boolean {
        val javaDirectory = File(directory.directoryFullPath)
        javaDirectory.mkdirs()
        return javaDirectory.canWrite()
    }

    private fun createNoMediaFile(file: MediaStoreDownloadFile): Boolean {
        val relativePath = file.subDirectoryPath

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, ".nomedia")
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
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
}