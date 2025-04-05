package baltiapps.migrate.domain.common.model

interface GenericFile {
    val path: String
    val name: String
    val exists: Boolean
    val canRead: Boolean
    val canWrite: Boolean
    val isFile: Boolean
}