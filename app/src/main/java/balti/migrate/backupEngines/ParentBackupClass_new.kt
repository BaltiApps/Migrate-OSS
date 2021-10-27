package balti.migrate.backupEngines

import balti.migrate.utilities.CommonToolsKotlin
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask

abstract class ParentBackupClass_new: AsyncCoroutineTask(DISP_IO) {

    val engineContext by lazy { BackupServiceKotlin.serviceContext }
    val commonTools by lazy { CommonToolsKotlin(engineContext) }

}