package balti.migrate.backupEngines.containers

data class BackupIntentData(val backupName: String, val destination: String){
    var batchErrorTag = ""
    private set
    fun setErrorTag(tag: String) {
        batchErrorTag = tag
    }
}