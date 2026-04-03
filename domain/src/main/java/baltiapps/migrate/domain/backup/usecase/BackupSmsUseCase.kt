package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.backup.sources.DataBackup
import baltiapps.migrate.domain.backup.sources.dataBackupRunner
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.GenericFile
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.common.model.SmsListItem
import kotlinx.coroutines.flow.Flow

class BackupSmsUseCase(
    private val smsDBWriter: DataBackup<DataItem<SmsListItem>>,
    private val dataRepository: BackupDataRepository,
) {
    operator fun invoke(
        file: GenericFile,
    ): Flow<Progress> {
        return dataBackupRunner(
            backupEngine = smsDBWriter,
            location = file,
            items = dataRepository.stagedSms
        )
    }
}