package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.model.SmsListItem
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.sources.DataRestore
import kotlinx.coroutines.flow.Flow

class RestoreSmsUseCase(
    private val dataRestore: DataRestore<DataItem<SmsListItem>>,
    private val restoreDataRepository: RestoreDataRepository,
) {
    operator fun invoke(): Flow<Progress> {
        return dataRestore.restoreDataItems(restoreDataRepository.smsDataItems)
    }
}