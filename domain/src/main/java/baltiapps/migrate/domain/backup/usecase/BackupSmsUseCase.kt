package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.model.SmsListItem
import baltiapps.migrate.domain.common.sources.fileSystem.DBWriter
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import kotlinx.coroutines.flow.Flow

class BackupSmsUseCase(
    private val fileSystemSource: FileSystemSource,
    private val smsDBWriter: DBWriter<DataItem<SmsListItem>>,
    private val dataRepository: BackupDataRepository,
) {
    operator fun invoke(
        file: GenericFile,
    ): Flow<Progress> {
        return fileSystemSource.writeDB(
            file = file,
            dbWriter = smsDBWriter,
            writerBlock = {
                it.writeRows(dataRepository.stagedSms)
            }
        )
    }
}