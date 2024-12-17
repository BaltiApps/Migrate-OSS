package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.model.Progress
import baltiapps.migrate.domain.backup.model.SmsListItem
import baltiapps.migrate.domain.backup.repository.PlatformDataRepository
import baltiapps.migrate.domain.backup.toListItems
import kotlinx.coroutines.flow.Flow

class ReadSmsUseCase(
    private val platformDataRepository: PlatformDataRepository,
) {
    suspend fun read(): Flow<Progress> {
        return platformDataRepository.readSmsFromDevice()
    }

    fun getReadSms(): List<SmsListItem> {
        return platformDataRepository.smsDataItems.toListItems()
            .sortedByDescending { it.creationDate.dateInLong }
    }
}