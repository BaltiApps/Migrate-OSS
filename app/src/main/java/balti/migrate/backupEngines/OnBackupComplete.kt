package balti.migrate.backupEngines

interface OnBackupComplete {
    fun onBackupComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any?)
}