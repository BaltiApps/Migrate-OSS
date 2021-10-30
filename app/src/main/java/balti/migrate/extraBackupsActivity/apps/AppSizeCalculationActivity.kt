package balti.migrate.extraBackupsActivity.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.AppInstance
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.AppInstance.Companion.appPackets
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_CANONICAL_DESTINATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FILEX_DESTINATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FLASHER_ONLY
import balti.module.baltitoolbox.functions.Misc
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.please_wait.*
import kotlinx.coroutines.delay

class AppSizeCalculationActivity: AppCompatActivity(R.layout.please_wait) {

    private var readTask: MakeAppPackets? = null

    private var canonicalDestination: String = ""
    private var fileXDestination: String = ""
    private var backupName: String = ""
    private var flasherOnly: Boolean = false

    private val commonTools by lazy { CommonToolsKotlin(this) }

    private val progressReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                // Unregister yourself to not get re-triggered.
                commonTools.LBM?.unregisterReceiver(this)

                // Wait 2 seconds before closing else the animation is very jarring.
                Misc.runSuspendFunction {
                    delay(2000)
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        canonicalDestination = intent.getStringExtra(EXTRA_CANONICAL_DESTINATION) ?: DEFAULT_INTERNAL_STORAGE_DIR
        fileXDestination = intent.getStringExtra(EXTRA_FILEX_DESTINATION) ?: if (FileXInit.isTraditional) canonicalDestination else ""
        backupName = intent.getStringExtra(EXTRA_BACKUP_NAME) ?: "No name backup"
        flasherOnly = intent.getBooleanExtra(EXTRA_FLASHER_ONLY, false)

        AppInstance.notificationManager.cancelAll()

        readTask = MakeAppPackets(canonicalDestination, flasherOnly, this)

        /**
         * Delete all cached files from previous backup.
         */
        val allCacheFiles = ArrayList<FileX>(0)
        filesDir.listFiles()?.map { FileX.new(it.canonicalPath, true) }?.let { allCacheFiles.addAll(it)}
        externalCacheDir?.listFiles()?.map { FileX.new(it.canonicalPath, true) }?.let { allCacheFiles.addAll(it)}
        FileX.new(CACHE_DIR, true).listFiles()?.let { allCacheFiles.addAll(it)}

        allCacheFiles.forEach {
            it.delete()
        }

        /**
         * Make cancel button operational
         */
        waiting_cancel.apply {
            visibility = View.VISIBLE
            setText(android.R.string.cancel)
            val cancellingText = getString(R.string.cancelling)

            setOnClickListener {
                readTask?.cancelThis = true
                text = cancellingText
                Toast.makeText(context, R.string.long_press_to_force_stop, Toast.LENGTH_SHORT).show()
            }
            setOnLongClickListener {
                if (text == cancellingText) commonTools.forceCloseThis()
                true
            }
        }

        waiting_progress_text.visibility = View.GONE
        waiting_progress_subtext.visibility = View.GONE

        /**
         * Register receiver to close activity.
         */
        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_BACKUP_PROGRESS))

        readTask?.execute()
    }

    override fun onBackPressed() {}

    fun updateWaitingLayout(vararg values: Any){
        waiting_progress_text.visibility = View.VISIBLE
        waiting_progress_subtext.visibility = View.VISIBLE
        values.run {
            0.let {if (size > it) waiting_head.text = this[it].toString().trim() }
            1.let {if (size > it) waiting_progress_text.text = this[it].toString().trim() }
            2.let {if (size > it) waiting_progress_subtext.text = this[it].toString().trim() }
        }
    }

    fun onFinishReading(results: Array<*>){

        if (readTask?.cancelThis == true) finish()

        try {

            waiting_cancel.visibility = View.GONE
            val success = results[0] as Boolean

            if (success) {
                onSuccessfulReading(results[1] as ArrayList<AppPacket>)
            }
            else {
                onFailureReading(results[1].toString(), results[2].toString())
            }
        } catch (e: Exception) {
            Misc.showErrorDialog(
                e.message.toString(),
                getString(R.string.error_occurred),
                this,
            )
        }
    }

    private fun onFailureReading(title: String, message: String){
        val errorDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.close){_, _ ->
                finish()
            }
            .create()

        if (title == getString(R.string.insufficient_storage))
            errorDialog.setIcon(R.drawable.ic_zipping_icon)
        else errorDialog.setIcon(R.drawable.ic_cancelled_icon)

        errorDialog.show()
    }

    private fun onSuccessfulReading(results: ArrayList<AppPacket>){
        appPackets.apply {
            clear()
            addAll(results)
        }

        waiting_head.setText(R.string.just_a_minute)
        waiting_progress_text.setText(R.string.starting_engine)
        waiting_progress_subtext.text = ""

        Intent(this, BackupServiceKotlin::class.java).apply {
            putExtra(EXTRA_CANONICAL_DESTINATION, canonicalDestination)
            putExtra(EXTRA_FILEX_DESTINATION, fileXDestination)
            putExtra(EXTRA_BACKUP_NAME, backupName)
            putExtra(EXTRA_FLASHER_ONLY, flasherOnly)
        }.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(this)
            } else {
                startService(this)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
    }
}