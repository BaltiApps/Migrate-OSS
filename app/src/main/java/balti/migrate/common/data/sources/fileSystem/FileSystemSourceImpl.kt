package balti.migrate.common.data.sources.fileSystem

import baltiapps.migrate.domain.common.model.Directory
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import java.io.File

class FileSystemSourceImpl : FileSystemSource() {

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
}