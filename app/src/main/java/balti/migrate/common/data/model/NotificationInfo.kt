package balti.migrate.common.data.model

data class NotificationInfo(
    val notificationId: Int,
    val notificationChannelId: String,
    val icon: Int,
    val title: String,
    val text: String? = null,
    val progress: Int = -1,
    val maxProgress: Int = -1,
    val isIndeterminate: Boolean = false,
    val shouldShowProgress: Boolean = false,
)
