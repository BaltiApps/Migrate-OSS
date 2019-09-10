package balti.migrate.backupEngines.containers

data class BackupIntentData(val backupName: String, val destination: String, val partNumber: Int, val totalParts: Int){
    val errorTag by lazy { "[${partNumber}/${totalParts}]" }
}