package balti.migrate.backup.ui.screens.backupName

data class BackupNameState(
    val backupName: String,
    val isSaf: Boolean?,
    val safUriString: String?,
    val locationString: String,
    val isSafUriAccessible: Boolean,
)
