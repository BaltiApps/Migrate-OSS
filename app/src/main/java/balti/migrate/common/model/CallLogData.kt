package balti.migrate.common.model

import balti.migrate.backup.data.utils.convertToDisplayDate
import baltiapps.migrate.domain.backup.model.CallLogListItem
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.ItemCreationDate

data class CallLogData(
    override val _id: String,
    override val logInfo: String,

    val callsCachedName: String = "",
    val callsCountryIso: String = "",
    val callsDataUsage: Long,
    val callsFeatures: Int,
    val callsGeocodedLocation: String = "",
    val callsIsRead: Boolean,
    val callsNumber: String = "",
    val callsNumberPresentation: Int,
    val callsPhoneAccountComponentName: String = "",
    val callsPhoneAccountId: String = "",
    val callsTranscription: String = "",
    val callsType: Int,
    val callsVoicemailUri: String = "",
    val callsDate: Long,
    val callsDuration: Long,
    val callsNew: Boolean,
) : DataItem<CallLogListItem> {
    override fun toListItem(): CallLogListItem {
        return CallLogListItem(
            _id = _id,
            callStatus = callsType,
            displayName = callsCachedName,
            displayNumber = callsNumber,
            creationDate = ItemCreationDate(
                dateInLong = callsDate,
                displayDate = convertToDisplayDate(callsDate)
            ),
            isChecked = false,
        )
    }
}