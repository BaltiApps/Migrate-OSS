package balti.migrate.extraBackupsActivity.contacts.containers

data class ContactsDataPacketKotlin (val fullName: String, val vcfData: String, var selected: Boolean) {
    constructor(arr: Array<String>, selected: Boolean) : this(arr[0], arr[1], selected)
}