package balti.migrate.common.data.sources.fileSystem

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.data.model.SafFile
import balti.migrate.common.utils.DBUtils
import timber.log.Timber
import java.io.File

object TransferUtils {

    val internalStoragePath: String
        get() = Environment.getExternalStorageDirectory().path

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

    fun getUriFileName(
        context: Context,
        file: SafFile,
    ): String {
        return file.name.ifBlank {
            getUriFileName(context, file.uriToLocation)
        }
    }

    fun getUriFileName(
        context: Context,
        uri: Uri,
    ): String {
        return DocumentFile.fromTreeUri(context, uri)?.name ?: ""
    }

    fun getUriFilePath(
        file: SafFile,
    ): String {
        return getUriFilePath(file.uriToLocation)
    }

    fun getUriFilePath(
        uri: Uri,
    ): String {
        val uriString = uri.toString()
        val androidContentUri = "content://com.android.externalstorage.documents/tree/"

        if (!uriString.startsWith(androidContentUri)) return ""

        val path = uriString.substringAfter(androidContentUri)
            .let {
                val head = it.substringBefore("%3A")
                if (head == "primary") {
                    "$internalStoragePath/${it.substringAfterLast("${head}%3A")}"
                } else {
                    "/mnt/media_rw/$head/${it.substringAfterLast("${head}%3A")}"
                }
            }
            .replace("%2F", "/")

        return path
    }

    fun getStatFsForSafUri(safUri: Uri, context: Context): StatFs? {
        val docId = DocumentsContract.getTreeDocumentId(safUri)
        val docUri = DocumentsContract.buildDocumentUriUsingTree(safUri, docId)
        Timber.d("getStatFsForSafUri : docId - $docId : docUri - $docUri")

        return context.contentResolver.openFileDescriptor(docUri, "r")?.use { pfd ->
            val pfdPath = "/proc/self/fd/${pfd.fd}"
            Timber.d("Using pfd path for StatFs: $pfdPath")
            StatFs(pfdPath)
        }
    }
}