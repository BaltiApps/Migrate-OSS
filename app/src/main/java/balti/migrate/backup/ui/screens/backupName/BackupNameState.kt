package balti.migrate.backup.ui.screens.backupName

data class BackupNameState(
    val backupName: String,
    val safUriString: String? =  null,
    val isSafUriAccessible: Boolean? = null,
)
