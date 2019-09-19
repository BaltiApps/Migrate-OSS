package balti.migrate.backupEngines.utils

interface OnBackupComplete {
    fun onBackupComplete(jobCode: Int, jobSuccess: Boolean, jobResults: ArrayList<String>?)
}