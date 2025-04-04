package baltiapps.migrate.domain.exceptions

import kotlin.reflect.KClass

class UnknownDataTypeException(
    val type: KClass<*>,
    override val message: String = "Unknown data type: ${type::class.java}"
): Exception()