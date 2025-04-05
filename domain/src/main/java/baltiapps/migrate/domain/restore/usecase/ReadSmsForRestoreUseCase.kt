package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.model.SmsListItem
import baltiapps.migrate.domain.common.sources.fileSystem.DBReader
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class ReadSmsForRestoreUseCase(
    private val fileSystemSource: FileSystemSource,
    private val smsDbReader: DBReader<DataItem<SmsListItem>>,
    private val restoreDataRepository: RestoreDataRepository,
) {
    operator fun invoke(): Flow<Progress> {
        return restoreDataRepository.run {
            val smsLogFile = this.getSmsBackupFile() ?: return emptyFlow()
            fileSystemSource.readDB(smsLogFile, smsDbReader) { reader ->
                reader.readRows { result ->
                    this.smsDataItems.clearAndAddAll(result)
                }
            }
        }
    }
}