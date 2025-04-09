package baltiapps.migrate.domain.common.usecase

import baltiapps.migrate.domain.common.clearAndAddAll
import baltiapps.migrate.domain.common.model.SmsListItem
import baltiapps.migrate.domain.common.repository.DataRepository

class StageSelectedSms {
    operator fun invoke(
        allListItems: List<SmsListItem>,
        dataRepository: DataRepository,
    ) {
        val selectedIds = allListItems.filter { it.isChecked }.map { it._id }
        val selectedDataItems = dataRepository.smsDataItems.filter { it._id in selectedIds }
        dataRepository.stagedSms.clearAndAddAll(selectedDataItems)
    }
}