package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.model.SmsListItem
import baltiapps.migrate.domain.backup.repository.DataRepository
import baltiapps.migrate.domain.backup.toListItems
import kotlinx.coroutines.flow.Flow

class ReadSmsUseCase(
    private val dataRepository: DataRepository,
) {
    suspend fun read(): Flow<Progress> {
        return dataRepository.readSmsFromDevice()
    }

    fun getReadSms(): List<SmsListItem> {
        return dataRepository.smsDataItems.toListItems()
            .sortedByDescending { it.creationDate.dateInLong }
    }
}