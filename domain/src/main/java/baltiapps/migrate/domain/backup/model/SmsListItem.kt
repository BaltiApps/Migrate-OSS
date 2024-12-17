package baltiapps.migrate.domain.backup.model

data class SmsListItem(
    override val _id: String,
    val smsAddress: String,
    val smsBody: String,
    val smsType: Int,
    val creationDate: ItemCreationDate,
    var isChecked: Boolean = false,
): ListItem
