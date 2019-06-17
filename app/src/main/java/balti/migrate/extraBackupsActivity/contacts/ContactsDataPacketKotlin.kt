package balti.migrate.extraBackupsActivity.contacts

data class ContactsDataPacketKotlin (val fullName: String, val vcfData: String) {
    constructor(arr: Array<String>) : this(arr[0], arr[1])
    var selected = false
}