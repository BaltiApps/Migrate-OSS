package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.model.SmsListItem
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import baltiapps.migrate.domain.restore.sources.RestoreReader
import baltiapps.migrate.domain.restore.sources.restoreReaderRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class ReadSmsForRestoreUseCase(
    private val smsRestoreReader: RestoreReader<DataItem<SmsListItem>>,
    private val restoreDataRepository: RestoreDataRepository,
) {
    operator fun invoke(): Flow<Progress> {
        return restoreDataRepository.run {
            val smsLogFile = this.getSmsBackupFile() ?: return emptyFlow()

            restoreReaderRunner(
                file = smsLogFile,
                restoreReader = smsRestoreReader,
                onItemsRead = { dataItems ->
                    this.smsDataItems.clearAndAddAll(dataItems)
                }
            )
        }
    }
}