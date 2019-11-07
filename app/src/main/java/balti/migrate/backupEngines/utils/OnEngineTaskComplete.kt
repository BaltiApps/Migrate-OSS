package balti.migrate.backupEngines.utils

interface OnEngineTaskComplete {
    fun onComplete(jobCode: Int, jobErrors: ArrayList<String> = ArrayList(0),
                   jobWarnings: ArrayList<String> = ArrayList(0), jobResults: Any? = null)
}