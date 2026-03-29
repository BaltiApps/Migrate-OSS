package baltiapps.migrate.domain.common.usecase

import baltiapps.migrate.domain.common.model.AppSizeInfo

class GetRequiredSpaceUseCase {
    operator fun invoke(appSizes: List<AppSizeInfo>): Long {
        val maxSize = appSizes.maxOf { it.sizeInBytes }

        val sum = appSizes.sumOf { it.sizeInBytes }
        val bufferSpace = maxSize

        val requiredSpace = sum + bufferSpace

        return requiredSpace
    }
}