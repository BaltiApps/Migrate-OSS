package baltiapps.migrate.domain.common.model

data class Directory(
    val directoryFullPath: String,
    val basePath: String,
    val name: String,
    val parent: Directory?,
    val isValidBackupDirectory: Boolean,
)