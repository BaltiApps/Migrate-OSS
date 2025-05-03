package baltiapps.migrate.domain.common.model

interface GenericFile {
    val path: String  // includes name
    val name: String
        get() = path.trimEnd('/').substringAfterLast("/")
    val subDirectoryPath: String
        get() = path.removeSuffix(name).trimEnd('/')
    val isValidBackupDirectory: Boolean
        get() = false
}