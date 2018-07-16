package balti.migrate;

import android.app.IntentService;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.Nullable;

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
        if (intent.getAction().equals("start service")) {
            backupEngine = new BackupEngine(intent.getStringExtra("backupSummary"), intent.getStringExtra("backupPackageNames"), intent.getStringExtra("backupName"), intent.getIntExtra("compressionLevel", 0), intent.getStringExtra("destination"), this);
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
                    backupEngine.cancelProcess();
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
                notification = new Notification.Builder(this, CHANNEL)
                        .setContentTitle(getString(R.string.loading))
                        .build();
            }
            else {
                notification = new Notification.Builder(this)
                        .setContentTitle(getString(R.string.loading))
                        .build();
            }

            startForeground(BackupEngine.NOTIFICATION_ID, notification);

            backupEngine.initiateBackup();

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
