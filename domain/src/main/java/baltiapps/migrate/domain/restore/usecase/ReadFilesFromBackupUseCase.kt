package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.common.utils.BackupFilesUtils
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReadFilesFromBackupUseCase(
    private val restoreDataRepository: RestoreDataRepository,
    private val fileSystemSource: FileSystemSource,
) {
    suspend operator fun invoke(
        exportDirectory: GenericFile,
        importDirectory: GenericFile,
        getGenericFileForRepository: (relativePath: String) -> GenericFile,
    ) {
        withContext(Dispatchers.IO) {
            restoreDataRepository.resetRepository()
            restoreDataRepository.setExportDirectory(exportDirectory)

            fileSystemSource.copyDirectory(
                source = exportDirectory,
                destination = importDirectory,
                relativeFilePathFilter = { relativeFilePath ->
                    val shouldImport = BackupFilesUtils.shouldImportFile(relativeFilePath)
                    if (shouldImport) {
                        val file = getGenericFileForRepository(relativeFilePath)
                        restoreDataRepository.backupFiles.add(file)
                    }
                    shouldImport
                }
            )
        }
    }
}