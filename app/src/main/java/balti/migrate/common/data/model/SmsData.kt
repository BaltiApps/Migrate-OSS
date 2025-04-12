package balti.migrate.common.data.model

import balti.migrate.common.utils.convertToDisplayDate
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.common.model.ItemCreationDate
import baltiapps.migrate.domain.common.model.SmsListItem

data class SmsData(
    override val _id: String,
    override val logInfo: String,

    val smsAddress: String,
    val smsBody: String,
    val smsDate: Long,
    val smsDateSent: Long,
    val smsType: Int,
    val smsPerson: String,
    val smsProtocol: Int,
    val smsSeen: Boolean,
    val smsServiceCenter: String,
    val smsStatus: Int,
    val smsSubject: String,
    val smsThreadID: Int,
    val smsErrorCode: Int,
    val smsRead: Boolean,
    val smsLocked: Boolean,
    val smsReplyPathPresent: Boolean,
): DataItem<SmsListItem> {
    override fun toListItem(): SmsListItem {
        return SmsListItem(
            _id = _id,
            smsAddress = smsAddress,
            smsBody = smsBody,
            smsType = smsType,
            creationDate = ItemCreationDate(
                dateInLong = smsDate,
                displayDate = convertToDisplayDate(smsDate)
            ),
        )
    }
}
