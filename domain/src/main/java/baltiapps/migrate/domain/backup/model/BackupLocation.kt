package baltiapps.migrate.domain.backup.model

data class BackupLocation(
    val backupName: String,
    val backupLocation: String
) {
    fun getFullPath(): String {
        return "$backupLocation/$backupName"
    }
}
