package balti.migrate;

public class BackupDataPacketWithSize {
    BackupDataPacket packet;
    long dataSize;
    long systemSize;
    long totalSize;

    public BackupDataPacketWithSize(BackupDataPacket packet, long dataSize, long systemSize) {
        this.packet = packet;
        this.dataSize = dataSize;
        this.systemSize = systemSize;

        totalSize = dataSize + systemSize;
    }
}
