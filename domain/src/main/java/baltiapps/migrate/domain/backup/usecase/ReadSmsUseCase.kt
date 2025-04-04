package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.backup.model.SmsListItem
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.DataSource
import kotlinx.coroutines.flow.Flow

class ReadSmsUseCase(
    private val smsSource: DataSource<DataItem<SmsListItem>>,
    private val backupDataRepository: BackupDataRepository,
) {
    suspend fun invoke(): Flow<Progress> {
        return smsSource.getData {
            backupDataRepository.smsDataItems.clearAndAddAll(it)
        }
    }
}