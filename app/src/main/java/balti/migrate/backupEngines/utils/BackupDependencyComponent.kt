package balti.migrate.backupEngines.utils

import balti.migrate.backupEngines.*
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [BackupServiceKotlin::class])
interface BackupDependencyComponent {
    fun inject(appBackupEngine: AppBackupEngine)
    fun inject(verificationEngine: VerificationEngine)
    fun inject(zippingEngine: ZippingEngine)
    fun inject(zipVerificationEngine: ZipVerificationEngine)
}