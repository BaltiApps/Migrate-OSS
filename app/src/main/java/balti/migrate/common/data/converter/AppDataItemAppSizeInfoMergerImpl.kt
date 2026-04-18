package balti.migrate.common.data.converter

import balti.migrate.common.data.model.AppData
import baltiapps.migrate.domain.common.converter.AppDataItemAppSizeInfoMerger
import baltiapps.migrate.domain.common.model.AppSizeInfo

class AppDataItemAppSizeInfoMergerImpl: AppDataItemAppSizeInfoMerger<AppData> {
    override fun merge(
        appDataItem: AppData,
        appSizeInfo: AppSizeInfo?
    ): AppData {
        return appDataItem.copy(
            apkSizeBytes = appSizeInfo?.bytesApk ?: 0L,
            dataSizeBytes = appSizeInfo?.bytesData ?: 0L,
            externalDataBytes = appSizeInfo?.bytesExternalData ?: 0L,
            externalMediaBytes = appSizeInfo?.bytesExternalMedia ?: 0L,
        )
    }
}
