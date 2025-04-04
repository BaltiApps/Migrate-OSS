package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.backup.model.SmsListItem
import baltiapps.migrate.domain.backup.repository.BackupDataRepository

class StageSelectedSms(
    private val backupDataRepository: BackupDataRepository,
) {
    operator fun invoke(
        allListItems: List<SmsListItem>
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        val selectedDataItems = backupDataRepository.smsDataItems.filter { it._id in selectedIds }
        backupDataRepository.stagedSms.clearAndAddAll(selectedDataItems)
    }
}