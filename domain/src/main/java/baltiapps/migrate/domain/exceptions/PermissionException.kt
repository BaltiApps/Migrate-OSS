package baltiapps.migrate.domain.exceptions

class PermissionException(
    val permissionName: String,
    override val message: String?
): Exception()