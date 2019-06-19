package balti.migrate.extraBackupsActivity.apps

class AppBatch(val appPackets: ArrayList<AppPacket>) {

    var dataSize = 0L
    var systemSize = 0L

    init {
        for (ap in appPackets){
            dataSize += ap.dataSize
            systemSize += ap.systemSize
        }
    }
}