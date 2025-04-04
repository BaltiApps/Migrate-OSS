package balti.migrate.restore.data.sources.sms.dummies

import android.app.Service
import android.content.Intent
import android.os.IBinder

class DummySmsSendService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        stopSelf()
    }
}
