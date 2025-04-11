package balti.migrate.common.data.sources.fileSystem

import baltiapps.migrate.domain.common.model.Directory
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import java.io.File

class FileSystemSourceImpl : FileSystemSource() {
    override fun checkPermission(filePath: String): Boolean {
        return File(filePath).canWrite()
    }

    override fun checkPermission(file: GenericFile): Boolean {
        return file.canWrite
    }

    override fun checkPermission(directory: Directory): Boolean {
        return File(directory.directoryFullPath).canWrite()
    }

    override fun createDirectory(dirPath: String): Boolean {
        File(dirPath).mkdirs()
        return checkPermission(dirPath)
    }

    override fun createDirectory(directory: Directory): Boolean {
        val javaDirectory = File(directory.directoryFullPath)
        javaDirectory.mkdirs()
        return checkPermission(directory)
    }
}