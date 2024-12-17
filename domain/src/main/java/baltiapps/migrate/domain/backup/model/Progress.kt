package baltiapps.migrate.domain.backup.model

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
    }

    private val backupFinishedTypes = setOf(
        Progress.ProgressType.BACKUP_FINISHED,
        Progress.ProgressType.BACKUP_FINISHED_WITH_ERRORS,
        Progress.ProgressType.BACKUP_CANCELLED,
    )

    fun isFinished(): Boolean {
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

