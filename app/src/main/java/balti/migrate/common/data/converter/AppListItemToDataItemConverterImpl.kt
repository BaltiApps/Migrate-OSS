package balti.migrate.common.data.converter

import balti.migrate.common.data.model.AppData
import balti.migrate.common.data.model.AppIcon
import baltiapps.migrate.domain.common.converter.AppListItemToDataItemConverter
import baltiapps.migrate.domain.common.model.AppListItem

class AppListItemToDataItemConverterImpl(): AppListItemToDataItemConverter<AppData> {
    override fun convertSelection(
        listItem: AppListItem,
        dataItem: AppData
    ): AppData {
        return dataItem.copy(
            shouldBackupApk = listItem.isApkSelected,
            shouldBackupData = listItem.isDataSelected,
            shouldBackupPermissions = listItem.isPermissionsSelected,
        )
    }

    override fun setIcon(listItem: AppListItem, dataItem: AppData): AppData {
        val icon = listItem.appIcon

        return if (icon is AppIcon) {
            dataItem.copy(
                appIcon = icon.drawable
            )
        } else dataItem
    }
}