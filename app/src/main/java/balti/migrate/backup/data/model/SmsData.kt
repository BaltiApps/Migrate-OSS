package balti.migrate.backup.data.model

import balti.migrate.backup.data.utils.convertToDisplayDate
import baltiapps.migrate.domain.backup.model.DataItem
import baltiapps.migrate.domain.backup.model.ItemCreationDate
import baltiapps.migrate.domain.backup.model.SmsListItem

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
