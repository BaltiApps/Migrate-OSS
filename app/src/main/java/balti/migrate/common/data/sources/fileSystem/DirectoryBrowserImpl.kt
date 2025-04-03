package balti.migrate.common.data.sources.fileSystem

import baltiapps.migrate.domain.BACKUP_FILE_NAME_CALL_LOGS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_CONTACTS
import baltiapps.migrate.domain.BACKUP_FILE_NAME_SMS
import baltiapps.migrate.domain.common.model.Directory
import baltiapps.migrate.domain.common.sources.fileSystem.DirectoryBrowser
import java.io.File

class DirectoryBrowserImpl: DirectoryBrowser {
    override fun isDirectoryAccessible(path: String): Boolean {
        return File(path).run {
            this.isDirectory && this.canRead()
        }
    }

    override suspend fun isValidBackupDirectory(path: String): Boolean {
        if (!isDirectoryAccessible(path)) return false
        return File(path).run {
            this.listFiles { _, name ->
                name in listOf(
                    BACKUP_FILE_NAME_CONTACTS,
                    BACKUP_FILE_NAME_CALL_LOGS,
                    BACKUP_FILE_NAME_SMS,
                )
            }?.isNotEmpty() ?: false
        }
    }

    override suspend fun getDirectoriesUnder(directory: Directory): List<Directory> {
        if (!isDirectoryAccessible(directory.directoryFullPath)) return emptyList()
        return File(directory.directoryFullPath).run {
            this.listFiles { dir, _ -> isDirectoryAccessible(dir.absolutePath) }?.map {
                Directory(
                    directoryFullPath = it.absolutePath,
                    basePath = directory.basePath,
                    name = it.name,
                    parent = directory,
                    isValidBackupDirectory = isValidBackupDirectory(it.absolutePath)
                )
            }?: listOf()
        }
    }
}