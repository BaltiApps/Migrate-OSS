package balti.migrate.common.data.converter

import balti.migrate.common.data.model.AppData
import baltiapps.migrate.domain.common.converter.AppDataItemAppSizeInfoExtractor
import baltiapps.migrate.domain.common.model.AppSizeInfo

class AppDataItemAppSizeInfoExtractorImpl: AppDataItemAppSizeInfoExtractor<AppData> {
    override fun extract(appDataItem: AppData): AppSizeInfo {
        return AppSizeInfo(
            packageName = appDataItem.packageName,
            bytesApk = appDataItem.apkSizeBytes.takeIf { appDataItem.shouldBackupApk } ?: 0L,
            bytesData = appDataItem.dataSizeBytes.takeIf { appDataItem.shouldBackupData } ?: 0L,
            bytesExternalDataMedia = appDataItem.externalDataMediaBytes.takeIf {
                appDataItem.shouldBackupExternalData
            } ?: 0L
        )
    }
}
