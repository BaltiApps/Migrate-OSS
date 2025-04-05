package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.Directory
import baltiapps.migrate.domain.common.sources.fileSystem.DirectoryBrowser
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository

class ReadFilesFromBackupUseCase(
    private val directoryBrowser: DirectoryBrowser,
    private val restoreDataRepository: RestoreDataRepository,
) {
    suspend operator fun invoke(directory: Directory) {
        directoryBrowser.getFilesUnder(directory).run {
            restoreDataRepository.backupFiles.clearAndAddAll(this)
        }
    }
}