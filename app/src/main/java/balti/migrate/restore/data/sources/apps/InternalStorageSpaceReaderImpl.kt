package balti.migrate.restore.data.sources.apps

import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.restore.sources.InternalStorageSpaceReader
import timber.log.Timber

class InternalStorageSpaceReaderImpl(
    private val superuserUtils: SuperuserUtils,
): InternalStorageSpaceReader {
    override suspend fun getSpaceInfo(): InternalStorageSpaceReader.InternalStorageSpaceInfo {
        val suShell = superuserUtils.getSuperuserShell()

        var spaceInfo = InternalStorageSpaceReader.InternalStorageSpaceInfo(0L, 0L)

        superuserUtils.runCommand(
            command = "df /data",
            parentSuperuserShell = suShell,
            onFinish = { success, message ->
                Timber.d("Success - $success, message - $message")
                if (success) {
                    val parts = message.trim().lines()
                        .lastOrNull()?.trim()?.split("\\s+".toRegex())
                    val bytesTotal = parts?.getOrNull(1)?.toLongOrNull()?.times(1024) ?: 0L
                    val bytesFree = parts?.getOrNull(3)?.toLongOrNull()?.times(1024) ?: 0L
                    spaceInfo = InternalStorageSpaceReader.InternalStorageSpaceInfo(
                        bytesFree = bytesFree,
                        bytesTotal = bytesTotal,
                    )
                }
            }
        )

        return spaceInfo
    }
}