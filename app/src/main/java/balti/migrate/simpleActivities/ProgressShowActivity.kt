package balti.migrate.simpleActivities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import balti.migrate.R
import balti.migrate.backupEngines.engines.AppBackupEngine
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_CANCEL
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_ACTUAL_DESTINATION
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_ERRORS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_IS_CANCELLED
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_APP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_CALLS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_CONTACTS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_CORRECTING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_FINISHED
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_SETTINGS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_SMS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_TESTING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_UPDATER_SCRIPT
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_VERIFYING
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_WIFI
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_SUBTASK
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TASKLOG
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TOTAL_PARTS
import balti.migrate.utilities.CommonToolKotlin.Companion.EXTRA_TOTAL_TIME
import balti.migrate.utilities.CommonToolKotlin.Companion.TIMEOUT_WAITING_TO_KILL
import balti.migrate.utilities.IconTools
import kotlinx.android.synthetic.main.backup_progress_layout.*
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter

class ProgressShowActivity: AppCompatActivity() {

    private val commonTools by lazy { CommonToolKotlin(this) }
    private val iconTools by lazy { IconTools() }
    private val errors by lazy { ArrayList<String>(0) }

    private var lastLog = ""
    private var lastIcon = ""

    private var lastTitle = ""

    private var forceStopDialog: AlertDialog? = null

    private fun setImageIcon(intent: Intent, type: String){

        if (type == EXTRA_PROGRESS_TYPE_APP_PROGRESS){
            try {
                if (lastIcon != AppBackupEngine.ICON_STRING) {
                    AppBackupEngine.ICON_STRING.run {
                        if (this.contains(".icon", true)){
                            val iconFile = File(intent.getStringExtra(EXTRA_ACTUAL_DESTINATION), this.trim())
                            iconTools.setIconFromFile(app_icon, iconFile)
                        }
                        else iconTools.setIconFromIconString(app_icon, this)
                        lastIcon = this
                    }
                }
            }
            catch (_: Exception){
                app_icon.setImageResource(R.drawable.ic_save_icon)
            }
        }
        else if (type == EXTRA_PROGRESS_TYPE_FINISHED){

            val isCancelled = intent.getBooleanExtra(EXTRA_IS_CANCELLED, false)

            app_icon.setImageResource(
                    when {
                        isCancelled -> R.drawable.ic_cancelled_icon
                        errors.size > 0 -> R.drawable.ic_error
                        else -> R.drawable.ic_finished_icon
                    }
            )
            if (errors.size != 0 || isCancelled) app_icon.setColorFilter(
                    ContextCompat.getColor(this@ProgressShowActivity, R.color.error_color),
                    android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
        else app_icon.setImageResource(
                when (type){

                    EXTRA_PROGRESS_TYPE_TESTING -> R.drawable.ic_system_testing_icon

                    EXTRA_PROGRESS_TYPE_CONTACTS -> R.drawable.ic_contacts_icon
                    EXTRA_PROGRESS_TYPE_SMS -> R.drawable.ic_sms_icon
                    EXTRA_PROGRESS_TYPE_CALLS -> R.drawable.ic_call_log_icon
                    EXTRA_PROGRESS_TYPE_WIFI -> R.drawable.ic_wifi_icon
                    EXTRA_PROGRESS_TYPE_SETTINGS -> R.drawable.ic_settings_icon

                    EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS -> R.drawable.ic_app_scripts_icon

                    EXTRA_PROGRESS_TYPE_VERIFYING -> R.drawable.ic_verify_icon
                    EXTRA_PROGRESS_TYPE_CORRECTING -> R.drawable.ic_correcting_icon

                    EXTRA_PROGRESS_TYPE_UPDATER_SCRIPT -> R.drawable.ic_updater_script_icon

                    EXTRA_PROGRESS_TYPE_ZIP_PROGRESS -> R.drawable.ic_zipping_icon
                    EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION -> R.drawable.ic_verifying_zip_icon

                    EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL -> R.drawable.ic_canceling_icon

                    else -> R.drawable.ic_save_icon
                })
    }

    private fun handleProgress(intent: Intent?){

        if (intent != null){

            intent.getIntExtra(EXTRA_PROGRESS_PERCENTAGE, -1).let {
                if (it != -1) {
                    progressBar.progress = it
                    progressBar.isIndeterminate = false
                    progressPercent.text = "$it%"
                } else {
                    progressBar.isIndeterminate = true
                    progressPercent.text = "<-->"
                }
            }

            if (intent.hasExtra(EXTRA_TITLE)) {
                intent.getStringExtra(EXTRA_TITLE).trim().run {
                    if (this != lastTitle && this != ""){
                        progressTask.text = this
                        progressLogTextView.append("\n$this\n")
                        lastTitle = this
                    }
                }
            }

            if (intent.hasExtra(EXTRA_SUBTASK))
                subTask.text = intent.getStringExtra(EXTRA_SUBTASK)

            if (intent.hasExtra(EXTRA_TASKLOG)){
                intent.getStringExtra(EXTRA_TASKLOG).run {
                    if (this != lastLog && this != ""){
                        progressLogTextView.append("$this\n")
                        lastLog = this
                    }
                }
            }

            if (intent.hasExtra(EXTRA_PROGRESS_TYPE)){

                val type = intent.getStringExtra(EXTRA_PROGRESS_TYPE)

                if (type == EXTRA_PROGRESS_TYPE_FINISHED) {

                    commonTools.tryIt { forceStopDialog?.dismiss() }

                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    closeWarning.visibility = View.GONE

                    if (intent.hasExtra(EXTRA_ERRORS))
                        errors.addAll(intent.getStringArrayListExtra(EXTRA_ERRORS))

                    if (errors.size > 0) {
                        reportLogButton.visibility = View.VISIBLE
                        errorLogTextView.visibility = View.VISIBLE
                        errors.forEach {
                            errorLogTextView.append("$it\n")
                        }
                    }

                    if (errors.size != 0 || intent.getBooleanExtra(EXTRA_IS_CANCELLED, false)) {
                        progressTask.setTextColor(resources.getColor(R.color.error_color))
                        subTask.setTextColor(resources.getColor(R.color.error_color))
                    }

                    intent.getLongExtra(EXTRA_TOTAL_TIME, -1).let {
                        if (it != -1L) subTask.text = calendarDifference(it)
                    }

                    progressActionButton.apply {
                        text = getString(R.string.close)
                        background = getDrawable(R.drawable.log_action_button)
                        setOnClickListener {
                            startActivity(Intent(this@ProgressShowActivity, MainActivityKotlin::class.java))
                            finish()
                        }
                    }
                }

                setImageIcon(intent, type)
            }
        }
    }

    private val progressReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                handleProgress(intent)
            }
        }
    }

    private fun calendarDifference(longDiff: Long): String {
        var longDiff = longDiff
        var diff = ""

        try {

            longDiff /= 1000

            val d = longDiff / (60 * 60 * 24)
            if (d != 0L) diff = diff + d + "days "
            longDiff %= (60 * 60 * 24)

            val h = longDiff / (60 * 60)
            if (h != 0L) diff = diff + h + "hrs "
            longDiff %= (60 * 60)

            val m = longDiff / 60
            if (m != 0L) diff = diff + m + "mins "
            longDiff %= 60

            val s = longDiff
            diff = diff + s + "secs"

        } catch (ignored: Exception) {
        }

        return diff
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.backup_progress_layout)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        progressTask.text = getString(R.string.please_wait)

        progressActionButton.apply {
            text = getString(android.R.string.cancel)
            background = getDrawable(R.drawable.cancel_backup)
            setOnClickListener {
                if (this.text == getString(R.string.force_stop)) {

                    forceStopDialog = AlertDialog.Builder(this@ProgressShowActivity).apply {

                        this.setTitle(getString(R.string.force_stop_alert_title))
                        this.setMessage(getString(R.string.force_stop_alert_desc))

                        setPositiveButton(R.string.kill_app) { _, _ ->

                            Runtime.getRuntime().exec("su").apply {
                                BufferedWriter(OutputStreamWriter(this.outputStream)).run {
                                    this.write("am force-stop $packageName\n")
                                    this.write("exit\n")
                                    this.flush()
                                }
                            }

                            val handler = Handler()
                            handler.postDelayed({
                                Toast.makeText(this@ProgressShowActivity, R.string.killing_programmatically, Toast.LENGTH_SHORT).show()
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }, TIMEOUT_WAITING_TO_KILL)
                        }

                        setNegativeButton(R.string.wait_to_cancel, null)

                    }.create()

                    forceStopDialog?.show()
                }
                else {
                    text = getString(R.string.force_stop)
                    commonTools.LBM?.sendBroadcast(Intent(ACTION_BACKUP_CANCEL))
                }
            }
        }

        progressBar.max = 100

        reportLogButton.apply {
            visibility = View.GONE
            setOnClickListener { commonTools.reportLogs(true) }
        }

        progressLogTextView.apply {
            gravity = Gravity.BOTTOM
            movementMethod = ScrollingMovementMethod()
        }

        if (intent.extras != null) {
            handleProgress(intent)
            intent.getIntExtra(EXTRA_TOTAL_PARTS, 1).run {
                if (this > 1){
                    val head = "$this ${getString(R.string.parts)}"
                    AlertDialog.Builder(this@ProgressShowActivity)
                            .setTitle(head)
                            .setMessage(getString(R.string.split_desc_1) + " " + head + "\n\n" + getString(R.string.split_desc_2))
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                }
            }
        }

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_BACKUP_PROGRESS))
    }

    override fun onDestroy() {
        super.onDestroy()
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        commonTools.tryIt { forceStopDialog?.dismiss() }
    }
}