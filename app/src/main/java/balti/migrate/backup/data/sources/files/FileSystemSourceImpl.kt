package balti.migrate.backup.data.sources.files

import baltiapps.migrate.domain.backup.sources.FileSystemSource
import java.io.File

class FileSystemSourceImpl : FileSystemSource() {
    override fun checkPermission(filePath: String): Boolean {
        return File(filePath).canWrite()
    }

    override fun createDirectory(dirPath: String): Boolean {
        File(dirPath).mkdirs()
        return checkPermission(dirPath)
    }
}