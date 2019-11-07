package balti.migrate.backupEngines.containers

data class ZipAppBatch(val zipPackets: ArrayList<ZipAppPacket>) {
    var batchSystemSize : Long = 0
    var batchDataSize : Long = 0
    var zipFullSize : Long = 0
    init {
        zipPackets.forEach {
            batchDataSize += it.appPacket_z.dataSize
            batchSystemSize += it.appPacket_z.systemSize
            zipFullSize += it.zipPacketSize
        }
    }
}