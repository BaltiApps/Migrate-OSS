package balti.migrate.common.data.sources.fileSystem

import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
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