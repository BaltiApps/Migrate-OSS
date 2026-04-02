package baltiapps.migrate.domain.common.model

data class AppSizeInfo(
    val packageName: String,
    val bytesApk: Long,
    val bytesData: Long,
) {
    val bytesTotal: Long get() = bytesApk + bytesData
}