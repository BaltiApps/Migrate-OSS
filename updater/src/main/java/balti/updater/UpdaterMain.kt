package balti.updater

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import balti.updater.Constants.Companion.EXTRA_CANCEL_DOWNLOAD
import balti.updater.Constants.Companion.EXTRA_DOWNLOAD_FINISHED
import balti.updater.Constants.Companion.EXTRA_DOWNLOAD_MESSAGE
import balti.updater.Constants.Companion.EXTRA_DOWNLOAD_URL
import balti.updater.Constants.Companion.EXTRA_ENTIRE_JSON_DATA
import balti.updater.Constants.Companion.EXTRA_FILE_SIZE
import balti.updater.Constants.Companion.EXTRA_HOST
import balti.updater.Constants.Companion.OK
import balti.updater.Constants.Companion.TELEGRAM_GROUP
import balti.updater.Constants.Companion.UPDATE_ERROR
import balti.updater.Constants.Companion.UPDATE_LAST_TESTED_ANDROID
import balti.updater.Constants.Companion.UPDATE_MESSAGE
import balti.updater.Constants.Companion.UPDATE_NAME
import balti.updater.Constants.Companion.UPDATE_STATUS
import balti.updater.Constants.Companion.UPDATE_URL
import balti.updater.Constants.Companion.UPDATE_VERSION
import balti.updater.Updater.Companion.thisVersion
import balti.updater.downloader.DownloaderService
import kotlinx.android.synthetic.main.updater_activity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import org.json.JSONObject

// Do not forget to declare in main app manifest

internal class UpdaterMain: AppCompatActivity() {

    private val tools by lazy { Tools(this) }
    private var updateUrl = ""
    private var updateSize = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.updater_activity)

        resetProgress()

        if (intent.hasExtra(EXTRA_ENTIRE_JSON_DATA)){
            tools.tryIt {
                JSONObject(intent.getStringExtra(EXTRA_ENTIRE_JSON_DATA)).let {

                    toggleLayout(2)
                    showDataFromJson(it)
                    setDownloadButton()

                    if (intent.getBooleanExtra(EXTRA_DOWNLOAD_FINISHED, false)) toggleLayout(3)

                    intent.getIntExtra(EXTRA_FILE_SIZE, 0).let { s-> if (s > 0) updateSize = s }

                    if (intent.hasExtra(EXTRA_HOST)) {
                        intent.getStringExtra(EXTRA_HOST).let { h -> update_host.text = "${getString(R.string.host)}: ${h?:""}" }
                    }

                    if (intent.hasExtra(EXTRA_DOWNLOAD_MESSAGE)) {
                        intent.getStringExtra(EXTRA_DOWNLOAD_MESSAGE).let {m ->
                            if (!m.isBlank()) showErrorDialog(m, "", false)
                        }
                    }
                }
            }
        }
        else {
            CoroutineScope(Main).launch {

                toggleLayout(1)
                val json = GetUpdateInfo(this@UpdaterMain).getInfo()

                tools.tryIt {
                    if (!isFinishing) {

                        if (json.has(UPDATE_ERROR)) {
                            showErrorDialog(json.getString(UPDATE_ERROR), getString(R.string.update_check_error), false)
                            toggleLayout(0)
                        } else if (json.has(UPDATE_VERSION) && json.getInt(UPDATE_VERSION) <= thisVersion) {

                            toggleLayout(0)
                            val ad = AlertDialog.Builder(this@UpdaterMain).apply {
                                val m = SpannableString(TELEGRAM_GROUP.let {
                                    if (it != "") String.format(getString(R.string.no_new_version_desc_tg), TELEGRAM_GROUP)
                                    else getString(R.string.no_new_version_desc)
                                })
                                Linkify.addLinks(m, Linkify.ALL)
                                setTitle(R.string.you_are_on_latest)
                                setMessage(m)
                                setCancelable(false)
                                setPositiveButton(R.string.close) { _, _ ->
                                    finish()
                                }
                            }
                                    .create()

                            ad.show()
                            ad.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()

                        } else if (!json.has(UPDATE_URL)) {
                            showErrorDialog(getString(R.string.no_update_url), getString(R.string.update_check_error), false)
                            toggleLayout(0)
                        } else {
                            toggleLayout(2)
                            showDataFromJson(json)
                            setDownloadButton(json)
                        }
                    }
                }
            }
        }

        DownloaderService.size.observe(this, Observer {
            updateSize = it
        })

        DownloaderService.progress.observe(this, Observer {
            if (it < 100) setDownloadProgress(it) else resetProgress()
        })

        DownloaderService.downladJobFinishedOrCancelled.observe(this, Observer {
            if (it) {
                resetProgress()
                setDownloadButton()
            }
        })

        DownloaderService.messageOnComplete.observe(this, Observer {
            if (it == OK) toggleLayout(3)
            else if (!it.isBlank()) showErrorDialog(it, getString(R.string.download_error))
            setDownloadButton()
        })
    }

    private fun resetProgress(){
        update_progress_layout.visibility = View.INVISIBLE
        update_download_progressbar.apply {
            max = 100
            progress = 0
        }
        update_size_in_text.text = ""
    }

    private fun setDownloadProgress(p: Int){
        if (DownloaderService.isJobActive) {
            update_progress_layout.visibility = View.VISIBLE
            update_download_progressbar.progress = p
            val progressInBytes = (updateSize * (p / 100.0)).toLong()
            update_size_in_text.text = "${tools.getHumanReadableStorageSpace(progressInBytes)}/${tools.getHumanReadableStorageSpace(updateSize.toLong())}"
        }
        else resetProgress()
    }

    private fun showDataFromJson(json: JSONObject){
        UPDATE_NAME.let { if (json.has(it)) update_name.text = json.getString(it) }
        UPDATE_VERSION.let { if (json.has(it)) update_version.text = json.getInt(it).toString() }
        UPDATE_LAST_TESTED_ANDROID.let { if (json.has(it)) update_last_android_version.text = json.get(it).toString() }
        UPDATE_STATUS.let { if (json.has(it)) update_status.text = json.getString(it) }
        UPDATE_MESSAGE.let { if (json.has(it)) update_info.text = json.getString(it) }
        updateUrl = json.getString(UPDATE_URL)
    }

    private fun toggleLayout(mode: Int){
        // mode = 0 -> hide all layouts
        // mode = 1 -> show only waiting layout
        // mode = 2 -> enable download layout
        // mode = 3 -> enable install layout

        if (mode == 0){
            update_check_wait_layout.visibility = View.GONE
            updater_content.visibility = View.GONE
        }
        else if (mode == 1) {
            update_check_wait_layout.visibility = View.VISIBLE
            updater_content.visibility = View.GONE
        }
        else if (mode > 1) {
            update_check_wait_layout.visibility = View.GONE
            updater_content.visibility = View.VISIBLE
            if (mode == 2) {
                update_button_install.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.disable_color))
                update_button_install.isEnabled = false
                update_radio_install_by_pm.isEnabled = false
                update_radio_install_by_root.isEnabled = false
            }
            if (mode == 3) {
                update_button_install.backgroundTintList = null
                update_button_install.isEnabled = true
                update_radio_install_by_pm.isEnabled = true
                update_radio_install_by_root.isEnabled = true
            }
        }
    }

    private fun setDownloadButton(jsonObject: JSONObject? = null) {
        update_button_download.apply {

            val host = Updater.getUpdateActiveHost()

            fun start(doCancel: Boolean) {
                Intent(this@UpdaterMain, DownloaderService::class.java).apply {

                    if (!doCancel) {
                        if (jsonObject != null) putExtra(EXTRA_ENTIRE_JSON_DATA, jsonObject.toString())
                        putExtra(EXTRA_DOWNLOAD_URL, updateUrl)
                    }
                    putExtra(EXTRA_CANCEL_DOWNLOAD, doCancel)
                    putExtra(EXTRA_HOST, host)

                }.let { i ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
                    else startService(i)
                }
            }

            text = if (DownloaderService.isJobActive)
                getString(android.R.string.cancel)
            else getString(R.string.download)

            setOnClickListener {
                if (text == getString(R.string.download)) {
                    if (updateUrl != "") {
                        update_host.text = "${getString(R.string.host)}: ${host}"
                        start(false)
                        text = getString(android.R.string.cancel)
                        setDownloadProgress(0)
                        toggleLayout(2)
                    }
                } else {
                    start(true)
                    text = getString(R.string.download)
                    resetProgress()
                }
            }
        }
    }

    private fun showErrorDialog(message: String, title: String = "", isCancelable: Boolean = true){
        try {
            AlertDialog.Builder(this)
                    .setMessage(message).apply {

                        if (isCancelable)
                            setNegativeButton(R.string.close, null)
                        else {
                            setCancelable(false)
                            setNegativeButton(R.string.close) { _, _ ->
                                finish()
                            }
                        }

                        if (title == "")
                            setTitle(R.string.error_occurred)
                        else setTitle(title)

                    }
                    .show()
        } catch (e: Exception){
            e.printStackTrace()
            try {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } catch (_: Exception){}
        }
    }
}