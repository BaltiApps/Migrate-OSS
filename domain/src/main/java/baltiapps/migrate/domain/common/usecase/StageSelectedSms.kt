package baltiapps.migrate.domain.common.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.SmsListItem
import baltiapps.migrate.domain.common.repository.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StageSelectedSms {
    suspend operator fun invoke(
        allListItems: List<SmsListItem>,
        dataRepository: DataRepository,
    ) {
        withContext(Dispatchers.IO) {
            val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
            val selectedDataItems = dataRepository.smsDataItems.filter { it._id in selectedIds }
            dataRepository.stagedSms.clearAndAddAll(selectedDataItems)
        }
    }
}