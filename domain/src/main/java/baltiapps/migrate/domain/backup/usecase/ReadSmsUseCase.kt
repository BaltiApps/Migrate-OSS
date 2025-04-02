package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.clearAndAddAll
import baltiapps.migrate.domain.backup.model.DataItem
import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.model.SmsListItem
import baltiapps.migrate.domain.backup.repository.DataRepository
import baltiapps.migrate.domain.backup.sources.DataSource
import kotlinx.coroutines.flow.Flow

class ReadSmsUseCase(
    private val smsSource: DataSource<DataItem<SmsListItem>>,
    private val dataRepository: DataRepository,
) {
    suspend fun invoke(): Flow<Progress> {
        return smsSource.getData {
            dataRepository.smsDataItems.clearAndAddAll(it)
        }
    }
}