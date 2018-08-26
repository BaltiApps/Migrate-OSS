package balti.migrate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.util.Objects;

/**
 * Created by sayantan on 15/10/17.
 */

public class BackupService extends Service {

    BroadcastReceiver cancelReceiver, progressBroadcast, requestProgress;
    IntentFilter cancelReceiverIF, progressBroadcastIF, requestProgressIF;

    int p;
    public static final String CHANNEL = "Backup notification";

    static BackupEngine backupEngine;

    Intent toReturnIntent;

    @Override
    public void onCreate() {
        super.onCreate();

        toReturnIntent = new Intent("Migrate progress broadcast");
        progressBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                toReturnIntent = intent;
            }
        };
        progressBroadcastIF = new IntentFilter("Migrate progress broadcast");
        LocalBroadcastManager.getInstance(this).registerReceiver(progressBroadcast, progressBroadcastIF);
        cancelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    BackupService.backupEngine.cancelProcess();
                } catch (Exception ignored) {
                }
            }
        };
        cancelReceiverIF = new IntentFilter("Migrate backup cancel broadcast");
        registerReceiver(cancelReceiver, cancelReceiverIF);
        LocalBroadcastManager.getInstance(this).registerReceiver(cancelReceiver, cancelReceiverIF);
        requestProgress = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LocalBroadcastManager.getInstance(BackupService.this).sendBroadcast(toReturnIntent.setAction("Migrate progress broadcast"));
            }
        };
        requestProgressIF = new IntentFilter("get data");
        LocalBroadcastManager.getInstance(this).registerReceiver(requestProgress, requestProgressIF);

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL, CHANNEL, NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) Objects.requireNonNull(getSystemService(NOTIFICATION_SERVICE))).createNotificationChannel(notificationChannel);
            notification = new Notification.Builder(this, CHANNEL)
                    .setContentTitle(getString(R.string.loading))
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.loading))
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .build();
        }

        startForeground(BackupEngine.NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        return super.onStartCommand(intent, flags, startId);
    }

    /*static void startBackup(String backupName, int compressionLevel, String destination,){
        try {
            BackupService.backupEngine = new BackupEngine(intent.getStringExtra("backupName"), intent.getIntExtra("compressionLevel", 0), intent.getStringExtra("destination"), this);
            Log.d("migrate", "init " + (backupEngine == null));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            BackupService.backupEngine.initiateBackup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/


    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(cancelReceiver);
        } catch (Exception ignored){}
        try {
            unregisterReceiver(cancelReceiver);
        } catch (Exception ignored){}
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(progressBroadcast);
        } catch (Exception ignored){}
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(requestProgress);
        } catch (Exception ignored){}
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
