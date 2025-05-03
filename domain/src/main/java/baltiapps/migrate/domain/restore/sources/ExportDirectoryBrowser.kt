package baltiapps.migrate.domain.restore.sources

import baltiapps.migrate.domain.common.model.GenericFile

interface ExportDirectoryBrowser<T: GenericFile> {
    fun getDirectories(root: T): List<T>
    fun getFilesUnder(directory: T): List<T>
}