package balti.updater.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import balti.updater.Constants.Companion.EXTRA_CANCEL_DOWNLOAD
import balti.updater.Constants.Companion.EXTRA_DOWNLOAD_URL
import balti.updater.Constants.Companion.NOTIFICATION_CHANNEL_DOWNLOAD
import balti.updater.Constants.Companion.NOTIFICATION_ID
import balti.updater.R
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

    companion object {
        val size = MutableLiveData<Int>()
        val progress = MutableLiveData<Int>()
        val messageOnComplete = MutableLiveData<String>()
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.run {

            if (getBooleanExtra(EXTRA_CANCEL_DOWNLOAD, false)){
                cJob.cancel()
                stopSelf()
            }
            else if (hasExtra(EXTRA_DOWNLOAD_URL) && !isJobActive) {
                val url = getStringExtra(EXTRA_DOWNLOAD_URL)

                CoroutineScope(Main + cJob).launch {

                    isJobActive = true

                    val downloadJob = DownloadJob()

                    downloadJob.lengthOfFile.observe(this@DownloaderService, Observer<Int> {
                        size.value = it
                    })

                    downloadJob.progress.observe(this@DownloaderService, Observer<Int> {
                        updateNotificationProgress(it)
                        progress.value = it
                    })

                    messageOnComplete.value = withContext(IO) {
                        downloadJob.execute(url)
                    }

                    stopSelf()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isJobActive = false
        super.onDestroy()
    }
}