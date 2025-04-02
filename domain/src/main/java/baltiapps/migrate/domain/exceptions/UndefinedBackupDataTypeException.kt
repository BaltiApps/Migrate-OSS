package baltiapps.migrate.domain.exceptions

import kotlin.reflect.KClass

class UndefinedBackupDataTypeException(
    val type: KClass<*>,
    override val message: String = "Unknown backup data type: ${type::class.java}"
): Exception()