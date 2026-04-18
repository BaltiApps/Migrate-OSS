package baltiapps.migrate.domain.common.model

data class AppSizeInfo(
    val packageName: String,
    val bytesApk: Long,
    val bytesData: Long,
    val bytesExternalData: Long,
    val bytesExternalMedia: Long,

    override val _id: String = packageName,
    override val logInfo: String = packageName,
): DataItem<Nothing> {
    val bytesTotal: Long get() = bytesApk + bytesData + bytesExternalData + bytesExternalMedia

    override fun toListItem(): Nothing {
        throw UnsupportedOperationException("AppSizeInfo does not support toListItem()")
    }
}