package baltiapps.migrate.domain.restore.sources

interface InternalStorageSpaceReader {
    data class InternalStorageSpaceInfo(
        val bytesFree: Long,
        val bytesTotal: Long,
    )

    suspend fun getSpaceInfo(): InternalStorageSpaceInfo
}