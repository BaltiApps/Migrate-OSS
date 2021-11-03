package balti.migrate.simpleActivities

import android.content.Intent
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
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import balti.filex.FileX
import balti.migrate.R
import balti.migrate.backupEngines.engines.AppBackupEngine
import balti.migrate.utilities.BackupProgressNotificationSystem
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.BackupUpdate
import balti.migrate.utilities.BackupProgressNotificationSystem.Companion.ProgressType.*
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_BACKUP_CANCEL
import balti.migrate.utilities.CommonToolsKotlin.Companion.DIR_APP_AUX_FILES
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_ERRORS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FINISHED_ZIP_PATHS
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_IS_CANCELLED
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_TITLE
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_TOTAL_TIME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_WARNINGS
import balti.migrate.utilities.IconTools
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.backup_progress_layout.*
import kotlinx.android.synthetic.main.zip_name_show.view.*
import kotlinx.coroutines.delay
import org.apache.commons.collections4.queue.CircularFifoQueue
import java.util.*
import kotlin.collections.ArrayList

class ProgressShowActivity_new: AppCompatActivity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }
    private val iconTools by lazy { IconTools() }
    private val errors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }

    private var lastLog = ""
    private var lastIcon = ""

    private var lastTitle = ""
    private var lastSubTask = ""

    private var forceStopDialog: AlertDialog? = null

    private val auxDirectory by lazy { FileX.new(filesDir.canonicalPath, DIR_APP_AUX_FILES, true) }

    private val logQueue: Queue<String> = CircularFifoQueue(25)
    private var checkLoop = false

    /**
     * Called from [updateUiOnProgress].
     * Method to set the [app_icon], depending on the `type` of [backupUpdate].
     */
    private fun setDisplayIcon(backupUpdate: BackupUpdate){

        /**
         * Handle app icons which are being backed up, separately.
         */
        if (backupUpdate.type == PROGRESS_TYPE_APP_PROGRESS){
            try {
                if (lastIcon != AppBackupEngine.ICON_STRING) {
                    AppBackupEngine.ICON_STRING.run {
                        if (this.contains(".icon", true) || this.contains(".png", true)){
                            iconTools.setIconFromFile(app_icon, FileX.new(auxDirectory.canonicalPath, this.trim(), true))
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

        /**
         * For other types, use predefined icons.
         */
        else {
            val iconRes: Int = when(backupUpdate.type){
                EMPTY -> R.drawable.ic_waiting
                BACKUP_CANCELLED -> R.drawable.ic_cancelled_icon.apply {
                    setFinishedIconColour(this)
                }
                PROGRESS_TYPE_FINISHED -> {
                    when {
                        errors.isNotEmpty() -> R.drawable.ic_error
                        warnings.isNotEmpty() -> R.drawable.ic_finished_warning
                        else -> R.drawable.ic_finished_icon
                    }.apply {
                        setFinishedIconColour(this)
                    }
                }


                PROGRESS_TYPE_TESTING -> R.drawable.ic_system_testing_icon
                PROGRESS_TYPE_CONTACTS -> R.drawable.ic_contacts_icon
                PROGRESS_TYPE_SMS -> R.drawable.ic_sms_icon
                PROGRESS_TYPE_CALLS -> R.drawable.ic_call_log_icon
                PROGRESS_TYPE_SETTINGS -> R.drawable.ic_settings_icon

                PROGRESS_TYPE_APP_PROGRESS -> R.drawable.ic_save_icon
                /* This is just to make "when" not complain.
                 * This progress type will be handled by the previous if condition.
                 *
                 * Not using "else ->" because in that case it will never complain
                 * if any new progress type is added, which is not desirable.
                 *
                 * Basically we don't want to completely suppress errors and warnings.
                 */

                PROGRESS_TYPE_MAKING_APP_SCRIPTS -> R.drawable.ic_app_scripts_icon
                PROGRESS_TYPE_MOVING_APP_FILES -> R.drawable.ic_moving_files

                PROGRESS_TYPE_VERIFYING -> R.drawable.ic_verify_icon
                PROGRESS_TYPE_CORRECTING -> R.drawable.ic_correcting_icon

                PROGRESS_TYPE_MAKING_ZIP_BATCH -> R.drawable.ic_making_batches

                PROGRESS_TYPE_UPDATER_SCRIPT -> R.drawable.ic_updater_script_icon

                PROGRESS_TYPE_ZIPPING -> R.drawable.ic_zipping_icon
                PROGRESS_TYPE_ZIP_VERIFICATION -> R.drawable.ic_verifying_zip_icon

                PROGRESS_TYPE_WAITING_TO_CANCEL -> R.drawable.ic_canceling_icon
            }

            app_icon.setImageResource(iconRes)
        }
    }

    /**
     * Called from [setDisplayIcon].
     * Colour the [app_icon] RED if errors are present.
     * If not, colour the icon YELLOW if warnings are present.
     */
    private fun setFinishedIconColour(iconRes: Int){
        var doColorRed = false
        var doColorYellow = false

        when (iconRes) {
            R.drawable.ic_cancelled_icon -> doColorRed = true
            R.drawable.ic_error -> doColorRed = true
            R.drawable.ic_finished_warning -> doColorYellow = true
        }

        if (doColorRed) {
            app_icon.setColorFilter (
                ContextCompat.getColor(this@ProgressShowActivity_new, R.color.error_color),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        if (doColorYellow) {
            app_icon.setColorFilter (
                ContextCompat.getColor(this@ProgressShowActivity_new, R.color.warning_color),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }
    }

    /**
     * Called if detected backup is cancelled or finished.
     * Called by [updateUiOnProgress].
     *
     * Function used to:
     * - Display errors and warnings.
     * - Display total time taken by backup.
     * - Update [progressActionButton] behaviour.
     *
     * @param bundle Bundle to read info from. Expected keys:
     * - [EXTRA_ERRORS]: ArrayList<String>
     * - [EXTRA_WARNINGS]: ArrayList<String>
     * - [EXTRA_IS_CANCELLED]: Boolean
     * - [EXTRA_TOTAL_TIME]: Long
     * - [EXTRA_FINISHED_ZIP_PATHS]: ArrayList<String>
     */
    private fun updateUiOnBackupFinished(bundle: Bundle){

        /**
         * First read all errors and warnings.
         */
        bundle.getStringArrayList(EXTRA_ERRORS)?.let { errors.addAll(it) }
        bundle.getStringArrayList(EXTRA_WARNINGS)?.let { warnings.addAll(it) }

        /**
         * Minor UI updates
         */
        tryIt { forceStopDialog?.dismiss() }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        closeWarning.visibility = View.GONE

        /**
         * Display the errors and warnings.
         */
        if (errors.isNotEmpty() || warnings.isNotEmpty()) {

            /**
             * Un-hide error reporting button.
             * Un-hide text view to display errors.
             */
            reportLogButton.visibility = View.VISIBLE
            errorLogTextView.visibility = View.VISIBLE

            /**
             * By default, [errorLogTextView] shows in RED.
             * For warnings, we want to display in yellow.
             */
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

            /**
             * Show errors in RED as normal.
             */
            if (errors.isNotEmpty()) {
                errors.forEach {
                    errorLogTextView.append("$it\n")
                }
            }

            /**
             * Colour the title and subTask in RED if there are errors, or backup is cancelled.
             */
            if (errors.size != 0 || bundle.getBoolean(EXTRA_IS_CANCELLED, false)) {
                progressTask.setTextColor(resources.getColor(R.color.error_color))
                subTask.setTextColor(resources.getColor(R.color.error_color))
            }

            /**
             * Get total time in millis and use [calendarDifference]
             * to get a human readable form.
             */
            bundle.getLong(EXTRA_TOTAL_TIME, -1).let {
                if (it != -1L) subTask.text = calendarDifference(it)
            }
        }

        /**
         * [progressActionButton] is by default used to cancel a backup.
         * Now that backup is over, modify its behavior to close the activity.
         */
        progressActionButton.apply {
            text = getString(R.string.close)
            background = AppCompatResources.getDrawable(this@ProgressShowActivity_new, R.drawable.log_action_button)
            setOnClickListener {
                startActivity(Intent(this@ProgressShowActivity_new, MainActivityKotlin::class.java))
                finish()
            }
        }
    }

    /**
     * Function to update title, subtask, display logs, update progress percentage.
     * This is the main function from where UI is updated when new update is received.
     */
    private fun updateUiOnProgress(backupUpdate: BackupUpdate) {

        /**
         * Set title if not same as [lastTitle].
         */
        backupUpdate.title.trim().run {
            if (this != lastTitle && this != "") {
                progressTask.text = this
                progressLogTextView.append("\n$this\n")
                lastTitle = this
            }
        }

        /**
         * Set sub-task if not same as [lastSubTask].
         */
        backupUpdate.subTask.trim().run {
            if (this != lastSubTask && this != "") {
                subTask.text = this
                progressLogTextView.append("\n$this\n")
                lastSubTask = this
            }
        }

        /**
         * Set verbose log.
         * Not running trim() because it kills extra line breaks.
         */
        backupUpdate.log.run {
            if (this != lastLog && this != "") {
                logQueue.add("$this\n")
                lastLog = this
            }
        }

        /**
         * Set progress.
         */
        backupUpdate.progressPercent.let {
            if (it != -1) {
                progressBar.progress = it
                progressBar.isIndeterminate = false
                progressPercent.text = "$it%"
            } else {
                progressBar.isIndeterminate = true
                progressPercent.text = "<-->"
            }
        }

        /**
         * Set [app_icon].
         * This will also set the icon colour, if needed,
         * to RED when backup finishes using [setFinishedIconColour].
         */
        setDisplayIcon(backupUpdate)

        if (backupUpdate.type == PROGRESS_TYPE_FINISHED || backupUpdate.type == BACKUP_CANCELLED)
            backupUpdate.extraInfoBundle?.let { updateUiOnBackupFinished(it) }
    }

    /**
     * If activity is called from notification, use this function
     * to create a [BackupUpdate] object from intent extras.
     *
     * Expected keys:
     * - [EXTRA_ERRORS]: ArrayList<String>
     * - [EXTRA_WARNINGS]: ArrayList<String>
     * - [EXTRA_IS_CANCELLED]: Boolean
     * - [EXTRA_TOTAL_TIME]: Long
     * - [EXTRA_FINISHED_ZIP_PATHS]: ArrayList<String>
     * - [EXTRA_TITLE]: String
     */
    private fun createFinishedUpdateFromIntent(bundle: Bundle): BackupUpdate {
        val backupUpdate = BackupUpdate(
            type =
            if (bundle.getBoolean(EXTRA_IS_CANCELLED, false)) BACKUP_CANCELLED
            else PROGRESS_TYPE_FINISHED,

            title = bundle.getString(EXTRA_TITLE, ""),

            subTask = "",        // subTask will have total time required to complete backup.

            log = "",

            progressPercent = 0,

            extraInfoBundle = Bundle().apply {
                putStringArrayList(EXTRA_ERRORS, bundle.getStringArrayList(EXTRA_ERRORS))
                putStringArrayList(EXTRA_WARNINGS, bundle.getStringArrayList(EXTRA_WARNINGS))
                putBoolean(EXTRA_IS_CANCELLED, bundle.getBoolean(EXTRA_IS_CANCELLED, false))
                putLong(EXTRA_TOTAL_TIME, bundle.getLong(EXTRA_TOTAL_TIME))
                putStringArrayList(EXTRA_FINISHED_ZIP_PATHS, bundle.getStringArrayList(EXTRA_FINISHED_ZIP_PATHS)?: arrayListOf())
            }

        )

        return backupUpdate
    }

    /**
     * @param longDiff in millis.
     */
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

        /**
         * By default, set [progressActionButton] to cancel backup / force stop application.
         */
        progressActionButton.apply {
            text = getString(android.R.string.cancel)
            background = AppCompatResources.getDrawable(this@ProgressShowActivity_new, R.drawable.cancel_backup)
            setOnClickListener {
                if (this.text == getString(R.string.force_stop)) {

                    forceStopDialog = AlertDialog.Builder(this@ProgressShowActivity_new).apply {

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

        /**
         * Add listener to listen to updates.
         */
        lifecycleScope.launchWhenStarted {
            BackupProgressNotificationSystem.addListener(true){
                updateUiOnProgress(it)
            }
        }

        lifecycleScope.launchWhenStarted {
            while (true){
                if (checkLoop){
                    while (logQueue.isNotEmpty()) {
                        logQueue.poll()?.let {
                            progressLogTextView.append(it)
                        }
                    }
                }
                delay(1)
            }
        }

        /**
         * In case activity is called from notification pending intent,
         * Intent extras will not be null.
         * Use the intent extras to create a new instance of [BackupUpdate] and pass to [updateUiOnProgress].
         */
        intent.extras?.run {
            if (!isEmpty){
                updateUiOnProgress(createFinishedUpdateFromIntent(this))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        checkLoop = false
    }

    override fun onResume() {
        super.onResume()
        checkLoop = true
    }

    override fun onDestroy() {
        super.onDestroy()
        tryIt { forceStopDialog?.dismiss() }
    }
}