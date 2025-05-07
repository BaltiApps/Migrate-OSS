package baltiapps.migrate.domain.backup.model

data class BackupLocation(
    val backupName: String,
    val backupUriString: String? = null,
) {
}
