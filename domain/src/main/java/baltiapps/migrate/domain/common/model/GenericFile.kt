package baltiapps.migrate.domain.common.model

interface GenericFile {
    val path: String  // includes name
    val name: String
        get() = path.removeSuffix("/").substringAfterLast("/")
    val subDirectoryPath: String
        get() = path.removeSuffix(name).removeSuffix("/")
}