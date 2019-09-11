package balti.migrate.backupEngines.utils

import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.engines.*
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [BackupServiceKotlin::class])
interface BackupDependencyComponent {
    fun inject(appBackupEngine: AppBackupEngine)
    fun inject(verificationEngine: VerificationEngine)
    fun inject(zippingEngine: ZippingEngine)
    fun inject(zipVerificationEngine: ZipVerificationEngine)
    fun masterInject(parentBackupClass: ParentBackupClass)
}