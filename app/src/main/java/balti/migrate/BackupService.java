package balti.migrate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Vector;

/**
 * Created by sayantan on 15/10/17.
 */

public class BackupService extends Service {

    BroadcastReceiver cancelReceiver, progressBroadcast, requestProgress;
    IntentFilter cancelReceiverIF, progressBroadcastIF, requestProgressIF;

    int p;
    public static final String BACKUP_START_NOTIFICATION = "Backup start notification";
    public static final String BACKUP_END_NOTIFICATION = "Backup finished notification";
    public static final String BACKUP_RUNNING_NOTIFICATION = "Backup running notification";

    BackupEngine backupEngine = null;

    Intent toReturnIntent;

    static Vector<BackupBatch> batches;
    static String backupName, destination, busyboxBinaryFile;
    static Vector<ContactsDataPacket> contactsList;
    static Vector<CallsDataPacket> callsList;
    static Vector<SmsDataPacket> smsList;
    static String dpiText = "";
    static boolean doBackupContacts, doBackupCalls, doBackupSms, doBackupDpi;
    static Vector<File> backupSummaries;

    BroadcastReceiver triggerBatchBackupReceiver;

    SharedPreferences main;

    int runningBatchCount = 0;

    boolean cancelAll = false;

    @Override
    public void onCreate() {
        super.onCreate();

        main = getSharedPreferences("main", MODE_PRIVATE);

        triggerBatchBackupReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                runNextBatch();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(triggerBatchBackupReceiver, new IntentFilter("start batch backup"));
        toReturnIntent = new Intent("Migrate progress broadcast");
        progressBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra("type") && intent.getStringExtra("type").equals("finished")) {
                    if ((runningBatchCount+1) < batches.size() && intent.getStringArrayListExtra("errors").size() == 0) {
                        runningBatchCount += 1;
                        runNextBatch();
                    }
                }
                toReturnIntent = intent;
            }
        };
        progressBroadcastIF = new IntentFilter("Migrate progress broadcast");
        LocalBroadcastManager.getInstance(this).registerReceiver(progressBroadcast, progressBroadcastIF);
        cancelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    cancelAll = true;
                    if (backupEngine != null)
                    backupEngine.cancelProcess();
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

            NotificationChannel notificationChannelStart = new NotificationChannel(BACKUP_START_NOTIFICATION, BACKUP_START_NOTIFICATION, NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) Objects.requireNonNull(getSystemService(NOTIFICATION_SERVICE))).createNotificationChannel(notificationChannelStart);

            NotificationChannel notificationChannelEnd = new NotificationChannel(BACKUP_END_NOTIFICATION, BACKUP_END_NOTIFICATION, NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) Objects.requireNonNull(getSystemService(NOTIFICATION_SERVICE))).createNotificationChannel(notificationChannelEnd);

            NotificationChannel notificationChannelRunning = new NotificationChannel(BACKUP_RUNNING_NOTIFICATION, BACKUP_RUNNING_NOTIFICATION, NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) Objects.requireNonNull(getSystemService(NOTIFICATION_SERVICE))).createNotificationChannel(notificationChannelRunning);

            notification = new Notification.Builder(this, BACKUP_START_NOTIFICATION)
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

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("backup service started"));
    }

    void runNextBatch(){

        if (cancelAll) return;

        if (batches.size() == 0){

            File tempBackupSummary = new File(getFilesDir(), "backup_summary_part0");
            try {
                tempBackupSummary.delete();
                tempBackupSummary.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            backupEngine = new BackupEngine(backupName, 0, 0, main.getInt("compressionLevel", 0), destination, busyboxBinaryFile,
                    this,
                    0,
                    0,
                    tempBackupSummary);
        }
        else {
            backupEngine = new BackupEngine(backupName, runningBatchCount + 1, batches.size(), main.getInt("compressionLevel", 0), destination, busyboxBinaryFile,
                    this,
                    batches.get(runningBatchCount).batchSystemSize,
                    batches.get(runningBatchCount).batchDataSize,
                    backupSummaries.get(runningBatchCount));
        }
        if (runningBatchCount == 0)
            backupEngine.startBackup(doBackupContacts, contactsList, doBackupSms, smsList, doBackupCalls, callsList, doBackupDpi, dpiText);
        else backupEngine.startBackup(false, null, false, null, false, null, false, "");
    }

    static void setBackupBatches(Vector<BackupBatch> batches, String backupName, String destination, String busyboxBinaryFile,
                                 Vector<ContactsDataPacket> contactsList, boolean doBackupContacts,
                                 Vector<CallsDataPacket> callsList, boolean doBackupCalls,
                                 Vector<SmsDataPacket> smsList, boolean doBackupSms,
                                 String dpiText, boolean doBackupDpi,
                                 Vector<File> backupSummaries
                                 ){

        BackupService.batches = batches;
        BackupService.backupName = backupName;
        BackupService.destination = destination;
        BackupService.busyboxBinaryFile = busyboxBinaryFile;
        BackupService.contactsList = contactsList;
        BackupService.doBackupContacts = doBackupContacts;
        BackupService.callsList = callsList;
        BackupService.doBackupCalls = doBackupCalls;
        BackupService.smsList = smsList;
        BackupService.doBackupSms = doBackupSms;
        BackupService.dpiText = dpiText;
        BackupService.doBackupDpi = doBackupDpi;
        BackupService.backupSummaries = backupSummaries;

    }


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
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(triggerBatchBackupReceiver);
        } catch (Exception ignored){}
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
