package balti.migrate;

import java.util.Vector;

public class BackupBatch{
    Vector<BackupDataPacketWithSize> appListWithSize;
    long batchSize;
    long batchSystemSize, batchDataSize;

    public BackupBatch(Vector<BackupDataPacketWithSize> appListWithSize, long batchSystemSize, long batchDataSize) {
        this.appListWithSize = appListWithSize;
        this.batchDataSize = batchDataSize;
        this.batchSystemSize = batchSystemSize;

        batchSize = batchDataSize + batchSystemSize;
    }
}