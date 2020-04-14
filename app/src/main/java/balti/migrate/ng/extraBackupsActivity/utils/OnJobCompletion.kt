package balti.migrate.ng.extraBackupsActivity.utils

interface OnJobCompletion {
    fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any?)
}