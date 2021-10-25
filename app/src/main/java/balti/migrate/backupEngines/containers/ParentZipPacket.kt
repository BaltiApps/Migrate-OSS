package balti.migrate.backupEngines.containers

abstract class ParentZipPacket {
    abstract val displayName: String
    abstract var zipPacketSize: Long
    abstract fun refreshTotal()
}