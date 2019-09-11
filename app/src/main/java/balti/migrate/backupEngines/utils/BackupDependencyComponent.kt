package balti.migrate.backupEngines.utils

import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [BackupServiceKotlin::class])
interface BackupDependencyComponent {
    fun masterInject(parentBackupClass: ParentBackupClass)
}