package balti.migrate.common.data.sources.fileSystem

import android.database.Cursor
import android.provider.MediaStore
import balti.migrate.common.data.model.JavaFile
import balti.migrate.common.data.model.MediaStoreDownloadFile
import balti.migrate.common.utils.DBUtils
import java.io.File

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
fun relativeDirectoryPath(source: JavaFile, currentFile: File): String {
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
fun relativeDirectoryPath(source: MediaStoreDownloadFile, cursor: Cursor, dbUtils: DBUtils): String {
    val relPath = runCatching {
        dbUtils.getCursorData<String>(cursor, MediaStore.Downloads.RELATIVE_PATH)
    }.getOrNull() ?: return ""
    return relPath
        .substringAfter(source.path)
        .trimEnd('/')
}