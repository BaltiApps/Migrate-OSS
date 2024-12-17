package balti.migrate.backup.data.sources.files

import baltiapps.migrate.domain.backup.sources.PlatformFileSystemSource
import java.io.File

class FileSystemSource : PlatformFileSystemSource() {
    override fun checkPermission(filePath: String): Boolean {
        return File(filePath).canWrite()
    }

    override fun createDirectory(dirPath: String): Boolean {
        File(dirPath).mkdirs()
        return checkPermission(dirPath)
    }
}