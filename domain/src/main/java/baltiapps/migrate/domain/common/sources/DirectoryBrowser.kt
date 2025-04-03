package baltiapps.migrate.domain.common.sources

import baltiapps.migrate.domain.common.model.Directory

interface DirectoryBrowser {
    fun isDirectoryAccessible(path: String): Boolean
    suspend fun getDirectoriesUnder(directory: Directory): List<Directory>
    suspend fun isValidBackupDirectory(path: String): Boolean
}