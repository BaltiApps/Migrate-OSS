package balti.migrate.ng.backupEngines.utils

interface OnBackupComplete {
    fun onBackupComplete(jobCode: Int, jobSuccess: Boolean, jobResults: ArrayList<String>?)
}