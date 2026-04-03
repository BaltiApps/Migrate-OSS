package baltiapps.migrate.domain.restore.usecase

import baltiapps.migrate.domain.common.converter.AppDataItemAppSizeInfoExtractor
import baltiapps.migrate.domain.common.model.AppListItem
import baltiapps.migrate.domain.common.model.AppSizeInfo
import baltiapps.migrate.domain.common.model.DataItem
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository

class GetRequiredSpaceForRestoreUseCase(
    private val dataRestore: RestoreDataRepository,
    private val appSizeInfoExtractor: AppDataItemAppSizeInfoExtractor<DataItem<AppListItem>>,
) {
    operator fun invoke(): Long {

        val appSizeInfos: List<AppSizeInfo> =
            dataRestore.stagedApps.map { appSizeInfoExtractor.extract(it) }

        val totalBytes = appSizeInfos.sumOf { it.bytesTotal }

        val biggestApkBytes = appSizeInfos.maxOf { it.bytesApk }

        val bufferSpace = appSizeInfos.maxOf { it.bytesTotal }

        return totalBytes +
                biggestApkBytes +  // each app's APKs are copied to /data/local/tmp before restore
                bufferSpace
    }
}