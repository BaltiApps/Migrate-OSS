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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
    static String keyboardText = "";
    static boolean doBackupContacts, doBackupCalls, doBackupSms, doBackupDpi, doBackupKeyboard;

    BroadcastReceiver triggerBatchBackupReceiver;

    SharedPreferences main;

    int runningBatchCount = 0;

    boolean cancelAll = false;

    BufferedWriter progressWriter, errorWriter;
    String lastProgressLog = "";
    String lastZipLog = "";

    static long PREVIOUS_TIME = 0;

    static ArrayList<String> previousErrors;

    @Override
    public void onCreate() {
        super.onCreate();

        previousErrors = new ArrayList<>(0);

        main = getSharedPreferences("main", MODE_PRIVATE);

        try {

            progressWriter = new BufferedWriter(new FileWriter(new File(getExternalCacheDir(), "progressLog.txt")));
            errorWriter = new BufferedWriter(new FileWriter(new File(getExternalCacheDir(), "errorLog.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }

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

                toReturnIntent = intent;

                if (intent.hasExtra("type")) {

                    if (intent.getStringExtra("type").equals("finished")) {

                        ArrayList<String> errors = intent.getStringArrayListExtra("errors");
                        if (errors != null) previousErrors.addAll(errors);

                        if (intent.getBooleanExtra("final_process", false)) {
                            try {
                                progressWriter.write("\n\n" + intent.getStringExtra("finishedMessage") + "\n\n\n");

                                String backupName = intent.getStringExtra("backupName");

                                if (backupName != null) {
                                    progressWriter.write("--->> " + backupName + " <<---\n");
                                    errorWriter.write("\n\n--->> " + backupName + " <<---\n");
                                }
                                progressWriter.write("--- Total parts : " + batches.size() + " ---\n");
                                progressWriter.write("--- Migrate version " + context.getString(R.string.current_version_name) + " ---\n");

                                progressWriter.close();

                                ArrayList<String> finalErrors = intent.getStringArrayListExtra("allErrors");
                                if (finalErrors != null) {
                                    for (int i = 0; i < finalErrors.size(); i++) {
                                        try {
                                            errorWriter.write(finalErrors.get(i) + "\n");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                errorWriter.write("--- Migrate version " + context.getString(R.string.current_version_name) + " ---\n");
                                errorWriter.close();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            PREVIOUS_TIME += intent.getLongExtra("total_time", 0);
                        }


                        if ((runningBatchCount + 1) < batches.size()) {
                            runningBatchCount += 1;
                            runNextBatch();
                        }
                    } else if (intent.getStringExtra("type").equals("app_progress") && intent.hasExtra("app_log")
                            && !intent.getStringExtra("app_log").equals(lastProgressLog)) {

                        try {
                            progressWriter.write((lastProgressLog = intent.getStringExtra("app_log")) + "\n");
                        } catch (IOException ignored) {
                        }

                    } else if (intent.getStringExtra("type").equals("zip_progress") && intent.hasExtra("zip_log")
                            && !intent.getStringExtra("zip_log").equals(lastZipLog)) {

                        try {
                            progressWriter.write((lastZipLog = intent.getStringExtra("zip_log")) + "\n");
                        } catch (IOException ignored) {
                        }

                    } else if (intent.getStringExtra("type").equals("verifying_backups") && intent.hasExtra("app_name")) {

                        try {
                            progressWriter.write(intent.getStringExtra("app_name") + "\n");
                        } catch (IOException ignored) {
                        }

                    } else if (intent.getStringExtra("type").equals("correcting_errors") && intent.hasExtra("retry_log")) {

                        try {
                            progressWriter.write(intent.getStringExtra("retry_log") + "\n");
                        } catch (IOException ignored) {
                        }

                    }
                }
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

            NotificationChannel notificationChannelEnd = new NotificationChannel(BACKUP_END_NOTIFICATION, BACKUP_END_NOTIFICATION, NotificationManager.IMPORTANCE_HIGH);
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

    void runNextBatch() {

        if (cancelAll) return;

        if (batches.size() == 0) {

            /*File tempBackupSummary = new File(getFilesDir(), "backup_summary_part0");
            try {
                tempBackupSummary.delete();
                tempBackupSummary.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            BackupBatch tempBackupBatch = new BackupBatch(new Vector<BackupDataPacketWithSize>(0), 0, 0);

            backupEngine = new BackupEngine(tempBackupBatch, backupName, 0, 0, main.getInt("compressionLevel", 0),
                    destination, busyboxBinaryFile, this);
        } else {
            backupEngine = new BackupEngine(batches.get(runningBatchCount), backupName, runningBatchCount + 1,
                    batches.size(), main.getInt("compressionLevel", 0), destination, busyboxBinaryFile, this);
        }

        try {
            progressWriter.write("\n\n" + "--- Next batch backup: " + (runningBatchCount + 1) + " ---\n\n");
        } catch (IOException ignored) {
        }

        if (runningBatchCount == 0)
            backupEngine.startBackup(doBackupContacts, contactsList, doBackupSms, smsList, doBackupCalls, callsList, doBackupDpi, dpiText,
                    doBackupKeyboard, keyboardText);
        else backupEngine.startBackup(false, null, false, null,
                false, null, false, "", false, "");
    }

    static void setBackupBatches(Vector<BackupBatch> batches, String backupName, String destination, String busyboxBinaryFile,
                                 Vector<ContactsDataPacket> contactsList, boolean doBackupContacts,
                                 Vector<CallsDataPacket> callsList, boolean doBackupCalls,
                                 Vector<SmsDataPacket> smsList, boolean doBackupSms,
                                 String dpiText, boolean doBackupDpi,
                                 String keyboardText, boolean doBackupKeyboard
    ) {

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
        BackupService.keyboardText = keyboardText;
        BackupService.doBackupKeyboard = doBackupKeyboard;

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(cancelReceiver);
        } catch (Exception ignored) {
        }
        try {
            unregisterReceiver(cancelReceiver);
        } catch (Exception ignored) {
        }
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(progressBroadcast);
        } catch (Exception ignored) {
        }
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(requestProgress);
        } catch (Exception ignored) {
        }
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(triggerBatchBackupReceiver);
        } catch (Exception ignored) {
        }

        try {
            progressWriter.close();
        } catch (Exception ignored) {
        }
        try {
            errorWriter.close();
        } catch (Exception ignored) {
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
