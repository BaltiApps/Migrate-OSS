package balti.migrate;

import java.util.Vector;

public class BackupBatch {
    Vector<BackupDataPacketWithSize> appListWithSize;
    int appCount;
    long batchSystemSize, batchDataSize;

    public BackupBatch(Vector<BackupDataPacketWithSize> appListWithSize, long batchDataSize, long batchSystemSize) {
        this.appListWithSize = appListWithSize;
        this.batchDataSize = batchDataSize;
        this.batchSystemSize = batchSystemSize;
        appCount = appListWithSize.size();
    }
}