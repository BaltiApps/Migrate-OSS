package balti.migrate.backupEngines

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BackupServiceKotlin_new: Service() {

    companion object {

        var fileXDestination: String? = null
        var cancelBackup: Boolean = false

    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fileXDestination = null
    }
}