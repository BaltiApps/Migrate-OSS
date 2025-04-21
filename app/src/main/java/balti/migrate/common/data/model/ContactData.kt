package balti.migrate.common.data.model

import baltiapps.migrate.domain.common.model.ContactListItem
import baltiapps.migrate.domain.common.model.DataItem

data class ContactData(
    override val _id: String,
    val displayName: String,
    val vcfContent: String,
    val isLocalContact : Boolean,
    override val logInfo: String,
) : DataItem<ContactListItem> {
    override fun toListItem(): ContactListItem {
        return ContactListItem(
            _id = _id,
            displayName = displayName,
            isLocalContact = isLocalContact,
            isChecked = true,
        )
    }
}
