package balti.migrate.common.data.sources.fileSystem

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource

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
}