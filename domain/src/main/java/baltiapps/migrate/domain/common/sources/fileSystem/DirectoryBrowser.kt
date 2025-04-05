package baltiapps.migrate.domain.common.sources.fileSystem

import baltiapps.migrate.domain.common.model.Directory
import baltiapps.migrate.domain.common.model.GenericFile

interface DirectoryBrowser {
    fun isDirectoryAccessible(path: String): Boolean
    suspend fun getDirectoriesUnder(directory: Directory): List<Directory>
    suspend fun getFilesUnder(directory: Directory): List<GenericFile>
    suspend fun isValidBackupDirectory(path: String): Boolean
}