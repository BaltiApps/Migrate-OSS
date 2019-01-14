package balti.migrate;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static balti.migrate.BackupService.BACKUP_END_NOTIFICATION;
import static balti.migrate.CommonTools.TEMP_DIR_NAME;
import static balti.migrate.MainActivity.THIS_VERSION;

/**
 * Created by sayantan on 9/10/17.
 */

public class BackupEngine {

    public static final int NOTIFICATION_ID = 100;

    private String destination, backupName;
    private String madePartName;
    private int partNumber, totalParts;
    private BackupBatch backupBatch;

    private Context context;
    private PackageManager pm;

    private SharedPreferences main;

    private Process suProcess = null;
    BufferedWriter suInputStream;

    private int compressionLevel;
    private int numberOfApps = 0;
    private String finalMessage = "";
    private ArrayList<String> errors;

    private String timeStamp = "";

    private String MIGRATE_STATUS = "migrate_status";

    private boolean doBackupContacts = false;
    private Vector<ContactsDataPacket> contactsDataPackets;
    private String contactsBackupName;

    private boolean doBackupSms = false;
    private Vector<SmsDataPacket> smsDataPackets;
    private String smsBackupName;

    private boolean doBackupCalls = false;
    private Vector<CallsDataPacket> callsDataPackets;
    private String callsBackupName;

    private boolean doBackupDpi = false;
    private String dpiText = "";
    private String dpiBackupName;

    private boolean doBackupKeyboard = false;
    private String keyboardText = "";
    private String keyboardBackupName;

    private Intent actualProgressBroadcast;

    private boolean isCancelled = false;
    private long startMillis;
    private long endMillis;

    private String errorTag;

    private String busyboxBinaryFilePath;
    //private String zipBinaryFilePath;

    StartBackup startBackupTask;

    private long systemRequiredSize = 0;
    private long dataRequiredSize = 0;

    private CommonTools commonTools;

    //private File backupSummary;

    static String ICON_STRING = "";
    private static int PID = -9999999;
    private static int CORRECTING_PID = -9999999;

    class StartBackup extends AsyncTask {

        boolean doBackupContacts;
        Vector<ContactsDataPacket> contactsDataPackets;

        boolean doSmsBackup;
        Vector<SmsDataPacket> smsDataPackets;

        boolean doBackupCalls;
        Vector<CallsDataPacket> callsDataPackets;

        boolean doBackupDpi;
        String dpiText;

        boolean doBackupKeyboard;
        String keyboardText;

        public StartBackup(boolean doContactsBackup, Vector<ContactsDataPacket> contactsDataPackets, boolean doSmsBackup, Vector<SmsDataPacket> smsDataPackets,
                           boolean doBackupCalls, Vector<CallsDataPacket> callsDataPackets, boolean doBackupDpi, String dpiText, boolean doBackupKeyboard, String keyboardText) {
            this.doBackupContacts = doContactsBackup;
            this.contactsDataPackets = contactsDataPackets;
            this.doSmsBackup = doSmsBackup;
            this.smsDataPackets = smsDataPackets;
            this.doBackupCalls = doBackupCalls;
            this.callsDataPackets = callsDataPackets;
            this.doBackupDpi = doBackupDpi;
            this.dpiText = dpiText;
            this.doBackupKeyboard = doBackupKeyboard;
            this.keyboardText = keyboardText;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            setDoContactsBackup(doBackupContacts, contactsDataPackets);
            setDoSmsBackup(doSmsBackup, smsDataPackets);
            setDoCallsBackup(doBackupCalls, callsDataPackets);
            setDoDpiBackup(doBackupDpi, dpiText);
            setDoKeyboardBackup(doBackupKeyboard, keyboardText);
            initiateBackup();
            return null;
        }
    }

    BackupEngine(BackupBatch backupBatch, String backupName, int partNumber, int totalParts, int compressionLevel, String destination, String busyboxBinaryFilePath,
                 Context context) {

        this.backupBatch = backupBatch;

        this.backupName = backupName;
        this.destination = destination;

        String actualBackupName = backupName;


        this.busyboxBinaryFilePath = busyboxBinaryFilePath;

        this.systemRequiredSize = backupBatch.batchSystemSize;
        this.dataRequiredSize = backupBatch.batchDataSize;
        this.numberOfApps = backupBatch.appCount;
        //this.backupSummary = backupSummary;

        ICON_STRING = "";

        this.partNumber = partNumber;
        this.totalParts = totalParts;

        if (totalParts > 1) {
            this.destination = this.destination + "/" + this.backupName;
            this.madePartName = context.getString(R.string.part) + " " + partNumber + " " + context.getString(R.string.of) + " " + totalParts;
            this.backupName = this.madePartName.replace(" ", "_");
        } else {
            this.madePartName = "";
        }

        this.errorTag = "[" + partNumber + "/" + totalParts + "]";

        this.compressionLevel = compressionLevel;
        (new File(this.destination)).mkdirs();
        this.context = context;

        assert this.backupName != null;
        if (this.backupName.endsWith(".zip"))
            this.backupName = this.backupName.substring(0, this.backupName.lastIndexOf("."));

        main = context.getSharedPreferences("main", Context.MODE_PRIVATE);
        actualProgressBroadcast = new Intent("Migrate progress broadcast")
                .putExtra("part_name", this.madePartName).putExtra("backupName", actualBackupName)
                .putExtra("total_parts", totalParts);

        errors = new ArrayList<>(0);

        //zipBinaryFilePath = "";

        timeStamp = "_" + new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Calendar.getInstance().getTime());

        contactsBackupName = "Contacts" + timeStamp + ".vcf";
        smsBackupName = "Sms" + timeStamp + ".sms.db";
        callsBackupName = "Calls" + timeStamp + ".calls.db";
        dpiBackupName = "screen.dpi";
        keyboardBackupName = "default.kyb";

        commonTools = new CommonTools(context);
        pm = context.getPackageManager();
    }

    NotificationCompat.Builder createNotificationBuilder() {
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(context, BackupService.BACKUP_RUNNING_NOTIFICATION);
        } else {
            builder = new NotificationCompat.Builder(context);
        }
        return builder;
    }

    long timeInMillis() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getTimeInMillis();
    }

    void startBackup(boolean doBackupContacts, Vector<ContactsDataPacket> contactsDataPackets, boolean doBackupSms, Vector<SmsDataPacket> smsDataPackets,
                     boolean doBackupCalls, Vector<CallsDataPacket> callsDataPackets, boolean doBackupDpi, String dpiText, boolean doBackupKeyboard, String keyboardText) {
        try {
            startBackupTask.cancel(true);
        } catch (Exception ignored) {
        }
        startBackupTask = new StartBackup(doBackupContacts, contactsDataPackets, doBackupSms, smsDataPackets, doBackupCalls, callsDataPackets, doBackupDpi, dpiText, doBackupKeyboard, keyboardText);
        startBackupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    String moveFile(File source, File destination) {
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(source));
            FileOutputStream fileOutputStream = new FileOutputStream(destination);

            byte[] buffer = new byte[2048];
            int read;

            while ((read = bufferedInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, read);
            }
            fileOutputStream.close();
            source.delete();
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    void initiateBackup() {

        startMillis = timeInMillis();

        errors = new ArrayList<>(0);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder progressNotif = createNotificationBuilder();

        notificationManager.cancel(NOTIFICATION_ID + 1);
        progressNotif.setSmallIcon(R.drawable.ic_notification_icon)
                .setContentTitle(context.getString(R.string.backingUp));

        Intent cancelIntent = new Intent("Migrate backup cancel broadcast");
        Intent activityProgressIntent = new Intent(context, BackupProgressLayout.class)
                .putExtra("part_number", this.partNumber).putExtra("total_parts", this.totalParts)
                .putExtra("part_name", this.madePartName);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, 53, cancelIntent, 0);

        progressNotif.setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationCompat.Action cancelAction = new NotificationCompat.Action(0, context.getString(android.R.string.cancel), cancelPendingIntent);

        try {


            if (partNumber == 1) {

                // delete previous backup scripts

                File previousBackupScripts[] = context.getFilesDir().listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.getName().startsWith("the_backup_script_") && f.getName().endsWith(".sh");
                    }
                });

                for (File f : previousBackupScripts)
                    f.delete();

                previousBackupScripts = context.getExternalCacheDir().listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.getName().startsWith("the_backup_script_") && f.getName().endsWith(".sh");
                    }
                });

                for (File f : previousBackupScripts)
                    f.delete();
            }

            String scriptFilePath = makeScripts(notificationManager, progressNotif, activityProgressIntent);

            if (isCancelled) throw new InterruptedIOException();
            if (scriptFilePath != null) {

                progressNotif.addAction(cancelAction);

                makePackageData();
                makeExtrasData();


                String makePackageFlashReadyErr = makePackageFlashReady(notificationManager, progressNotif, activityProgressIntent);
                if (!makePackageFlashReadyErr.equals("")) errors.add(makePackageFlashReadyErr);

                String contactsErr = backupContacts(notificationManager, progressNotif, activityProgressIntent);
                if (!contactsErr.equals("")) errors.add("CONTACTS" + errorTag + ": " + contactsErr);

                String smsErr = backupSms(notificationManager, progressNotif, activityProgressIntent);
                if (!smsErr.equals("")) errors.add("SMS" + errorTag + ": " + smsErr);

                String callsErr = backupCalls(notificationManager, progressNotif, activityProgressIntent);
                if (!callsErr.equals("")) errors.add("CALLS" + errorTag + ": " + callsErr);

                String dpiErr = backupDpi(notificationManager, progressNotif, activityProgressIntent);
                if (!dpiErr.equals("")) errors.add("DPI" + errorTag + ": " + dpiErr);

                String keybErr = backupKeyboard(notificationManager, progressNotif, activityProgressIntent);
                if (!keybErr.equals("")) errors.add("KEYBOARD" + errorTag + ": " + keybErr);

                backupAppPermissions(notificationManager, progressNotif, activityProgressIntent);

                if (isCancelled)
                    throw new InterruptedIOException();

                int c = 0, p = 100;

                try {

                    File scriptFile = new File(scriptFilePath);

                    suProcess = Runtime.getRuntime().exec("su");
                    suInputStream = new BufferedWriter(new OutputStreamWriter(suProcess.getOutputStream()));
                    BufferedReader outputStream = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));
                    BufferedReader errorStream = new BufferedReader(new InputStreamReader(suProcess.getErrorStream()));

                    suInputStream.write("sh " + scriptFile.getAbsolutePath() + "\n");
                    suInputStream.write("exit\n");
                    suInputStream.flush();

                    String line;

                    String iconString;

                    while ((line = outputStream.readLine()) != null) {

                        if (isCancelled) throw new InterruptedIOException();

                        line = line.trim();

                        actualProgressBroadcast.putExtra("type", "app_progress");

                        if (line.startsWith("--- PID:")) {
                            try {
                                PID = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (line.startsWith(MIGRATE_STATUS)) {
                            line = line.substring(16);

                            if (line.contains("icon:")) {

                                iconString = line.substring(line.lastIndexOf(' ') + 1);
                                line = line.substring(0, line.indexOf("icon:"));
                                iconString = iconString.trim();

                                ICON_STRING = iconString;

                                actualProgressBroadcast.putExtra("app_name", line);
                            } else {
                                actualProgressBroadcast.putExtra("app_name", line);
                            }

                            if (numberOfApps != 0)
                                p = ++c * 100 / numberOfApps;
                            actualProgressBroadcast.putExtra("progress", p);

                            LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                            activityProgressIntent.putExtras(actualProgressBroadcast);

                            String title = (totalParts > 1) ? context.getString(R.string.backingUp) + " : " + madePartName : context.getString(R.string.backingUp);
                            progressNotif.setContentTitle(title)
                                    .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                                    .setProgress(numberOfApps, c, false)
                                    .setContentText(line);
                            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());
                        }

                        actualProgressBroadcast.putExtra("app_log", line);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                        if (line.equals("--- App files copied ---"))
                            break;

                    }

                    try {
                        suProcess.waitFor();
                    } catch (Exception ignored) {
                    }

                    while ((line = errorStream.readLine()) != null) {
                        line = line.trim();
                        if (!line.endsWith("socket ignored") && !line.endsWith("No such file or directory")) {
                            errors.add("RUN" + errorTag + ": " + line);
                        }
                        else {
                            errors.add("SUPPRESSED_RUN" + errorTag + ": " + line);
                        }
                    }

                    if (!isCancelled && numberOfApps > 0) {

                        ArrayList<String> defects = verifyBackups(notificationManager, progressNotif, activityProgressIntent);
                        if (!isCancelled)
                            tryingToCorrect(defects, notificationManager, progressNotif, activityProgressIntent);

                    }

                } catch (Exception e) {
                    if (!isCancelled) {
                        e.printStackTrace();
                        errors.add("RUN_CODE_ERROR" + errorTag + ": " + e.getMessage());
                    }
                }


            } else {
                errors.add(context.getString(R.string.errorMakingScripts));
            }

            if (!isCancelled) {
                String zipErr = javaZipAll(notificationManager, progressNotif, activityProgressIntent);
                if (!zipErr.trim().equals("")) errors.add("ZIP" + errorTag + ": " + zipErr);
            }

        } catch (Exception e) {
            if (!isCancelled) {
                e.printStackTrace();
                errors.add("INIT" + errorTag + ": " + e.getMessage());
            }
        }

        progressNotif = createNotificationBuilder();
        progressNotif.setSmallIcon(R.drawable.ic_notification_icon);

        endMillis = timeInMillis();

        boolean finalProcess = (partNumber == totalParts) || isCancelled;

        actualProgressBroadcast.putExtra("part_name", "");

        actualProgressBroadcast.putExtra("type", "finished").putExtra("final_process", finalProcess);

        actualProgressBroadcast.putStringArrayListExtra("errors", errors);

        if (finalProcess) {

            actualProgressBroadcast.putExtra("complete_time", BackupService.PREVIOUS_TIME + (endMillis - startMillis));

            ArrayList<String> allErr = new ArrayList<>(0);
            if (BackupService.previousErrors != null)
                allErr.addAll(BackupService.previousErrors);
            allErr.addAll(errors);

            boolean isErr = false;
            String toShowError = "";
            for (String e : allErr) {
                if (!e.startsWith("SUPPRESSED")) {
                    toShowError = e;
                    isErr = true;
                    break;
                }
            }

            if (isCancelled) finalMessage = context.getString(R.string.backupCancelled);
            else if (!isErr) finalMessage = context.getString(R.string.noErrors);
            else {
                if (partNumber == totalParts)
                    finalMessage = context.getString(R.string.backupFinishedWithErrors);
                else
                    finalMessage = context.getString(R.string.backupPartHadErrors);
                progressNotif.setContentText(toShowError);
            }

            actualProgressBroadcast.putExtra("finishedMessage", finalMessage.trim()).putStringArrayListExtra("allErrors", allErr);
            activityProgressIntent.putExtras(actualProgressBroadcast);

            progressNotif
                    .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentTitle(finalMessage)
                    .setProgress(0, 0, false)
                    .setAutoCancel(true)
                    .setChannelId(BACKUP_END_NOTIFICATION)
                    .mActions.clear();

            notificationManager.cancel(NOTIFICATION_ID);
            notificationManager.notify(NOTIFICATION_ID + 1, progressNotif.build());

        } else {
            actualProgressBroadcast.putExtra("total_time", endMillis - startMillis);
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

        if (finalProcess)
            context.stopService(new Intent(context, BackupService.class));
    }

    ArrayList<String> verifyBackups(NotificationManager notificationManager, NotificationCompat.Builder progressNotif, Intent activityProgressIntent) {

        ArrayList<String> allRecovery = new ArrayList<>(0);

        if (numberOfApps == 0)
            return allRecovery;

        String title = (totalParts > 1) ?
                context.getString(R.string.verifying_backups) + " : " + madePartName : context.getString(R.string.verifying_backups);

        actualProgressBroadcast.putExtra("type", "verifying_backups");
        actualProgressBroadcast.putExtra("app_name", "");
        actualProgressBroadcast.putExtra("progress", 0);
        activityProgressIntent.putExtras(actualProgressBroadcast);

        progressNotif.setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setProgress(0, 0, false)
                .setContentText("");
        notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

        int c = 0;

        try {
            for (BackupDataPacketWithSize packetWithSize : backupBatch.appListWithSize) {

                if (isCancelled) break;

                BackupDataPacket packet = packetWithSize.packet;

                PackageInfo pi = packet.PACKAGE_INFO;

                String dataName = pi.packageName + ".tar.gz";
                String permName = pi.packageName + ".perm";

                File backupData = new File(destination + "/" + backupName + "/" + dataName);
                File backupPerm = new File(destination + "/" + backupName + "/" + permName);

                String backupApkDirPath = destination + "/" + backupName + "/" + pi.packageName + ".app";

                if (packet.APP && (!new File(backupApkDirPath).exists() || commonTools.getDirLength(backupApkDirPath) == 0)) {

                    String apkPath = pi.applicationInfo.sourceDir;
                    String apkName = apkPath.substring(apkPath.lastIndexOf('/') + 1);
                    apkPath = apkPath.substring(0, apkPath.lastIndexOf('/'));
                    if (apkPath.endsWith(":")) apkPath = apkPath.substring(0, apkPath.length()-1);

                    apkName = commonTools.applyNamingCorrectionForShell(apkName);

                    allRecovery.add("echo \"Copy apk(s): " + pi.packageName + "\"\n" +
                            "mkdir -p " + backupApkDirPath + "\n" +
                            "cd " + apkPath + "\n" +
                            "cp *.apk " + backupApkDirPath + "/\n\n" +
                            "mv " + backupApkDirPath + "/" + apkName + " " + backupApkDirPath + "/" + pi.packageName + ".apk\n"
                    );

                }

                if (packet.DATA && (!backupData.exists() || backupData.length() == 0)) {

                    String fullDataPath = pi.applicationInfo.dataDir;
                    String actualDataName = fullDataPath.substring(fullDataPath.lastIndexOf('/') + 1);
                    String dataPathParent = fullDataPath.substring(0, fullDataPath.lastIndexOf('/'));

                    String backupDataCommand = "" +
                            "if [ -e " + fullDataPath + " ]; then\n" +
                            "   cd " + dataPathParent + "\n" +
                            "   echo \"Copy data: " + dataName + "\" && " + busyboxBinaryFilePath + " tar -vczpf " + backupData.getAbsolutePath() + " " + actualDataName + "\n" +
                            "fi\n\n";

                    allRecovery.add(backupDataCommand);
                }

                if (packet.PERMISSIONS && (!backupPerm.exists() || backupPerm.length() == 0)) {
                    writeGrantedPermissions(packet.PACKAGE_INFO.packageName, true);
                }

                String appName = pm.getApplicationLabel(pi.applicationInfo).toString();

                actualProgressBroadcast.putExtra("type", "verifying_backups")
                        .putExtra("app_name", "verifying: " + appName)
                        .putExtra("progress", (++c * 100 / numberOfApps));
                activityProgressIntent.putExtras(actualProgressBroadcast);

                progressNotif.setProgress(numberOfApps, c, false)
                        .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                        .setContentText(appName);
                notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return allRecovery;
    }

    void tryingToCorrect(ArrayList<String> defects, NotificationManager notificationManager, NotificationCompat.Builder progressNotif, Intent activityProgressIntent) {


        if (numberOfApps == 0 || defects.size() == 0)
            return;


        String title = (totalParts > 1) ?
                context.getString(R.string.correcting_errors) + " : " + madePartName : context.getString(R.string.correcting_errors);

        actualProgressBroadcast.putExtra("type", "correcting_errors");
        actualProgressBroadcast.putExtra("defect_number", defects.size());
        actualProgressBroadcast.putExtra("progress", 0);
        activityProgressIntent.putExtras(actualProgressBroadcast);

        progressNotif.setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setProgress(0, 0, false)
                .setContentText("");
        notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

        File retryScript = new File(context.getFilesDir(), "retry_script_" + partNumber + ".sh");
        try {
            BufferedWriter scriptWriter = new BufferedWriter(new FileWriter(retryScript));

            scriptWriter.write("#!sbin/sh\n\n");
            scriptWriter.write("echo \" \"\n");
            scriptWriter.write("sleep 1\n");
            scriptWriter.write("echo \"--- RECOVERY PID: $$\"\n");
            scriptWriter.write("cp " + retryScript.getAbsolutePath() + " " + context.getExternalCacheDir() + "/\n");

            int totalRemainingDefects = defects.size();

            for (String defect : defects) {

                if (isCancelled) break;

                scriptWriter.write("echo \"--- DEFECT: " + totalRemainingDefects + "\"\n");
                scriptWriter.write(defect);
                totalRemainingDefects--;
            }

            scriptWriter.write("echo \"--- Retry complete ---\"\n");
            scriptWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {

            suProcess = Runtime.getRuntime().exec("su");
            suInputStream = new BufferedWriter(new OutputStreamWriter(suProcess.getOutputStream()));
            BufferedReader outputStream = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));
            BufferedReader errorStream = new BufferedReader(new InputStreamReader(suProcess.getErrorStream()));


            suInputStream.write("sh " + retryScript.getAbsolutePath() + "\n");
            suInputStream.write("exit\n");
            suInputStream.flush();

            String line;

            int done = 0;

            while ((line = outputStream.readLine()) != null) {

                if (isCancelled) throw new InterruptedIOException();

                line = line.trim();

                actualProgressBroadcast.putExtra("type", "correcting_errors");

                if (line.startsWith("--- RECOVERY PID:")) {
                    try {
                        CORRECTING_PID = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (line.startsWith("--- DEFECT:")) {
                    try {

                        int defectNumber = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));

                        done = defects.size() - defectNumber;
                        actualProgressBroadcast.putExtra("defect_number", defectNumber);
                        actualProgressBroadcast.putExtra("progress", (done * 100) / defects.size());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                activityProgressIntent.putExtras(actualProgressBroadcast);

                progressNotif.setProgress(defects.size(), done, false)
                        .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT));
                notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                actualProgressBroadcast.putExtra("retry_log", line);
                LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                if (line.equals("--- Retry complete ---"))
                    break;

            }


            try {
                suProcess.waitFor();
            } catch (Exception ignored) {
            }

            while ((line = errorStream.readLine()) != null) {
                line = line.trim();
                if (!line.endsWith("socket ignored")) {
                    errors.add("RETRY" + errorTag + ": " + line);
                }
                else {
                    errors.add("SUPPRESSED_RETRY" + errorTag + ": " + line);
                }
            }

        } catch (Exception e) {
            if (!isCancelled) {
                e.printStackTrace();
                errors.add("RETRY_CODE_ERROR" + errorTag + ": " + e.getMessage());
            }
        }

    }

    /*private String zipAll(NotificationManager notificationManager, NotificationCompat.Builder progressNotif, Intent activityProgressIntent){

        String err = "";
        try {

            String title = (totalParts > 1)? context.getString(R.string.combining) + " : " + madePartName : context.getString(R.string.combining);
            progressNotif.setContentTitle(title)
                    .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setProgress(0, 0, false)
                    .setContentText("");
            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            getNumberOfFiles = Runtime.getRuntime().exec("ls -l " + destination + "/" + backupName);

            BufferedReader outputReader = new BufferedReader(new InputStreamReader(getNumberOfFiles.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(getNumberOfFiles.getErrorStream()));

            getNumberOfFiles.waitFor();
            String l; int n = 0;
            while (outputReader.readLine() != null){
                ++n;
            }
            --n;
            while ((l = errorReader.readLine()) != null){
                err = err + l + "\n";
            }

            (new File(destination + "/" + backupName + ".zip")).delete();

            File tempScript = new File(context.getFilesDir().getAbsolutePath() + "/tempScript.sh");
            String cmds = "#!/sbin/sh\n\n" +
                    "cd " + destination + "/" + backupName + "\n" +
                    zipBinaryFilePath + " " + backupName + ".zip -" + compressionLevel + "vurm *\n" +
                    "mv " + backupName + ".zip " + destination + "\n" +
                    "rm -r " + destination + "/" + backupName + "\n" +
                    "rm " + tempScript.getAbsolutePath() + "\n";

            BufferedWriter writer = new BufferedWriter(new FileWriter(tempScript));
            BufferedReader reader = new BufferedReader(new StringReader(cmds));
            while ((l = reader.readLine()) != null){
                writer.write(l + "\n");
            }
            writer.close();

            zipProcess = Runtime.getRuntime().exec("sh " + tempScript.getAbsolutePath());

            outputReader = new BufferedReader(new InputStreamReader(zipProcess.getInputStream()));
            errorReader = new BufferedReader(new InputStreamReader(zipProcess.getErrorStream()));

            String last = "";
            int c = 0;
            while ((l = outputReader.readLine()) != null) {
                l = l.trim();
                if (l.startsWith("adding:")) {
                    if (l.indexOf('/') != -1) {
                        if (!l.substring(0, l.indexOf('/')).equals(last)) {
                            last = l.substring(0, l.indexOf('/'));
                            ++c;
                        }
                    } else {
                        if (!l.substring(0, l.lastIndexOf(' ')).equals(last)) {
                            last = l.substring(0, l.lastIndexOf(' '));
                            ++c;
                        }
                    }
                }

                if (l.contains("/dev/fd/")) {
                    err = err + l + "\n";
                    zipProcess.destroy();
                    return err;
                }

                if (l.startsWith("zip I/O error:") || l.startsWith("zip error:"))
                    err = err + l + "\n";

                int pr = c*100 / n;
                if (pr == 100) pr = 99;


                actualProgressBroadcast.putExtra("type", "zip_progress")
                        .putExtra("progress", pr)
                        .putExtra("zip_log", l);

                activityProgressIntent.putExtras(actualProgressBroadcast);

                LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                progressNotif.setProgress(n, c, false)
                        .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT));

                notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            }

            while ((l = errorReader.readLine()) != null){
                err = err + l + "\n";
            }

        }
        catch (Exception e) {
            if (!isCancelled)
                err = err + e.getMessage() + "\n";
        }
        return err;
    }*/

    private String javaZipAll(NotificationManager notificationManager, NotificationCompat.Builder progressNotif, Intent activityProgressIntent) {

        String err = "";
        try {

            String title = (totalParts > 1) ? context.getString(R.string.combining) + " : " + madePartName : context.getString(R.string.combining);
            progressNotif.setContentTitle(title)
                    .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setProgress(0, 0, false)
                    .setContentText("");
            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            (new File(destination + "/" + backupName + ".zip")).delete();
            int c = 0;

            File directory = new File(destination + "/" + backupName);
            Vector<File> files = getAllFiles(directory, new Vector<File>(0));
            int n = files.size();

            File zipFile = new File(destination + "/" + backupName + ".zip");

            FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

            //int part = 0;

            for (File file : files) {

                if (isCancelled) break;

                String fn = file.getAbsolutePath();
                fn = fn.substring(directory.getAbsolutePath().length() + 1);
                ZipEntry zipEntry;


                if (file.isDirectory()) {
                    zipEntry = new ZipEntry(fn + "/");

                    zipOutputStream.putNextEntry(zipEntry);
                    zipOutputStream.closeEntry();
                    c++;
                    continue;
                } else {
                    zipEntry = new ZipEntry(fn);

                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                    byte[] buffer = new byte[4096];
                    int read;

                    CRC32 crc32 = new CRC32();

                    while ((read = bis.read(buffer)) > 0) {
                        crc32.update(buffer, 0, read);
                    }

                    bis.close();

                    zipEntry.setSize(file.length());
                    zipEntry.setCompressedSize(file.length());
                    zipEntry.setCrc(crc32.getValue());
                    zipEntry.setMethod(ZipEntry.STORED);

                    zipOutputStream.putNextEntry(zipEntry);

                    FileInputStream fileInputStream = new FileInputStream(file);
                    buffer = new byte[4096];

                    while ((read = fileInputStream.read(buffer)) > 0) {
                        zipOutputStream.write(buffer, 0, read);
                    }
                    zipOutputStream.closeEntry();
                    fileInputStream.close();
                    file.delete();
                }

                /*if (zipFile.length() >= 1000000){
                    zipOutputStream.close();
                    zipFile = new File(destination + "/" + backupName + "_part" + ++part + ".zip");
                    fileOutputStream = new FileOutputStream(zipFile);
                    zipOutputStream = new ZipOutputStream(fileOutputStream);
                }*/

                int pr = c++ * 100 / n;
                if (pr == 100) pr = 99;

                actualProgressBroadcast.putExtra("type", "zip_progress")
                        .putExtra("zip_log", "zipped: " + file.getName())
                        .putExtra("progress", pr);

                activityProgressIntent.putExtras(actualProgressBroadcast);

                LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);


                progressNotif.setProgress(n, c, false)
                        .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT));

                notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            }
            zipOutputStream.close();
            fullDelete(directory.getAbsolutePath());

            if (c < n) {
                err += context.getString(R.string.incompleteZip) + "\n";
            }

        } catch (Exception e) {
            if (!isCancelled)
                err = err + e.getMessage() + "\n";
        }
        return err;
    }

    Vector<File> getAllFiles(File directory, Vector<File> allFiles) {
        File files[] = directory.listFiles();
        for (File f : files) {
            if (f.isFile())
                allFiles.addElement(f);
            else {
                allFiles.addElement(f);
                getAllFiles(f, allFiles);
            }
        }
        return allFiles;
    }

    void makePackageData() {

        File package_data = new File(destination + "/" + backupName + "/package-data.txt");
        String contents = "";

        package_data.getParentFile().mkdirs();

        contents += "backup_name " + backupName + "\n";
        contents += "timestamp " + timeStamp + "\n";
        contents += "device " + Build.DEVICE + "\n";
        contents += "sdk " + Build.VERSION.SDK_INT + "\n";
        contents += "cpu_abi " + Build.SUPPORTED_ABIS[0] + "\n";
        contents += "data_required_size " + dataRequiredSize + "\n";
        contents += "system_required_size " + systemRequiredSize + "\n";
        contents += "migrate_version " + context.getString(R.string.current_version_name) + "\n";

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(package_data));
            writer.write(contents);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void makeExtrasData() {

        File package_data = new File(destination + "/" + backupName + "/extras-data");
        String contents = "";

        package_data.getParentFile().mkdirs();

        if (doBackupContacts) {
            contents += contactsBackupName + "\n";
        }
        if (doBackupSms) {
            contents += smsBackupName + "\n";
        }
        if (doBackupCalls) {
            contents += callsBackupName + "\n";
        }
        if (doBackupDpi) {
            contents += dpiBackupName + "\n";
        }
        if (doBackupKeyboard) {
            contents += keyboardBackupName + "\n";
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(package_data));
            writer.write(contents);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void systemAppInstallScript(String sysAppPackageName, String sysAppPastingDir, String appDir) {

        String scriptName = sysAppPackageName + ".sh";
        String scriptLocation = destination + "/" + backupName + "/" + scriptName;
        File script = new File(scriptLocation);

        String scriptText = "#!sbin/sh\n\n";

        scriptText = scriptText +
                "mkdir -p " + sysAppPastingDir + "\n" +
                "cd " + sysAppPastingDir + "\n" +
                "mv /tmp/" + appDir + "/*.apk " + sysAppPastingDir + "/" + "\n" +
                "cd /tmp/" + "\n" +
                "rm -rf " + appDir + "\n" +
                "rm -rf " + scriptName + "\n";
        ;


        (new File(destination + "/" + backupName)).mkdirs();

        BufferedReader reader = new BufferedReader(new StringReader(scriptText));

        try {
            script.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(script));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line + '\n');
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        script.setExecutable(true, false);
    }

    void makeMetadataFile(String appName, String packageName, String apkName, String dataName, String icon, String version, boolean permissions) {
        String metadataFileName = packageName + ".json";
        String metadataLocation = destination + "/" + backupName + "/" + metadataFileName;
        File metadataFile = new File(metadataLocation);

        if (!apkName.equals("NULL"))
            apkName = apkName + ".apk";

        if (!dataName.equals("NULL"))
            dataName = dataName + ".tar.gz";

        String metadataContent = "" +
                "{\n" +
                "   \"app_name\" : \"" + appName + "\",\n" +
                "   \"package_name\" : \"" + packageName + "\",\n" +
                "   \"apk\" : \"" + apkName + "\",\n" +
                "   \"data\" : \"" + dataName + "\",\n" +
                "   \"icon\" : \"" + icon + "\",\n" +
                "   \"version\" : \"" + version + "\",\n" +
                "   \"permissions\" : " + permissions + "\n" +
                "}\n";

        (new File(destination + "/" + backupName)).mkdirs();

        BufferedReader reader = new BufferedReader(new StringReader(metadataContent));

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(metadataFile));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line + '\n');
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String byteToString(byte[] bytes) {
        StringBuilder res = new StringBuilder();
        for (byte b : bytes) {
            res.append(b).append("_");
        }
        return res.toString();
    }

    private Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    String makeScripts(NotificationManager notificationManager, NotificationCompat.Builder progressNotif, Intent activityProgressIntent) {

        //zipBinaryFilePath = commonTools.unpackAssetToInternal("zip", "zip");

        /*if (busyboxBinaryFilePath.equals("") || zipBinaryFilePath.equals(""))
            return new File[]{null, null};*/

        //File pre_script = new File(context.getFilesDir(), "pre_script.sh");
        //File helper = new File(context.getFilesDir() + "/system/app/MigrateHelper", "MigrateHelper.apk");
        //String verifyScript = commonTools.unpackAssetToInternal("verify.sh", "verify.sh");
        /*AssetManager assetManager = context.getAssets();
        int read;
        byte buffer[] = new byte[4096];

        (new File(helper.getAbsolutePath().substring(0, helper.getAbsolutePath().lastIndexOf('/')))).mkdirs();

        try {
            InputStream inputStream = assetManager.open("helper.apk");
            FileOutputStream writer = new FileOutputStream(helper);
            while ((read = inputStream.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        String title = (totalParts > 1) ? context.getString(R.string.reading_data) + " : " + madePartName : context.getString(R.string.reading_data);

        actualProgressBroadcast.putExtra("type", "reading_backup_data");
        actualProgressBroadcast.putExtra("app_name", "");
        actualProgressBroadcast.putExtra("progress", 0);
        activityProgressIntent.putExtras(actualProgressBroadcast);

        progressNotif.setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setProgress(0, 0, false)
                .setContentText("");
        notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

        if (busyboxBinaryFilePath.equals("")) {
            errors.add("EMPTY_BUSYBOX_FILEPATH");
            return null;
        }

        (new File(destination + "/" + backupName)).mkdirs();

        File scriptFile = new File(context.getFilesDir(), "the_backup_script_" + partNumber + ".sh");
        File updater_script = new File(context.getExternalCacheDir(), "updater-script");

        String appAndDataBackupScript = commonTools.unpackAssetToInternal("backup_app_and_data.sh", "backup_app_and_data.sh", false);


        try {
            //BufferedWriter pre_writer = new BufferedWriter(new FileWriter(pre_script));

            //BufferedReader backupSummaryReader = new BufferedReader(new FileReader(backupSummary));
            BufferedWriter updater_writer = new BufferedWriter(new FileWriter(updater_script));

            updater_writer.write("show_progress(0, 0);\n");
            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"---------------------------------\");\n");
            updater_writer.write("ui_print(\"      Migrate Flash package      \");\n");
            updater_writer.write("ui_print(\"---------------------------------\");\n");
            if (!madePartName.trim().equals("")) {
                updater_writer.write("ui_print(\"*** " + madePartName + " ***\");\n");
                updater_writer.write("ui_print(\"---------------------------------\");\n");
            }

            updater_writer.write("package_extract_file(\"" + "busybox" + "\", \"" + "/tmp/busybox" + "\");\n");
            updater_writer.write("package_extract_file(\"" + "mount_using_self_busybox.sh" + "\", \"" + "/tmp/mount_using_self_busybox.sh" + "\");\n");
            updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + "/tmp/mount_using_self_busybox.sh" + "\");\n");

            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Mounting partition...\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"mount\", \"/system\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"mount\", \"/data\");\n");

            updater_writer.write("ifelse(is_mounted(\"/data\") && is_mounted(\"/system\"), ui_print(\"\"), " +
                    "ui_print(\"Mounting using self busybox...\") && " +
                    "run_program(\"/tmp/mount_using_self_busybox.sh\", \"m\"));\n");

            updater_writer.write("ifelse(is_mounted(\"/data\") && is_mounted(\"/system\"), ui_print(\"Mounted!\"), abort(\"Mount failed! Exiting...\"));\n");

            /*updater_writer.write("package_extract_file(\"helper.apk\", \"/tmp/helper.apk\");\n");
            updater_writer.write("set_perm_recursive(0, 0, 0777, 0777, \"" + "/tmp/helper.apk" + "\");\n");*/

            updater_writer.write("package_extract_file(\"" + "prep.sh" + "\", \"" + "/tmp/prep.sh" + "\");\n");
            updater_writer.write("package_extract_file(\"" + "package-data.txt" + "\", \"" + "/tmp/package-data.txt" + "\");\n");
            updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + "/tmp/prep.sh" + "\");\n");
            updater_writer.write("run_program(\"/tmp/prep.sh\", \"" + TEMP_DIR_NAME + "\", \"" + timeStamp + "\");\n");

            updater_writer.write("ifelse(is_mounted(\"/data\"), ui_print(\"Parameters checked!\") && sleep(2s), abort(\"Exiting...\"));\n");
            updater_writer.write("ui_print(\" \");\n");

            updater_writer.write("ui_print(\"Restoring to Migrate cache...\");\n");
            updater_writer.write("ui_print(\" \");\n");

            /*try {

                pre_writer.write("echo \"" + MIGRATE_STATUS + ": " + "Making package Flash Ready" + "\"\n");
                pre_writer.write("mkdir -p " + flashDirPath + "\n");
                pre_writer.write("mv " + updater_script.getAbsolutePath() + " " + flashDirPath + "\n");
                pre_writer.write("mv " + update_binary + " " + flashDirPath + "\n");
                pre_writer.write("mv " + prepScript + " " + destination + "/" + backupName + "\n");
                pre_writer.write("mv " + verifyScript + " " + destination + "/" + backupName + "\n");

                pre_writer.write("echo \"migrate status: " + "Including helper" + "\"\n");
                pre_writer.write("cp -r " + context.getFilesDir() + "/system" + " " + destination + "/" + backupName + "\n");
                pre_writer.write("rm -r " + context.getFilesDir() + "/system" + "\n");

                pre_writer.close();

                pre_script.setExecutable(true);
            }
            catch (Exception e){
                e.printStackTrace();
                errors.add("PRE_SCRIPT_ERROR" + errorTag + ": " + e.getMessage());
            }*/

            int c = 0;

            BufferedWriter scriptWriter = new BufferedWriter(new FileWriter(scriptFile));

            scriptWriter.write("#!sbin/sh\n\n");
            scriptWriter.write("echo \" \"\n");
            scriptWriter.write("sleep 1\n");
            scriptWriter.write("echo \"--- PID: $$\"\n");
            scriptWriter.write("cp " + scriptFile.getAbsolutePath() + " " + context.getExternalCacheDir() + "/\n");
            scriptWriter.write("cp " + busyboxBinaryFilePath + " " + destination + "/" + backupName + "/\n");

            //String line;
            for (int packetCount = 0; packetCount < numberOfApps && !isCancelled; packetCount++) {

                try {

                    //String items[] = line.split(" ");

                    /*if (items.length != 7){
                        errors.add("UNKNOWN_LINE" + errorTag + ": PARAMS: " + items.length + " LINE: '" + line + "'");
                        c++;
                        continue;
                    }*/

                    /*String appName = items[0].replace('`', '\'').replace('"', '\'');
                    String packageName = items[1];
                    String apkPath = items[2].replace('`', '\'').substring(0, items[2].lastIndexOf('/'));
                    String apkName = items[2].replace('`', '\'').substring(items[2].lastIndexOf('/') + 1);       //has .apk extension
                    String dataPath = items[3].trim();
                    String dataName = "NULL";
                    String appIcon = items[4];
                    String version = items[5].replace('`', '\'').replace('"', '\'');
                    boolean permissions = Boolean.parseBoolean(items[6]);*/

                    BackupDataPacket packet = backupBatch.appListWithSize.get(packetCount).packet;

                    String appName = pm.getApplicationLabel(packet.PACKAGE_INFO.applicationInfo).toString().replace(' ', '_').replace('`', '\'').replace('"', '\'');
                    String packageName = packet.PACKAGE_INFO.packageName;
                    String apkPath = packet.PACKAGE_INFO.applicationInfo.sourceDir;
                    String apkName = apkPath.substring(apkPath.lastIndexOf('/') + 1);       //has .apk extension
                    apkPath = apkPath.substring(0, apkPath.lastIndexOf('/'));

                    apkName = commonTools.applyNamingCorrectionForShell(apkName);

                    if (apkPath.endsWith(":")) apkPath = apkPath.substring(0, apkPath.length()-1);

                    String dataPath = "NULL";
                    String dataName = "NULL";
                    if (packet.DATA) {
                        dataPath = packet.PACKAGE_INFO.applicationInfo.dataDir;
                        dataName = dataPath.substring(dataPath.lastIndexOf('/') + 1);
                        dataPath = dataPath.substring(0, dataPath.lastIndexOf('/'));
                    }
                    String versionName = packet.PACKAGE_INFO.versionName;
                    if (versionName == null || versionName.equals(""))
                        versionName = "_";
                    else
                        versionName = versionName.replace(' ', '_').replace('`', '\'').replace('"', '\'');

                    String appIcon = "_";
                    boolean permissions = packet.PERMISSIONS;

                    try {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        Bitmap icon = getBitmapFromDrawable(pm.getApplicationIcon(packet.PACKAGE_INFO.applicationInfo));
                        icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        appIcon = byteToString(stream.toByteArray());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    c = packetCount + 1;

                    actualProgressBroadcast.putExtra("type", "reading_backup_data")
                            .putExtra("app_name", appName)
                            .putExtra("progress", (c * 100 / numberOfApps));
                    activityProgressIntent.putExtras(actualProgressBroadcast);

                    progressNotif.setProgress(numberOfApps, packetCount, false)
                            .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                            .setContentText(appName);
                    notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                    LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);


                    /*if (!dataPath.equals("NULL")) {
                        dataName = dataPath.substring(dataPath.lastIndexOf('/') + 1);
                        dataPath = dataPath.substring(0, dataPath.lastIndexOf('/'));
                    }*/

                    String echoCopyCommand = "echo \"" + MIGRATE_STATUS + ": " + appName + " (" + c + "/" + numberOfApps + ") icon: " + appIcon + "\"\n";
                /*String permissionCommand = "";

                if (permissions){
                    permissionCommand = "dumpsys package " + packageName + " | grep android.permission | grep granted=true > " +
                            destination + "/" + backupName + "/" + packageName + ".perm" + "\n";
                }

                String command = "cd " + apkPath + "; cp " + apkName + " " + destination + "/" + backupName + "/" + packageName + ".apk" + "\n";
                if (!dataPath.equals("NULL")){
                    command = command + "if [ -e " + dataPath + "/" + dataName + " ]; then\n";
                    command = command + "   cd " + dataPath + "; " + busyboxBinaryFilePath + " tar -cvzpf " + destination + "/" + backupName + "/" + dataName + ".tar.gz " + dataName + "\n";
                    command = command + "else\n";
                    command = command + "   echo \"dataPath : " + dataPath + "/" + dataName + " does not exist\"\n";
                    command = command + "fi\n";
                }*/

                    String scriptCommand = "sh " + appAndDataBackupScript + " " + packageName + " " + destination + "/" + backupName + " " +
                            apkPath + " " + apkName + " " + dataPath + " " + dataName + " " + busyboxBinaryFilePath + " " + permissions + "\n";

                    scriptWriter.write(echoCopyCommand, 0, echoCopyCommand.length());
                    scriptWriter.write(scriptCommand, 0, scriptCommand.length());

                    updater_writer.write("ui_print(\"" + appName + " (" + c + "/" + numberOfApps + ")\");\n");

                    if (apkPath.startsWith("/system")) {
                        updater_writer.write("package_extract_dir(\"" + packageName + ".app" + "\", \"/tmp/" + packageName + ".app" + "\");\n");
                        systemAppInstallScript(packageName, apkPath, packageName + ".app");
                        updater_writer.write("package_extract_file(\"" + packageName + ".sh" + "\", \"/tmp/" + packageName + ".sh" + "\");\n");
                        updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"/tmp/" + packageName + ".sh" + "\");\n");
                        updater_writer.write("run_program(\"/system/" + packageName + ".sh" + "\");\n");
                    }


                    String tempApkName;
                    if (apkPath.startsWith("/system")) {
                        tempApkName = "NULL";
                    } else {
                        tempApkName = packageName;
                        updater_writer.write("package_extract_dir(\"" + packageName + ".app" + "\", \"" + TEMP_DIR_NAME + "/" + packageName + ".app" + "\");\n");
                    }
                    makeMetadataFile(appName, packageName, tempApkName, dataName, appIcon, versionName, permissions);

                    if (permissions)
                        updater_writer.write("package_extract_file(\"" + packageName + ".perm" + "\", \"" + TEMP_DIR_NAME + "/" + packageName + ".perm" + "\");\n");

                    if (!dataName.equals("NULL")) {
                        updater_writer.write("package_extract_file(\"" + dataName + ".tar.gz" + "\", \"" + "/data/data/" + dataName + ".tar.gz" + "\");\n");
                    }
                    updater_writer.write("package_extract_file(\"" + packageName + ".json" + "\", \"" + TEMP_DIR_NAME + "/" + packageName + ".json" + "\");\n");

                    String pString = String.format(Locale.ENGLISH, "%.4f", ((c * 1.0) / numberOfApps));
                    pString = pString.replace(",", ".");
                    updater_writer.write("set_progress(" + pString + ");\n");

                } catch (Exception e) {
                    e.printStackTrace();
                    errors.add("APP_READ" + errorTag + ": " + e.getMessage());
                }
            }

            scriptWriter.write("echo \"--- App files copied ---\"\n");
            scriptWriter.close();

            scriptFile.setExecutable(true);

            if (doBackupContacts || doBackupSms || doBackupCalls || doBackupDpi || doBackupKeyboard) {
                updater_writer.write("ui_print(\" \");\n");

                if (doBackupContacts) {
                    updater_writer.write("ui_print(\"Extracting contacts: " + contactsBackupName + "\");\n");
                    updater_writer.write("package_extract_file(\"" + contactsBackupName + "\", \"" + TEMP_DIR_NAME + "/" + contactsBackupName + "\");\n");
                }
                if (doBackupSms) {
                    updater_writer.write("ui_print(\"Extracting sms: " + smsBackupName + "\");\n");
                    updater_writer.write("package_extract_file(\"" + smsBackupName + "\", \"" + TEMP_DIR_NAME + "/" + smsBackupName + "\");\n");
                }
                if (doBackupCalls) {
                    updater_writer.write("ui_print(\"Extracting call logs: " + callsBackupName + "\");\n");
                    updater_writer.write("package_extract_file(\"" + callsBackupName + "\", \"" + TEMP_DIR_NAME + "/" + callsBackupName + "\");\n");
                }
                if (doBackupDpi) {
                    updater_writer.write("ui_print(\"Extracting dpi data: " + dpiBackupName + "\");\n");
                    updater_writer.write("package_extract_file(\"" + dpiBackupName + "\", \"" + TEMP_DIR_NAME + "/" + dpiBackupName + "\");\n");
                }
                if (doBackupKeyboard) {
                    updater_writer.write("ui_print(\"Extracting keyboard data: " + keyboardBackupName + "\");\n");
                    updater_writer.write("package_extract_file(\"" + keyboardBackupName + "\", \"" + TEMP_DIR_NAME + "/" + keyboardBackupName + "\");\n");
                }
            }

            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Unpacking helper\");\n");
            updater_writer.write("package_extract_dir(\"system\", \"/system\");\n");
            updater_writer.write("package_extract_file(\"" + "helper_unpacking_script.sh" + "\", \"" + "/tmp/helper_unpacking_script.sh" + "\");\n");
            updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + "/tmp/helper_unpacking_script.sh" + "\");\n");
            updater_writer.write("run_program(\"/tmp/helper_unpacking_script.sh\", \"/system/app/helper\", \"" + THIS_VERSION + "\");\n");
            updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + "/system/app/MigrateHelper" + "\");\n");

            updater_writer.write("set_progress(1.0000);\n");

            updater_writer.write("package_extract_file(\"" + "verify.sh" + "\", \"" + "/tmp/verify.sh" + "\");\n");
            updater_writer.write("package_extract_file(\"" + "extras-data" + "\", \"" + "/tmp/extras-data" + "\");\n");
            updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + "/tmp/verify.sh" + "\");\n");
            updater_writer.write("run_program(\"/tmp/verify.sh\", \"" + TEMP_DIR_NAME + "\");\n");
            updater_writer.write("ifelse(is_mounted(\"/data\"), ui_print(\"Verification done!\") && sleep(2s), abort(\"Exiting...\"));\n");

            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Unmounting partitions...\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"umount\", \"/system\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"umount\", \"/data\");\n");

            updater_writer.write("ifelse(is_mounted(\"/data\") && is_mounted(\"/system\"), ui_print(\"\"), " +
                    "run_program(\"/tmp/mount_using_self_busybox.sh\", \"u\"));\n");

            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Finished!\");\n");
            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Files have been restored to Migrate cache.\");\n");
            updater_writer.write("ui_print(\"---------------------------------\");\n");
            updater_writer.write("ui_print(\"PLEASE ROOT YOUR ROM WITH MAGISK.\");\n");
            updater_writer.write("ui_print(\"YOU WILL BE PROMPTED TO CONTINUE RESTORE AFTER STARTUP!!\");\n");
            updater_writer.write("ui_print(\"---------------------------------\");\n");
            updater_writer.write("ui_print(\" \");\n");

            updater_writer.close();

        } catch (Exception e) {
            errors.add("SCRIPT" + errorTag + ": " + e.getMessage());
            e.printStackTrace();
        }

        //return new String[]{pre_script.getAbsolutePath(), scriptFile.getAbsolutePath()};
        return scriptFile.getAbsolutePath();
    }

    String makePackageFlashReady(NotificationManager notificationManager, NotificationCompat.Builder progressNotif, Intent activityProgressIntent) {


        String title = (totalParts > 1) ?
                context.getString(R.string.making_package_flash_ready) + " : " + madePartName : context.getString(R.string.making_package_flash_ready);

        actualProgressBroadcast.putExtra("type", "making_package_flash_ready");
        activityProgressIntent.putExtras(actualProgressBroadcast);

        progressNotif.setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setProgress(0, 0, false)
                .setContentText("");
        notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

        String err = "";
        String moveErr = "";

        String flashDirPath = destination + "/" + backupName + "/META-INF/com/google/android/";
        new File(flashDirPath).mkdirs();

        File updater_script_destination = new File(flashDirPath + "updater-script");

        File updater_script = new File(context.getExternalCacheDir(), "updater-script");
        if (updater_script.exists()) {
            moveErr = moveFile(updater_script, updater_script_destination);
            //updater_script.renameTo(updater_script_destination);
        } else {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "updater-script was not made" + "\n";
        }

        if (!updater_script_destination.exists()) {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "updater-script could not be moved " + moveErr + "\n";
        }

        File update_binary = new File(commonTools.unpackAssetToInternal("update-binary", "update-binary", false));

        File update_binary_destination = new File(flashDirPath + "update-binary");
        if (update_binary.exists()) {
            moveErr = moveFile(update_binary, update_binary_destination);
            //update_binary.renameTo(update_binary_destination);
        } else {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "update-binary could not be unpacked" + "\n";
        }

        if (!update_binary_destination.exists()) {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "update-binary could not be moved " + moveErr + "\n";
        }


        File prepScript = new File(commonTools.unpackAssetToInternal("prep.sh", "prep.sh", false));

        File prepScript_destination = new File(destination + "/" + backupName + "/prep.sh");
        if (prepScript.exists()) {
            moveErr = moveFile(prepScript, prepScript_destination);
            //prepScript.renameTo(prepScript_destination);
        } else {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "prep.sh could not be unpacked" + "\n";
        }

        if (!prepScript_destination.exists()) {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "prep.sh could not be moved " + moveErr + "\n";
        }


        File unpackerScript = new File(commonTools.unpackAssetToInternal("helper_unpacking_script.sh", "helper_unpacking_script.sh", false));

        File unpackerScript_destination = new File(destination + "/" + backupName + "/helper_unpacking_script.sh");
        if (unpackerScript.exists()) {
            moveErr = moveFile(unpackerScript, unpackerScript_destination);
            //prepScript.renameTo(prepScript_destination);
        } else {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "helper_unpacking_script.sh could not be unpacked" + "\n";
        }

        if (!unpackerScript_destination.exists()) {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "helper_unpacking_script.sh could not be moved " + moveErr + "\n";
        }


        File verifyScript = new File(commonTools.unpackAssetToInternal("verify.sh", "verify.sh", false));

        File verifyScript_destination = new File(destination + "/" + backupName + "/verify.sh");
        if (verifyScript.exists()) {
            moveErr = moveFile(verifyScript, verifyScript_destination);
            //verifyScript.renameTo(verifyScript_destination);
        } else {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "verify.sh could not be unpacked" + "\n";
        }

        if (!verifyScript_destination.exists()) {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "verify.sh could not be moved " + moveErr + "\n";
        }

        File mountScript = new File(commonTools.unpackAssetToInternal("mount_using_self_busybox.sh", "mount_using_self_busybox.sh", false));

        File mountScript_destination = new File(destination + "/" + backupName + "/mount_using_self_busybox.sh");
        if (mountScript.exists()) {
            moveErr = moveFile(mountScript, mountScript_destination);
            //mountScript.renameTo(mountScript_destination);
        } else {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "mount_using_self_busybox.sh could not be unpacked" + "\n";
        }

        if (!verifyScript_destination.exists()) {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "mount_using_self_busybox.sh could not be moved " + moveErr + "\n";
        }

        File helper = new File(commonTools.unpackAssetToInternal("helper.apk", "helper.apk", false));

        File helper_destination = new File(destination + "/" + backupName + "/system/app/helper/MigrateHelper.apk");
        helper_destination.getParentFile().mkdirs();
        //new File(destination + "/" + backupName + "/system/app/MigrateHelper").mkdirs();

        if (helper.exists()) {
            moveErr = moveFile(helper, helper_destination);
            //helper.renameTo(helper_destination);
        } else {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "Helper app could not be unpacked" + "\n";
        }

        if (!helper_destination.exists()) {
            err = err + "MAKE_PACKAGE_FLASH_READY" + errorTag + ": " + "Helper app could not be moved " + moveErr + "\n";
        }

        return err;

    }

    void cancelProcess() {
        try {
            Process killProcess = Runtime.getRuntime().exec("su");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(killProcess.getOutputStream()));
            writer.write("kill -9 " + PID + "\n");
            writer.write("kill -15 " + PID + "\n");
            writer.write("kill -9 " + CORRECTING_PID + "\n");
            writer.write("kill -15 " + CORRECTING_PID + "\n");
            writer.write("exit\n");
            writer.flush();

            killProcess.waitFor();

            try {
                suProcess.waitFor();
            } catch (Exception ignored) {
            }

        } catch (Exception ignored) {
        }

        isCancelled = true;

        Toast.makeText(context, context.getString(R.string.deletingFiles), Toast.LENGTH_SHORT).show();

        if (madePartName.trim().equals("")) {
            fullDelete(destination + "/" + backupName);
            fullDelete(destination + "/" + backupName + ".zip");
        } else {
            fullDelete(destination);
        }
    }

    void fullDelete(String path) {
        File file = new File(path);
        if (file.exists() && !file.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            if (!file.isDirectory())
                file.delete();
            else {
                File files[] = file.listFiles();
                for (int i = 0; i < files.length; i++)
                    fullDelete(files[i].getAbsolutePath());
                file.delete();
            }
        }
    }

    void backupAppPermissions(NotificationManager notificationManager, NotificationCompat.Builder progressNotif, Intent activityProgressIntent) {

        String title = (totalParts > 1) ?
                context.getString(R.string.backing_app_permissions) + " : " + madePartName : context.getString(R.string.backing_app_permissions);

        actualProgressBroadcast.putExtra("type", "backing_app_permissions");
        actualProgressBroadcast.putExtra("app_name", "");
        actualProgressBroadcast.putExtra("progress", 0);
        activityProgressIntent.putExtras(actualProgressBroadcast);

        progressNotif.setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setProgress(0, 0, false)
                .setContentText("");
        notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

        int c = 0;
        String appName = "";

        for (BackupDataPacketWithSize packetWithSize : backupBatch.appListWithSize) {

            if (isCancelled) break;

            BackupDataPacket packet = packetWithSize.packet;

            actualProgressBroadcast.putExtra("progress", (++c * 100 / numberOfApps))
                    .putExtra("type", "backing_app_permissions");

            if (packet.PERMISSIONS) {

                appName = pm.getApplicationLabel(packet.PACKAGE_INFO.applicationInfo).toString();

                writeGrantedPermissions(packet.PACKAGE_INFO.packageName, false);
            } else {
                appName = "";
            }

            actualProgressBroadcast.putExtra("app_name", "Permissions: " + appName);

            activityProgressIntent.putExtras(actualProgressBroadcast);

            progressNotif.setProgress(numberOfApps, c, false)
                    .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentText(appName);
            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);
        }

    }

    void writeGrantedPermissions(String packageName, boolean retry) {
        /*ArrayList<String> grantedPerms = new ArrayList<>(0);
        try {
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (pi.requestedPermissions == null){
                grantedPerms.add("no_permissions_granted");
            }
            else {
                for (int i = 0; i < pi.requestedPermissions.length; i++) {
                    if ((pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                        String p = pi.requestedPermissions[i].trim();
                        if (p.startsWith("android.permission"))
                            grantedPerms.add(p);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errors.add("READ_PERMISSION_ERROR" + errorTag + ": PACKAGE: " + packageName + " ERROR: " + e.getMessage());
        }
        return grantedPerms;*/

        File backupPerm = new File(destination + "/" + backupName + "/" + packageName + ".perm");
        try {

            BufferedWriter writer = new BufferedWriter(new FileWriter(backupPerm));

            //ArrayList<String> grantedPerms = getGrantedPermissions(packet.PACKAGE_INFO.packageName);
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (pi.requestedPermissions == null) {
                writer.write("no_permissions_granted");
            } else {
                for (int i = 0; i < pi.requestedPermissions.length; i++) {
                    if ((pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                        String p = pi.requestedPermissions[i].trim();
                        if (p.startsWith("android.permission"))
                            writer.write(p + "\n");
                    }
                }
            }

            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
            if (!retry && !e.getMessage().endsWith("No such file or directory")) {
                errors.add("PERM_FILE_MAKE_ERROR" + errorTag + ": PACKAGE: " + packageName + " ERROR: " + e.getMessage());
            }
            else {
                errors.add("SUPPRESSED_PERM_FILE_MAKE_ERROR" + errorTag + ": PACKAGE: " + packageName + " ERROR: " + e.getMessage());
            }
        }
    }

    void setDoContactsBackup(boolean doBackupContacts, Vector<ContactsDataPacket> contactList) {
        this.doBackupContacts = doBackupContacts;
        if (contactList == null)
            contactsDataPackets = null;
        else {
            contactsDataPackets = new Vector<>(0);
            for (ContactsDataPacket packet : contactList)
                if (packet.selected)
                    contactsDataPackets.add(packet);
        }
    }

    void setDoSmsBackup(boolean doBackupSms, Vector<SmsDataPacket> smsDataPackets) {
        this.doBackupSms = doBackupSms;
        if (smsDataPackets == null)
            this.smsDataPackets = null;
        else {
            this.smsDataPackets = new Vector<>(0);
            for (SmsDataPacket packet : smsDataPackets)
                if (packet.selected)
                    this.smsDataPackets.add(packet);
        }
    }

    void setDoCallsBackup(boolean doBackupCalls, Vector<CallsDataPacket> callsDataPackets) {
        this.doBackupCalls = doBackupCalls;
        if (callsDataPackets == null)
            this.callsDataPackets = null;
        else {
            this.callsDataPackets = new Vector<>(0);
            for (CallsDataPacket packet : callsDataPackets)
                if (packet.selected)
                    this.callsDataPackets.add(packet);
        }
    }

    void setDoDpiBackup(boolean doBackupDpi, String dpiText) {
        this.doBackupDpi = doBackupDpi;
        if (dpiText != null)
            this.dpiText = dpiText;
    }

    void setDoKeyboardBackup(boolean doBackupKeyboard, String keyboardText) {
        this.doBackupKeyboard = doBackupKeyboard;
        if (keyboardText != null)
            this.keyboardText = keyboardText;
    }

    String backupContacts(NotificationManager notificationManager, NotificationCompat.Builder progressNotif, Intent activityProgressIntent) {

        StringBuilder errors = new StringBuilder();

        if (!doBackupContacts || isCancelled)
            return errors.toString();

        (new File(destination + "/" + backupName)).mkdirs();

        String title = (totalParts > 1) ? context.getString(R.string.backing_contacts) + " : " + madePartName : context.getString(R.string.backing_contacts);
        progressNotif.setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setProgress(0, 0, false)
                .setContentText("");
        notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

        String vcfFilePath = destination + "/" + backupName + "/" + contactsBackupName;

        File vcfFile = new File(vcfFilePath);
        if (vcfFile.exists()) vcfFile.delete();

        if (contactsDataPackets != null) {

            int n = contactsDataPackets.size();

            BufferedWriter bufferedWriter = null;
            try {
                bufferedWriter = new BufferedWriter(new FileWriter(vcfFile, true));
            } catch (IOException e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

            if (bufferedWriter == null)
                return errors.toString();

            for (int j = 0; j < n; j++) {

                if (isCancelled)
                    return errors.toString();

                ContactsDataPacket thisPacket = contactsDataPackets.get(j);

                try {

                    bufferedWriter.write(thisPacket.vcfData + "\n");

                    actualProgressBroadcast.putExtra("type", "contact_progress").putExtra("contact_name", thisPacket.fullName).putExtra("progress", (j * 100 / n));
                    activityProgressIntent.putExtras(actualProgressBroadcast);

                    progressNotif.setProgress(n, j, false)
                            .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                            .setContentText(thisPacket.fullName);
                    notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                    LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                } catch (IOException e) {
                    e.printStackTrace();
                    errors.append(e.getMessage()).append("\n");
                }
            }


            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }
        } else {

            VcfTools vcfTools = new VcfTools(context);
            Cursor cursor = vcfTools.getContactsCursor();

            Vector<String[]> vcfDatas = new Vector<>(0);

            int n = -1;

            try {
                n = cursor.getCount();
                cursor.moveToFirst();
            } catch (Exception e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

            if (n == -1)
                return errors.toString();


            BufferedWriter bufferedWriter = null;

            try {
                bufferedWriter = new BufferedWriter(new FileWriter(vcfFile, true));
            } catch (IOException e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

            if (bufferedWriter == null)
                return errors.toString();

            for (int j = 0; j < n; j++) {


                if (isCancelled)
                    return errors.toString();

                try {

                    String[] data = vcfTools.getVcfData(cursor);

                    if (!isDuplicate(data, vcfDatas)) {
                        bufferedWriter.write(data[1] + "\n");

                        actualProgressBroadcast.putExtra("type", "contact_progress").putExtra("contact_name", data[0]).putExtra("progress", (j * 100 / n));
                        activityProgressIntent.putExtras(actualProgressBroadcast);

                        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                        vcfDatas.add(data);
                    }

                    progressNotif.setProgress(n, j, false)
                            .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                            .setContentText(data[0]);
                    notificationManager.notify(NOTIFICATION_ID, progressNotif.build());


                    cursor.moveToNext();

                } catch (IOException e) {
                    e.printStackTrace();
                    errors.append(e.getMessage()).append("\n");
                }
            }


            try {
                cursor.close();
            } catch (Exception e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }


            try {
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }
        }

        return errors.toString();
    }

    boolean isDuplicate(String data[], Vector<String[]> vcfDatas) {
        for (String vcfData[] : vcfDatas) {
            if (data[0].equals(vcfData[0]) && data[1].equals(vcfData[1]))
                return true;
        }
        return false;
    }

    String backupSms(NotificationManager notificationManager, NotificationCompat.Builder progressNotif, Intent activityProgressIntent) {

        StringBuilder errors = new StringBuilder();

        if (!doBackupSms || isCancelled)
            return errors.toString();

        (new File(destination + "/" + backupName)).mkdirs();

        String smsDBFilePath = destination + "/" + backupName + "/" + smsBackupName;

        String DROP_TABLE = "DROP TABLE IF EXISTS sms";
        String CREATE_TABLE = "CREATE TABLE sms ( id INTEGER PRIMARY KEY, smsAddress TEXT, smsBody TEXT, smsType TEXT, smsDate TEXT, smsDateSent TEXT, smsCreator TEXT, smsPerson TEXT, smsProtocol TEXT, smsSeen TEXT, smsServiceCenter TEXT, smsStatus TEXT, smsSubject TEXT, smsThreadId TEXT, smsError INTEGER, smsRead INTEGER, smsLocked INTEGER, smsReplyPathPresent INTEGER )";

        if (smsDataPackets == null) {

            smsDataPackets = new Vector<>(0);

            actualProgressBroadcast.putExtra("type", "sms_reading");
            activityProgressIntent.putExtras(actualProgressBroadcast);

            String title = (totalParts > 1) ? context.getString(R.string.reading_sms) + " : " + madePartName : context.getString(R.string.reading_sms);
            progressNotif.setContentTitle(title)
                    .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setProgress(0, 0, false)
                    .setContentText("");
            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

            Cursor inboxCursor, outboxCursor, sentCursor, draftCursor;

            SmsTools smsTools = new SmsTools(context);

            inboxCursor = smsTools.getSmsInboxCursor();
            outboxCursor = smsTools.getSmsOutboxCursor();
            sentCursor = smsTools.getSmsSentCursor();
            draftCursor = smsTools.getSmsDraftCursor();

            try {
                if (inboxCursor != null && inboxCursor.getCount() > 0) {
                    inboxCursor.moveToFirst();
                    do {

                        if (isCancelled)
                            return errors.toString();

                        smsDataPackets.add(smsTools.getSmsPacket(inboxCursor, true));
                    }
                    while (inboxCursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

            try {
                if (outboxCursor != null && outboxCursor.getCount() > 0) {
                    outboxCursor.moveToFirst();
                    do {
                        if (isCancelled)
                            return errors.toString();

                        smsDataPackets.add(smsTools.getSmsPacket(outboxCursor, true));
                    }
                    while (outboxCursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

            try {
                if (sentCursor != null && sentCursor.getCount() > 0) {
                    sentCursor.moveToFirst();
                    do {
                        if (isCancelled)
                            return errors.toString();

                        smsDataPackets.add(smsTools.getSmsPacket(sentCursor, true));
                    }
                    while (sentCursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

            try {
                if (draftCursor != null && draftCursor.getCount() > 0) {
                    draftCursor.moveToFirst();
                    do {
                        if (isCancelled)
                            return errors.toString();

                        smsDataPackets.add(smsTools.getSmsPacket(draftCursor, true));
                    }
                    while (draftCursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

        }

        if (smsDataPackets != null || smsDataPackets.size() != 0) {

            String title = (totalParts > 1) ? context.getString(R.string.backing_sms) + " : " + madePartName : context.getString(R.string.backing_sms);
            progressNotif.setContentTitle(title)
                    .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setProgress(0, 0, false)
                    .setContentText("");
            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            int n = 0;
            SQLiteDatabase db = null;

            try {
                n = smsDataPackets.size();
                db = SQLiteDatabase.openOrCreateDatabase(smsDBFilePath, null);
                if (android.os.Build.VERSION.SDK_INT >= 27) {
                    db = SQLiteDatabase.openDatabase(smsDBFilePath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READWRITE);
                }
                db.execSQL(DROP_TABLE);
                db.execSQL(CREATE_TABLE);
            } catch (Exception e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

            for (int j = 0; j < n; j++) {

                try {

                    if (isCancelled)
                        return errors.toString();

                    SmsDataPacket dataPacket = smsDataPackets.get(j);

                    ContentValues contentValues = new ContentValues();
                    contentValues.put("smsAddress", dataPacket.smsAddress);
                    contentValues.put("smsBody", dataPacket.smsBody);
                    contentValues.put("smsDate", dataPacket.smsDate);
                    contentValues.put("smsDateSent", dataPacket.smsDateSent);
                    contentValues.put("smsType", dataPacket.smsType);
                    contentValues.put("smsCreator", dataPacket.smsCreator);
                    contentValues.put("smsPerson", dataPacket.smsPerson);
                    contentValues.put("smsProtocol", dataPacket.smsProtocol);
                    contentValues.put("smsSeen", dataPacket.smsSeen);
                    contentValues.put("smsServiceCenter", dataPacket.smsServiceCenter);
                    contentValues.put("smsStatus", dataPacket.smsStatus);
                    contentValues.put("smsSubject", dataPacket.smsSubject);
                    contentValues.put("smsError", dataPacket.smsError);
                    contentValues.put("smsRead", dataPacket.smsRead);
                    contentValues.put("smsLocked", dataPacket.smsLocked);
                    contentValues.put("smsReplyPathPresent", dataPacket.smsReplyPathPresent);

                    db.insert("sms", null, contentValues);

                    actualProgressBroadcast.putExtra("type", "sms_progress").putExtra("sms_address", dataPacket.smsAddress).putExtra("progress", (j * 100 / n));
                    activityProgressIntent.putExtras(actualProgressBroadcast);

                    progressNotif.setProgress(n, j, false)
                            .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                            .setContentText(dataPacket.smsAddress);
                    notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                    LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                } catch (Exception e) {
                    e.printStackTrace();
                    errors.append(e.getMessage()).append("\n");
                }

            }

            try {
                db.close();
            } catch (Exception ignored) {
            }

        }

        (new File(smsDBFilePath + "-shm")).delete();
        (new File(smsDBFilePath + "-wal")).delete();

        return errors.toString();
    }

    String backupCalls(NotificationManager notificationManager, NotificationCompat.Builder progressNotif, Intent activityProgressIntent) {

        StringBuilder errors = new StringBuilder();

        if (!doBackupCalls || isCancelled)
            return errors.toString();

        (new File(destination + "/" + backupName)).mkdirs();

        String callsDBFilePath = destination + "/" + backupName + "/" + callsBackupName;

        String DROP_TABLE = "DROP TABLE IF EXISTS calls";
        String CREATE_TABLE = "CREATE TABLE calls ( id INTEGER PRIMARY KEY, callsCachedFormattedNumber TEXT, callsCachedLookupUri TEXT, callsCachedMatchedNumber TEXT," +
                "callsCachedName TEXT, callsCachedNormalizedNumber TEXT, callsCachedNumberLabel TEXT, callsCachedNumberType TEXT, callsCachedPhotoId TEXT," +
                "callsCountryIso TEXT, callsDataUsage TEXT, callsFeatures TEXT, callsGeocodedLocation TEXT, callsIsRead TEXT, callsNumber TEXT," +
                "callsNumberPresentation TEXT, callsPhoneAccountComponentName TEXT, callsPhoneAccountId TEXT, callsTranscription TEXT," +
                "callsType TEXT, callsVoicemailUri TEXT," +
                "callsDate INTEGER, callsDuration INTEGER, callsNew INTEGER )";

        if (callsDataPackets == null) {

            callsDataPackets = new Vector<>(0);

            actualProgressBroadcast.putExtra("type", "calls_reading");
            activityProgressIntent.putExtras(actualProgressBroadcast);

            String title = (totalParts > 1) ? context.getString(R.string.reading_calls) + " : " + madePartName : context.getString(R.string.reading_calls);
            progressNotif.setContentTitle(title)
                    .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setProgress(0, 0, false)
                    .setContentText("");
            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

            Cursor callsCursor;

            CallsTools callsTools = new CallsTools(context);

            callsCursor = callsTools.getCallsCursor();

            try {
                if (callsCursor != null && callsCursor.getCount() > 0) {
                    callsCursor.moveToFirst();
                    do {
                        if (isCancelled)
                            return errors.toString();

                        callsDataPackets.add(callsTools.getCallsPacket(callsCursor, true));
                    }
                    while (callsCursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

        }

        if (callsDataPackets != null || callsDataPackets.size() != 0) {

            String title = (totalParts > 1) ? context.getString(R.string.backing_calls) + " : " + madePartName : context.getString(R.string.backing_calls);
            progressNotif.setContentTitle(title)
                    .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setProgress(0, 0, false)
                    .setContentText("");
            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            int n = 0;
            SQLiteDatabase db = null;

            try {
                n = callsDataPackets.size();
                db = SQLiteDatabase.openOrCreateDatabase(callsDBFilePath, null);
                if (android.os.Build.VERSION.SDK_INT >= 27) {
                    db = SQLiteDatabase.openDatabase(callsDBFilePath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READWRITE);
                }
                db.execSQL(DROP_TABLE);
                db.execSQL(CREATE_TABLE);
            } catch (Exception e) {
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

            for (int j = 0; j < n; j++) {

                try {

                    if (isCancelled)
                        return errors.toString();

                    CallsDataPacket dataPacket = callsDataPackets.get(j);

                    ContentValues contentValues = new ContentValues();
                    contentValues.put("callsCachedFormattedNumber", dataPacket.callsCachedFormattedNumber);
                    contentValues.put("callsCachedLookupUri", dataPacket.callsCachedLookupUri);
                    contentValues.put("callsCachedMatchedNumber", dataPacket.callsCachedMatchedNumber);
                    contentValues.put("callsCachedName", dataPacket.callsCachedName);
                    contentValues.put("callsCachedNormalizedNumber", dataPacket.callsCachedNormalizedNumber);
                    contentValues.put("callsCachedNumberLabel", dataPacket.callsCachedNumberLabel);
                    contentValues.put("callsCachedNumberType", dataPacket.callsCachedNumberType);
                    contentValues.put("callsCachedPhotoId", dataPacket.callsCachedPhotoId);
                    contentValues.put("callsCountryIso", dataPacket.callsCountryIso);
                    contentValues.put("callsDataUsage", dataPacket.callsDataUsage);
                    contentValues.put("callsFeatures", dataPacket.callsFeatures);
                    contentValues.put("callsGeocodedLocation", dataPacket.callsGeocodedLocation);
                    contentValues.put("callsIsRead", dataPacket.callsIsRead);
                    contentValues.put("callsNumber", dataPacket.callsNumber);
                    contentValues.put("callsNumberPresentation", dataPacket.callsNumberPresentation);
                    contentValues.put("callsPhoneAccountComponentName", dataPacket.callsPhoneAccountComponentName);
                    contentValues.put("callsPhoneAccountId", dataPacket.callsPhoneAccountId);
                    contentValues.put("callsTranscription", dataPacket.callsTranscription);
                    contentValues.put("callsType", dataPacket.callsType);
                    contentValues.put("callsVoicemailUri", dataPacket.callsVoicemailUri);
                    contentValues.put("callsDate", dataPacket.callsDate);
                    contentValues.put("callsDuration", dataPacket.callsDuration);
                    contentValues.put("callsNew", dataPacket.callsNew);

                    db.insert("calls", null, contentValues);

                    String display;
                    if (dataPacket.callsCachedName != null && !dataPacket.callsCachedName.equals(""))
                        display = dataPacket.callsCachedName;
                    else display = dataPacket.callsNumber;

                    actualProgressBroadcast.putExtra("type", "calls_progress").putExtra("calls_name", display).putExtra("progress", (j * 100 / n));
                    activityProgressIntent.putExtras(actualProgressBroadcast);

                    progressNotif.setProgress(n, j, false)
                            .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                            .setContentText(display);
                    notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                    LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                } catch (Exception e) {
                    e.printStackTrace();
                    errors.append(e.getMessage()).append("\n");
                }

            }

            try {
                db.close();
            } catch (Exception ignored) {
            }

        }

        return errors.toString();
    }

    String backupDpi(NotificationManager notificationManager, NotificationCompat.Builder progressNotif, Intent activityProgressIntent) {

        String errors = "";

        if (!doBackupDpi || isCancelled)
            return errors;

        actualProgressBroadcast.putExtra("type", "dpi_progress");
        activityProgressIntent.putExtras(actualProgressBroadcast);

        String title = (totalParts > 1) ? context.getString(R.string.backing_dpi) + " : " + madePartName : context.getString(R.string.backing_dpi);
        progressNotif.setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setProgress(0, 0, true)
                .setContentText("");
        notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

        (new File(destination + "/" + backupName)).mkdirs();

        String dpiFilePath = destination + "/" + backupName + "/" + dpiBackupName;

        BufferedReader reader;

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(dpiFilePath));

            if (dpiText != null && !dpiText.equals("")) {
                reader = new BufferedReader(new StringReader(dpiText));
            } else {
                Process dpiReader = Runtime.getRuntime().exec("su");
                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(dpiReader.getOutputStream()));
                w.write("wm density\n");
                w.write("exit\n");
                w.flush();

                reader = new BufferedReader(new InputStreamReader(dpiReader.getInputStream()));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line.trim() + "\n");
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            errors = e.getMessage();
        }

        return errors;
    }

    String backupKeyboard(NotificationManager notificationManager, NotificationCompat.Builder progressNotif, Intent activityProgressIntent) {

        String errors = "";

        if (!doBackupKeyboard || isCancelled)
            return errors;

        actualProgressBroadcast.putExtra("type", "keyboard_progress");
        activityProgressIntent.putExtras(actualProgressBroadcast);

        String title = (totalParts > 1) ? context.getString(R.string.backing_keyboard) + " : " + madePartName : context.getString(R.string.backing_keyboard);
        progressNotif.setContentTitle(title)
                .setContentIntent(PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setProgress(0, 0, true)
                .setContentText("");
        notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

        (new File(destination + "/" + backupName)).mkdirs();

        String keyboardFilePath = destination + "/" + backupName + "/" + keyboardBackupName;

        BufferedReader reader;

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(keyboardFilePath));

            if (keyboardText != null && !keyboardText.equals("")) {
                reader = new BufferedReader(new StringReader(keyboardText));
            } else {
                return context.getString(R.string.no_keyboard_data);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line.trim() + "\n");
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            errors = e.getMessage();
        }

        return errors;
    }

}
