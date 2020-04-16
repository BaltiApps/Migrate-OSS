package balti.migrate.extraBackupsActivity.utils

interface OnJobCompletion {
    fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any?)
}