package balti.migrate.common.data.sources.fileSystem

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.data.model.SafFile
import balti.migrate.common.utils.DBUtils
import java.io.File

object TransferUtils {
    /**
     * Example 1:
     *
     * parent - /root/dirA
     * child - /root/dirA/dirB/f1.txt
     *
     * Output - /dirB
     *
     * Example 2:
     *
     * parent - /root/dirA
     * child - /root/dirA/f1.txt
     *
     * Output - (blank)
     */
    fun relativeDirectoryPath(parent: File, child: File): String {
        return child.absolutePath
            .substringAfter(parent.absolutePath)
            .removeSuffix(child.name)
            .trimEnd('/')
    }

    fun relativeDirectoryPath(source: JavaFile, currentFile: File): String {
        return relativeDirectoryPath(
            parent = source.file,
            child = currentFile
        )
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
    fun relativeDirectoryPath(
        source: MediaStoreDownloadFile,
        cursor: Cursor,
        dbUtils: DBUtils
    ): String {
        val relPath = runCatching {
            dbUtils.getCursorData<String>(cursor, MediaStore.Downloads.RELATIVE_PATH)
        }.getOrNull() ?: return ""
        return relPath
            .substringAfter(source.path)
            .trimEnd('/')
    }

    fun hasPermission(context: Context, uri: Uri): Boolean {
        return context.applicationContext.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
    }

    fun getSafFileName(
        context: Context,
        file: SafFile
    ): String {
        return file.name.ifBlank {
            DocumentFile.fromTreeUri(context, file.uriToLocation)?.name ?: ""
        }
    }

    fun getSafFilePath(
        file: SafFile
    ): String {
        val uri = file.uriToLocation.toString()
        val androidContentUri = "content://com.android.externalstorage.documents/tree/"

        if (!uri.startsWith(androidContentUri)) return ""

        val internalStoragePath = Environment.getExternalStorageDirectory().path

        val path = uri.substringAfter(androidContentUri)
            .let {
                val head = it.substringBefore("%3A")
                if (head == "primary") {
                    "$internalStoragePath/${it.substringAfter("${head}%3A")}"
                } else {
                    "/mnt/media_rw/$head/${it.substringAfter("${head}%3A")}"
                }
            }
            .replace("%3A", "/")
            .replace("%2F", "/")

        return path
    }
}