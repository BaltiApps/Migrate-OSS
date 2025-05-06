package balti.migrate.common.data.sources.fileSystem

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.utils.DBUtils
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemUtils
import timber.log.Timber
import java.io.File

class MediaStoreDownloadUtils(
    private val applicationContext: Context,
    private val dbUtils: DBUtils,
) {
    private val fsUtils by lazy {
        FileSystemUtils()
    }

    fun createNoMediaFile(file: MediaStoreDownloadFile): Boolean {

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

    fun transferMediaStoreDownloadsToJavaFile(
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
                        val targetFile = File(targetFileParent, fsUtils.sanitizeFilename(name))

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