package balti.migrate;

import java.util.Vector;

/**
 * Created by sayantan on 9/10/17.
 */

public interface OnCheck {
    void onCheck(Vector<BackupDataPacket> backupDataPackets);
}
