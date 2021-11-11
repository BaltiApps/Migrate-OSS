package balti.migrate.backupEngines.utils

import balti.migrate.backupEngines.BackupServiceKotlin_new

data class EngineJobResultHolder(
    val success: Boolean,
    val result: Any?,
    val errors: ArrayList<String> = arrayListOf(),
    val warnings: ArrayList<String> = arrayListOf(),
){
    /**
     * Will be true if [success] = `true` AND [result] is not null.
     * Used in [BackupServiceKotlin_new.startBackup].
     * To check if result is a success and perform operations on non-null result.
     */
    val successResultNotNull: Boolean = success && (result != null)
}