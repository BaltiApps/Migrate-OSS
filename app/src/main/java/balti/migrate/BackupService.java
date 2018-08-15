package balti.migrate;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.Nullable;

import java.util.Objects;

/**
 * Created by sayantan on 15/10/17.
 */

public class BackupService extends IntentService {

    BroadcastReceiver cancelReceiver, progressBroadcast, requestProgress;
    IntentFilter cancelReceiverIF, progressBroadcastIF, requestProgressIF;

    int p;
    String task = "";
    public static final String CHANNEL = "Backup notification";

    BackupEngine backupEngine;

    Intent toReturnIntent;

    public BackupService() {
        super("bService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        assert intent != null;
        try {
            backupEngine = new BackupEngine(intent.getStringExtra("backupName"), intent.getIntExtra("compressionLevel", 0), intent.getStringExtra("destination"), this);
        }
        catch (Exception e){e.printStackTrace();}
            toReturnIntent = new Intent("Migrate progress broadcast");
            progressBroadcast = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    toReturnIntent = intent;
                }
            };
            progressBroadcastIF = new IntentFilter("Migrate progress broadcast");
            registerReceiver(progressBroadcast, progressBroadcastIF);
            cancelReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        backupEngine.cancelProcess();
                    }
                    catch (Exception ignored){}
                }
            };
            cancelReceiverIF = new IntentFilter("Migrate backup cancel broadcast");
            registerReceiver(cancelReceiver, cancelReceiverIF);
            requestProgress = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    sendBroadcast(toReturnIntent.setAction("Migrate progress broadcast"));
                }
            };
            requestProgressIF = new IntentFilter("get data");
            registerReceiver(requestProgress, requestProgressIF);

            Notification notification;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                NotificationChannel notificationChannel = new NotificationChannel(CHANNEL, CHANNEL, NotificationManager.IMPORTANCE_DEFAULT);
                ((NotificationManager) Objects.requireNonNull(getSystemService(NOTIFICATION_SERVICE))).createNotificationChannel(notificationChannel);
                notification = new Notification.Builder(this, CHANNEL)
                        .setContentTitle(getString(R.string.loading))
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .build();
            }
            else {
                notification = new Notification.Builder(this)
                        .setContentTitle(getString(R.string.loading))
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .build();
            }

            startForeground(BackupEngine.NOTIFICATION_ID, notification);

            try {
                backupEngine.initiateBackup();
            }catch (Exception e){
                e.printStackTrace();
                toReturnIntent.putExtra("progress", 100).putExtra("task", getString(R.string.error_loading_engine));
                sendBroadcast(toReturnIntent.setAction("Migrate progress broadcast"));
                stopSelf();
            }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(cancelReceiver);
        unregisterReceiver(progressBroadcast);
        unregisterReceiver(requestProgress);
    }
}
