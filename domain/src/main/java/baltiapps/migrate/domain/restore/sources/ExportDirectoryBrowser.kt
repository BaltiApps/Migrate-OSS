package baltiapps.migrate.domain.restore.sources

import baltiapps.migrate.domain.common.model.GenericFile

interface ExportDirectoryBrowser<T: GenericFile> {
    suspend fun getDirectories(root: T): List<T>
    suspend fun getFilesUnder(directory: T): List<T>
}