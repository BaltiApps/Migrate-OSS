package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.common.model.AppSizeInfo

class GetRequiredSpaceForBackupUseCase {
    operator fun invoke(appSizes: List<AppSizeInfo>): Long {
        val maxSize = appSizes.maxOf { it.bytesTotal }

        val sum = appSizes.sumOf { it.bytesTotal }
        val bufferSpace = maxSize

        val requiredSpace = sum + bufferSpace

        return requiredSpace
    }
}