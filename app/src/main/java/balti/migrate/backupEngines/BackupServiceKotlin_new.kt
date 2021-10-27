package balti.migrate.backupEngines

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BackupServiceKotlin_new: Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}