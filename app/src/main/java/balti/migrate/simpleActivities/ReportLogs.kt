package balti.migrate.simpleActivities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import balti.filex.FileX
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_IS_ERROR_LOG_MANDATORY
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_DEVICE_INFO
import balti.migrate.utilities.CommonToolsKotlin.Companion.REPORTING_EMAIL
import balti.migrate.utilities.CommonToolsKotlin.Companion.TG_CLIENTS
import balti.migrate.utilities.CommonToolsKotlin.Companion.TG_LINK
import balti.module.baltitoolbox.functions.Misc.activityStart
import balti.module.baltitoolbox.functions.Misc.doBackgroundTask
import balti.module.baltitoolbox.functions.Misc.isPackageInstalled
import balti.module.baltitoolbox.functions.Misc.openWebLink
import balti.module.baltitoolbox.functions.Misc.playStoreLink
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.error_report_layout.*

class ReportLogs: AppCompatActivity() {

    private var isErrorLogMandatory = false

    private val cache = AppInstance.CACHE_DIR

    private val progressLog by lazy { FileX.new(cache, CommonToolsKotlin.FILE_PROGRESSLOG, isTraditional = true) }
    private val errorLog by lazy { FileX.new(cache, CommonToolsKotlin.FILE_ERRORLOG, isTraditional = true) }
    private val backupScripts by lazy {
        cache.let {
            FileX.new(it, true).listFiles { f: FileX ->
                (
                        f.name.startsWith(CommonToolsKotlin.FILE_PREFIX_BACKUP_SCRIPT) ||
                                f.name.startsWith(CommonToolsKotlin.FILE_PREFIX_RETRY_SCRIPT) ||
                                f.name.startsWith(CommonToolsKotlin.FILE_PREFIX_TAR_CHECK)
                        ) && f.name.endsWith(".sh")
            }
        } ?: emptyArray()
    }
    private val rawList by lazy { FileX.new(cache, CommonToolsKotlin.FILE_RAW_LIST, true) }
    private val mdpLog by lazy { FileX.new(cache, CommonToolsKotlin.FILE_MDP_LOG, true) }

    private val deviceSpecifications by lazy {
        CommonToolsKotlin().deviceSpecifications
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.error_report_layout)

        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        setFinishOnTouchOutside(false)

        isErrorLogMandatory = intent?.getBooleanExtra(EXTRA_IS_ERROR_LOG_MANDATORY, false) ?: false

        fun CheckBox.setCheckedEnabled(value: Boolean) = this.apply {
            isChecked = value; isEnabled = value
        }

        share_progress_checkbox.setCheckedEnabled(progressLog.exists())
        share_mdp_checkbox.setCheckedEnabled(mdpLog.exists())
        share_script_checkbox.setCheckedEnabled(backupScripts.isNotEmpty())
        share_errors_checkbox.setCheckedEnabled(errorLog.exists())
        share_rawList_checkbox.setCheckedEnabled(rawList.exists())

        if (isErrorLogMandatory && !errorLog.exists()) {
            noLogsExist(true)
        } else if (errorLog.exists() || progressLog.exists() || backupScripts.isNotEmpty()) {

            report_button_privacy_policy.setOnClickListener {
                activityStart(this, PrivacyPolicy::class.java)
            }

            report_button_join_group.setOnClickListener {
                openWebLink(TG_LINK)
            }

            report_button_old_email.setOnClickListener {
                sendIntent(getUris(), true)
            }

            var isTgClientInstalled = false
            for (i in TG_CLIENTS.indices) {
                if (isPackageInstalled(TG_CLIENTS[i])) {
                    isTgClientInstalled = true
                    break
                }
            }

            if (!isTgClientInstalled) {
                report_button_telegram.apply {
                    text = context.getString(R.string.install_tg)
                    setOnClickListener { playStoreLink(TG_CLIENTS[0]) }
                }
            } else {
                report_button_telegram.apply {
                    text = context.getString(R.string.send_to_tg)
                    setOnClickListener { sendIntent(getUris()) }
                }
            }
        }
        else {
            noLogsExist()
        }

    }

    private fun getUris(): ArrayList<Uri> {

        val uris = ArrayList<Uri>(0)
        try {

            if (share_errors_checkbox.isChecked) uris.add(getUri(errorLog))
            if (share_progress_checkbox.isChecked) uris.add(getUri(progressLog))
            if (share_mdp_checkbox.isChecked) uris.add(getUri(mdpLog))
            if (share_script_checkbox.isChecked) for (f in backupScripts) uris.add(getUri(f))
            if (share_rawList_checkbox.isChecked) uris.add(getUri(rawList))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
        }

        return uris
    }

    private fun getUri(file: FileX) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                FileProvider.getUriForFile(this, "migrate.provider", file.file)
            else Uri.fromFile(file.file)

    private fun sendIntent(uris: ArrayList<Uri>, isEmail: Boolean = false) {
        Intent().run {

            action = Intent.ACTION_SEND_MULTIPLE
            type = "text/plain"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            if (isEmail) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORTING_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "Log report for Migrate")
                putExtra(Intent.EXTRA_TEXT, deviceSpecifications)

                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                startActivity(Intent.createChooser(this, getString(R.string.select_mail)))
            } else doBackgroundTask({

                tryIt {
                    val infoFile = FileX.new(cache, FILE_DEVICE_INFO, true)
                    infoFile.startWriting(object : FileX.Writer(){
                        override fun writeLines() {
                            writeLine(deviceSpecifications)
                        }
                    })
                    uris.add(getUri(infoFile))
                }

            }, {
                this.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                startActivity(Intent.createChooser(this, getString(R.string.select_telegram)))
            })
        }
    }

    private fun noLogsExist(onlyError: Boolean = false) {

        val msg = if (onlyError) {
            getString(R.string.error_log_does_not_exist)
        } else {
            getString(R.string.progress_log_does_not_exist) + "\n" +
                    getString(R.string.error_log_does_not_exist) + "\n" +
                    getString(R.string.backup_script_does_not_exist) + "\n"
        }

        AlertDialog.Builder(this)
                .setTitle(R.string.log_files_do_not_exist)
                .setMessage(msg)
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel){_, _ ->
                    finish()
                }
                .show()
    }
}