package balti.migrate.simpleActivities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import balti.migrate.R
import balti.migrate.backupEngines.engines.AppBackupEngine
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_BACKUP_CANCEL
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_ACTUAL_DESTINATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_ERRORS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_IS_CANCELLED
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_PERCENTAGE
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_APP_PROGRESS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CALLS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CONTACTS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_CORRECTING
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_FINISHED
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_MAKING_APP_SCRIPTS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_MAKING_ZIP_BATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_SETTINGS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_SMS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_TESTING
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_UPDATER_SCRIPT
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_VERIFYING
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_WAITING_TO_CANCEL
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_WIFI
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_PROGRESS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_SUBTASK
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_TASKLOG
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_TOTAL_TIME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_WARNINGS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_ZIP_NAMES
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DELETE_ERROR_BACKUP
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SHOW_BACKUP_SUMMARY
import balti.migrate.utilities.IconTools
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import kotlinx.android.synthetic.main.backup_progress_layout.*
import kotlinx.android.synthetic.main.zip_name_show.view.*
import java.io.File

class ProgressShowActivity: AppCompatActivity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val iconTools by lazy { IconTools() }
    private val errors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }

    private var lastLog = ""
    private var lastIcon = ""

    private var lastTitle = ""

    private var forceStopDialog: AlertDialog? = null

    private fun setImageIcon(intent: Intent, type: String){

        if (type == EXTRA_PROGRESS_TYPE_APP_PROGRESS){
            try {
                if (lastIcon != AppBackupEngine.ICON_STRING) {
                    AppBackupEngine.ICON_STRING.run {
                        if (this.contains(".icon", true) || this.contains(".png", true)){
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
                        errors.isNotEmpty() -> R.drawable.ic_error
                        warnings.isNotEmpty() -> R.drawable.ic_finished_warning
                        else -> R.drawable.ic_finished_icon
                    }
            )

            fun showPartNames(){
                if (getPrefBoolean(PREF_SHOW_BACKUP_SUMMARY, true)) {
                    tryIt {
                        val view = View.inflate(this, R.layout.zip_name_show, null)
                        val name = view.zns_backup_name
                        val zipList = view.zns_zip_holder
                        if (intent.hasExtra(EXTRA_BACKUP_NAME)) {
                            tryIt {
                                name.text = intent.getStringExtra(EXTRA_BACKUP_NAME)
                            }
                        }
                        if (intent.hasExtra(EXTRA_ZIP_NAMES)) {
                            tryIt {
                                val parts = intent.getStringArrayListExtra(EXTRA_ZIP_NAMES)
                                parts.sortWith(Comparator { p1, p2 ->
                                    String.CASE_INSENSITIVE_ORDER.compare(p1, p2)
                                })
                                parts.forEach {
                                    zipList.append("$it.zip\n")
                                }
                            }
                        }

                        AlertDialog.Builder(this).apply {
                            setView(view)
                            setPositiveButton(android.R.string.ok, null)
                        }
                                .show()
                    }
                }
            }

            if (errors.size != 0 || isCancelled) {
                app_icon.setColorFilter (
                        ContextCompat.getColor(this@ProgressShowActivity, R.color.error_color),
                        android.graphics.PorterDuff.Mode.SRC_IN
                )
                if (!getPrefBoolean(PREF_DELETE_ERROR_BACKUP, true)){
                    showPartNames()
                }
            }
            else {
                showPartNames()
            }
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

                    EXTRA_PROGRESS_TYPE_MAKING_ZIP_BATCH -> R.drawable.ic_making_batches

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

                    tryIt { forceStopDialog?.dismiss() }

                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    closeWarning.visibility = View.GONE

                    if (intent.hasExtra(EXTRA_ERRORS))
                        errors.addAll(intent.getStringArrayListExtra(EXTRA_ERRORS))

                    if (intent.hasExtra(EXTRA_WARNINGS))
                        warnings.addAll(intent.getStringArrayListExtra(EXTRA_WARNINGS))

                    if (errors.isNotEmpty() || warnings.isNotEmpty()) {
                        reportLogButton.visibility = View.VISIBLE
                        errorLogTextView.visibility = View.VISIBLE

                        if (warnings.isNotEmpty()){
                            val builder = SpannableStringBuilder()
                            warnings.forEach {
                                SpannableString("$it\n").run {
                                    setSpan(ForegroundColorSpan(resources.getColor(R.color.warning_color)), 0, it.length, 0)
                                    builder.append(this)
                                }
                            }
                            errorLogTextView.setText(builder, TextView.BufferType.SPANNABLE)
                        }

                        if (errors.isNotEmpty()) {
                            errors.forEach {
                                errorLogTextView.append("$it\n")
                            }
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
                            commonTools.forceCloseThis()
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
        }

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_BACKUP_PROGRESS))
    }

    override fun onDestroy() {
        super.onDestroy()
        tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        tryIt { forceStopDialog?.dismiss() }
    }
}