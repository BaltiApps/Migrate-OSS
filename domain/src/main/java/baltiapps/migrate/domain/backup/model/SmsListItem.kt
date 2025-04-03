package baltiapps.migrate.domain.backup.model

import baltiapps.migrate.domain.common.model.ItemCreationDate
import baltiapps.migrate.domain.common.model.ListItem

data class SmsListItem(
    override val _id: String,
    val smsAddress: String,
    val smsBody: String,
    val smsType: Int,
    val creationDate: ItemCreationDate,
    var isChecked: Boolean = false,
): ListItem
