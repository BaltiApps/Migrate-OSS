package baltiapps.migrate.domain.common.model

data class Progress(
    val progressType: ProgressType,
    val percentage: Double,
    val logs: String = "",
    val isFailure: Boolean = false,
    val isLogHeading: Boolean = false,
) {
    enum class ProgressType {
        STANDBY,
        CONTACTS_READ,
        CONTACTS_BACKUP,
        CALL_LOG_READ,
        CALL_LOG_BACKUP,
        SMS_READ,
        SMS_BACKUP,
        BACKUP_FINISHED,
        BACKUP_FINISHED_WITH_ERRORS,
        BACKUP_CANCELLED,
        CALL_LOG_BACKUP_READ,
        SMS_BACKUP_READ,
        CALL_LOG_RESTORE,
        SMS_RESTORE,
    }

    private val backupFinishedTypes = setOf(
        ProgressType.BACKUP_FINISHED,
        ProgressType.BACKUP_FINISHED_WITH_ERRORS,
        ProgressType.BACKUP_CANCELLED,
    )

    fun isBackupFinished(): Boolean {
        return progressType in backupFinishedTypes
    }

    companion object {
        val Empty = Progress(
            progressType = ProgressType.STANDBY,
            percentage = 0.0,
            logs = ""
        )
    }
}

