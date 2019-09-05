package balti.migrate.backupEngines

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [BackupServiceKotlin::class])
interface BackupDependencyComponent {
    fun inject(appBackupEngine: AppBackupEngine)
    fun inject(verificationEngine: VerificationEngine)
}