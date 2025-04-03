package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.BACKUP_FILE_NAME_SMS
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.Progress
import baltiapps.migrate.domain.backup.model.SmsListItem
import baltiapps.migrate.domain.common.sources.fileSystem.DBWriter
import baltiapps.migrate.domain.common.sources.fileSystem.FileSystemSource
import baltiapps.migrate.domain.backup.tryPerformWrite
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class BackupSmsUseCase(
    private val fileSystemSource: FileSystemSource,
    private val smsDBWriter: DBWriter<DataItem<SmsListItem>>,
) {
    operator fun invoke(
        backupRoot: String,
        stagedSms: List<DataItem<SmsListItem>>
    ): Flow<Progress> {
        return flow {
            if (stagedSms.isEmpty()) return@flow
            fileSystemSource.writeDB(
                directory = backupRoot,
                fileName = BACKUP_FILE_NAME_SMS,
                dbWriter = smsDBWriter,
            ) { dbWriter ->
                tryPerformWrite(
                    items = stagedSms,
                    progressType = Progress.ProgressType.SMS_BACKUP,
                ) { item ->
                    dbWriter.writeRow(item)
                }
            }
        }.flowOn(IO)
    }
}