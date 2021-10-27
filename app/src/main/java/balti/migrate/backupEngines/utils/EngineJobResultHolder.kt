package balti.migrate.backupEngines.utils

data class EngineJobResultHolder(
    val success: Boolean,
    val result: Any?,
    val errors: ArrayList<String> = arrayListOf(),
    val warnings: ArrayList<String> = arrayListOf(),
)