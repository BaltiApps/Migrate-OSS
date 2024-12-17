package balti.migrate.backup.data.model

import baltiapps.migrate.domain.backup.model.ContactListItem
import baltiapps.migrate.domain.backup.model.DataItem

data class ContactData(
    override val _id: String,
    val displayName: String,
    val displayNumber: String,
    val vcfContent: String,
    override val logInfo: String,
) : DataItem<ContactListItem> {
    override fun toListItem(): ContactListItem {
        return ContactListItem(
            _id = _id,
            displayName = displayName,
            displayNumber = displayNumber,
            isChecked = false,
        )
    }
}
