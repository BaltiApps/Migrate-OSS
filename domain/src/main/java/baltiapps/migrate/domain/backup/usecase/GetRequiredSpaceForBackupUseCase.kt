package baltiapps.migrate.domain.backup.usecase

import baltiapps.migrate.domain.backup.repository.BackupDataRepository

class GetRequiredSpaceForBackupUseCase(
    private val backupDataRepository: BackupDataRepository,
) {
    operator fun invoke(): Long {
        val appSizes = backupDataRepository.stagedAppSizes
        val maxSize = appSizes.maxOf { it.bytesTotal }

        val sum = appSizes.sumOf { it.bytesTotal }
        val bufferSpace = maxSize

        val requiredSpace = sum + bufferSpace

        return requiredSpace
    }
}