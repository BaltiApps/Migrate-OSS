package balti.migrate.extraBackupsActivity.apps

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.AsyncTask
import android.support.v7.app.AlertDialog
import android.view.View
import balti.migrate.R
import balti.migrate.backupActivity.BackupDataPacketKotlin
import balti.migrate.extraBackupsActivity.OnJobCompletion
import balti.migrate.extraBackupsActivity.ViewOperations
import kotlinx.android.synthetic.main.please_wait.view.*
import java.util.*
import kotlin.collections.ArrayList

class MakeAppPackets(private val jobCode: Int, val context: Context,
                     private val appList: Vector<BackupDataPacketKotlin> = Vector(0)):
        AsyncTask<Any, String, Any>() {

    private val dialogView by lazy { View.inflate(context, R.layout.please_wait, null) }
    private val waitingDialog by lazy { AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private val vOp by lazy { ViewOperations(context) }
    private val appBatches by lazy { ArrayList<AppPacket>(0) }
    private val notificationManager by lazy { context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    init {
        dialogView.waiting_cancel.setOnClickListener {
            cancel(true)
            try { waitingDialog.dismiss() } catch (_: Exception){}
        }
        vOp.visibilitySet(dialogView.waiting_progress, View.GONE)
        vOp.visibilitySet(dialogView.waiting_details, View.GONE)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        waitingDialog.show()
        notificationManager.cancelAll()
        (context.filesDir.listFiles() + context.externalCacheDir.listFiles()).forEach {
            it.delete()
        }
    }

    override fun doInBackground(vararg params: Any?): Any? {

        return null

    }
}