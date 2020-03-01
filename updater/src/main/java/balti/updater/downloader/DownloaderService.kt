package balti.updater.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import balti.updater.Constants.Companion.EXTRA_CANCEL_DOWNLOAD
import balti.updater.Constants.Companion.EXTRA_DOWNLOAD_FINISHED
import balti.updater.Constants.Companion.EXTRA_DOWNLOAD_MESSAGE
import balti.updater.Constants.Companion.EXTRA_DOWNLOAD_URL
import balti.updater.Constants.Companion.EXTRA_ENTIRE_JSON_DATA
import balti.updater.Constants.Companion.EXTRA_FILE_SIZE
import balti.updater.Constants.Companion.EXTRA_HOST
import balti.updater.Constants.Companion.NOTIFICATION_CHANNEL_DOWNLOAD
import balti.updater.Constants.Companion.NOTIFICATION_ID
import balti.updater.Constants.Companion.OK
import balti.updater.R
import balti.updater.Tools
import balti.updater.Updater
import balti.updater.UpdaterMain
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

internal class DownloaderService: LifecycleService() {

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val downloadNotif by lazy {
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_DOWNLOAD).apply {
            setSmallIcon(R.drawable.ic_download_notif_icon)
            setContentTitle(getString(R.string.loading))
            addAction(NotificationCompat.Action(null, getString(android.R.string.cancel), PendingIntent.getService(
                    this@DownloaderService, 0,
                    Intent(this@DownloaderService, DownloaderService::class.java)
                            .putExtra(EXTRA_CANCEL_DOWNLOAD, true),
                    PendingIntent.FLAG_UPDATE_CURRENT
            )))
        }
    }

    private lateinit var cJob: CompletableJob
    private var jsonData = ""
    private var host = ""

    companion object {
        val size = MutableLiveData<Int>()
        val progress = MutableLiveData<Int>()
        val messageOnComplete = MutableLiveData<String>()
        val downladJobFinishedOrCancelled = MutableLiveData<Boolean>(false)
        var isJobActive = false
    }

    private fun updateNotificationProgress(progress: Int){
        downloadNotif.apply {
            setContentTitle(getString(R.string.downloading))
            setProgress(100, progress, false)
        }
        notificationManager.notify(NOTIFICATION_ID, downloadNotif.build())
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_DOWNLOAD, NOTIFICATION_CHANNEL_DOWNLOAD, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        cJob = Job()

        startForeground(NOTIFICATION_ID, downloadNotif.build())
    }

    private fun setContentIntentExtras(bundle: Bundle = Bundle()){
        downloadNotif.setContentIntent(PendingIntent.getActivity(this@DownloaderService, 10,
                Intent(Updater.context, UpdaterMain::class.java).putExtra(EXTRA_ENTIRE_JSON_DATA, jsonData)
                        .putExtra(EXTRA_FILE_SIZE, size.value)
                        .putExtra(EXTRA_HOST, host)
                        .putExtras(bundle),
                PendingIntent.FLAG_UPDATE_CURRENT
        ))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.run {

            Tools().tryIt {
                if (hasExtra(EXTRA_ENTIRE_JSON_DATA))
                    jsonData = getStringExtra(EXTRA_ENTIRE_JSON_DATA)

                if (hasExtra(EXTRA_HOST))
                    host = getStringExtra(EXTRA_HOST)

                setContentIntentExtras()
            }

            if (getBooleanExtra(EXTRA_CANCEL_DOWNLOAD, false)){

                isJobActive = false
                downladJobFinishedOrCancelled.value = true

                cJob.cancel()
                stopSelf()
            }
            else if (hasExtra(EXTRA_DOWNLOAD_URL) && !isJobActive) {
                val url = getStringExtra(EXTRA_DOWNLOAD_URL)

                CoroutineScope(Main + cJob).launch {

                    downladJobFinishedOrCancelled.value = false
                    isJobActive = true

                    notificationManager.cancelAll()

                    val downloadJob = DownloadJob()

                    downloadJob.lengthOfFile.observe(this@DownloaderService, Observer<Int> {
                        size.value = it
                    })

                    downloadJob.progress.observe(this@DownloaderService, Observer<Int> {
                        updateNotificationProgress(it)
                        progress.value = it
                    })

                    withContext(IO) {
                        downloadJob.execute(url)
                    }.let {
                        if (it != ""){
                            downloadNotif.apply {
                                setContentTitle(getString(R.string.download_error))
                                setContentText(it)
                                setSmallIcon(R.drawable.ic_error_notif_icon)
                            }
                            setContentIntentExtras(Bundle().apply { putString(EXTRA_DOWNLOAD_MESSAGE, it) })
                        }
                        else {
                            downloadNotif.apply {
                                setContentTitle(getString(R.string.download_complete))
                                setContentText(getString(R.string.click_to_install))
                            }
                            setContentIntentExtras(Bundle().apply {
                                putBoolean(EXTRA_DOWNLOAD_FINISHED, true)
                            })
                        }
                        downloadNotif.apply {
                            mActions.clear()
                            setProgress(0, 0, false)
                            setAutoCancel(true)
                        }

                        isJobActive = false
                        downladJobFinishedOrCancelled.value = true

                        messageOnComplete.value = if (it.isBlank()) OK else it
                        notificationManager.notify(NOTIFICATION_ID+1, downloadNotif.build())
                    }

                    stopSelf()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isJobActive = false
        downladJobFinishedOrCancelled.value = true
        messageOnComplete.value = ""
        super.onDestroy()
    }
}