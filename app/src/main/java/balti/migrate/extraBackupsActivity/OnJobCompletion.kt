package balti.migrate.extraBackupsActivity

interface OnJobCompletion {
    fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any?)
}