package balti.migrate;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Vector;

/**
 * Created by sayantan on 9/10/17.
 */

public class BackupEngine {

    public static final int NOTIFICATION_ID = 100;

    private String destination, backupName;
    private Context context;

    SharedPreferences main;

    private Process suProcess = null;
    private int compressionLevel;
    int numberOfJobs = 0;
    private String finalMessage = "";
    private ArrayList<String> errors;

    String timeStamp = "";

    private boolean doBackupContacts = false;
    private Vector<ContactsDataPacket> contactsDataPackets;
    private String contactsBackupName;

    private boolean doBackupSms = false;
    private Vector<SmsDataPacket> smsDataPackets;
    private String smsBackupName;

    private boolean doBackupCalls = false;
    private Vector<CallsDataPacket> callsDataPackets;
    private String callsBackupName;

    private Process getNumberOfFiles;
    private Process zipProcess;

    private Intent actualProgressBroadcast;

    private boolean isCancelled = false;
    private long startMillis;
    private long endMillis;

    private String zipBinaryFilePath, busyboxBinaryFilePath;

    private final String TEMP_DIR_NAME = "/data/balti.migrate";

    StartBackup startBackupTask;

    private long systemRequiredSize = 0;
    private long dataRequiredSize = 0;
    private boolean finalProcess = true;

    private CommonTools commonTools;

    private File backupSummary;

    class StartBackup extends AsyncTask{

        boolean doBackupContacts;
        Vector<ContactsDataPacket> contactsDataPackets;

        boolean doSmsBackup;
        Vector<SmsDataPacket> smsDataPackets;

        boolean doBackupCalls;
        Vector<CallsDataPacket> callsDataPackets;

        public StartBackup(boolean doContactsBackup, Vector<ContactsDataPacket> contactsDataPackets, boolean doSmsBackup, Vector<SmsDataPacket> smsDataPackets,
                boolean doBackupCalls, Vector<CallsDataPacket> callsDataPackets) {
            this.doBackupContacts = doContactsBackup;
            this.contactsDataPackets = contactsDataPackets;
            this.doSmsBackup = doSmsBackup;
            this.smsDataPackets = smsDataPackets;
            this.doBackupCalls = doBackupCalls;
            this.callsDataPackets = callsDataPackets;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            setDoContactsBackup(doBackupContacts, contactsDataPackets);
            setDoSmsBackup(doSmsBackup, smsDataPackets);
            setDoCallsBackup(doBackupCalls, callsDataPackets);
            initiateBackup();
            return null;
        }
    }

    BackupEngine(String backupName, int compressionLevel, String destination, String busyboxBinaryFilePath,
                 Context context, long systemRequiredSize, long dataRequiredSize, boolean finalProcess, File backupSummary) {
        this.backupName = backupName;
        this.destination = destination;
        this.compressionLevel = compressionLevel;
        (new File(destination)).mkdirs();
        this.context = context;

        this.busyboxBinaryFilePath = busyboxBinaryFilePath;

        this.systemRequiredSize = systemRequiredSize;
        this.dataRequiredSize = dataRequiredSize;
        this.finalProcess = finalProcess;
        this.backupSummary = backupSummary;

        getNumberOfFiles = zipProcess = null;

        main = context.getSharedPreferences("main", Context.MODE_PRIVATE);
        actualProgressBroadcast = new Intent("Migrate progress broadcast");

        errors = new ArrayList<>(0);

        zipBinaryFilePath = "";

        timeStamp = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Calendar.getInstance().getTime());

        contactsBackupName = "Contacts_" + timeStamp + ".vcf";
        smsBackupName = "Sms_" + timeStamp + ".sms.db";
        callsBackupName = "Calls_" + timeStamp + ".calls.db";

        commonTools = new CommonTools(context);
    }

    NotificationCompat.Builder createNotificationBuilder(){
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            builder = new NotificationCompat.Builder(context, BackupService.CHANNEL);
        }
        else {
            builder = new NotificationCompat.Builder(context);
        }
        return builder;
    }

    long timeInMillis(){
        Calendar calendar = Calendar.getInstance();
        return calendar.getTimeInMillis();
    }

    String calendarDifference(long start, long end){
        String diff = "";

        try {

            long longDiff = end - start;
            longDiff = longDiff / 1000;

            long d = longDiff / (60 * 60 * 24);
            if (d != 0) diff = diff + d + "days ";
            longDiff = longDiff % (60 * 60 * 24);

            long h = longDiff / (60 * 60);
            if (h != 0) diff = diff + h + "hrs ";
            longDiff = longDiff % (60 * 60);

            long m = longDiff / 60;
            if (m != 0) diff = diff + m + "mins ";
            longDiff = longDiff % 60;

            long s = longDiff;
            diff = diff + s + "secs";

        }
        catch (Exception ignored){}

        return diff;
    }

    void startBackup(boolean doBackupContacts, Vector<ContactsDataPacket> contactsDataPackets, boolean doBackupSms, Vector<SmsDataPacket> smsDataPackets,
            boolean doBackupCalls, Vector<CallsDataPacket> callsDataPackets){
        try {
            startBackupTask.cancel(true);
        }
        catch (Exception ignored){}
        startBackupTask = new StartBackup(doBackupContacts, contactsDataPackets, doBackupSms, smsDataPackets, doBackupCalls, callsDataPackets);
        startBackupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        Intent activityProgressIntent = new Intent(context, BackupProgressLayout.class);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, 53, cancelIntent, 0);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(context, 1, activityProgressIntent, 0);

        progressNotif.setContentIntent(activityPendingIntent);

        NotificationCompat.Action cancelAction = new NotificationCompat.Action(0, context.getString(android.R.string.cancel), cancelPendingIntent);

        try {
            File receivedFiles[] = makeScripts();
            if (isCancelled) throw new InterruptedIOException();
            if (receivedFiles[0] != null && receivedFiles[1] != null) {

                progressNotif.addAction(cancelAction);

                makePackageData();

                String contactsErr = backupContacts(notificationManager, progressNotif);
                if (!contactsErr.equals("")) errors.add("CONTACTS: " + contactsErr);

                String smsErr = backupSms(notificationManager, progressNotif);
                if (!smsErr.equals("")) errors.add("SMS: " + smsErr);

                String callsErr = backupCalls(notificationManager, progressNotif);
                if (!callsErr.equals("")) errors.add("CALLS: " + callsErr);

                if (isCancelled)
                    throw new InterruptedIOException();

                    suProcess = Runtime.getRuntime().exec("su -c sh " + receivedFiles[0].getAbsolutePath());
                    BufferedReader outputStream = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));
                    BufferedReader errorStream = new BufferedReader(new InputStreamReader(suProcess.getErrorStream()));
                    String line;
                    int c = 0, p = 100;


                    String iconString;

                    while ((line = outputStream.readLine()) != null) {

                        line = line.trim();

                        actualProgressBroadcast.putExtra("type", "app_progress");

                        if (line.startsWith("migrate status:")) {
                            line = line.substring(16);

                            if (line.contains("icon:")) {
                                iconString = line.substring(line.lastIndexOf(' ') + 1);
                                line = line.substring(0, line.indexOf("icon:"));
                                actualProgressBroadcast.putExtra("app_icon", iconString);
                                actualProgressBroadcast.putExtra("app_name", line);
                            }
                            else {
                                actualProgressBroadcast.putExtra("app_name", line);
                            }

                            if (numberOfJobs != 0) p = c++ * 100 / numberOfJobs;
                            actualProgressBroadcast.putExtra("progress", p);

                            LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                            progressNotif.setContentTitle(context.getString(R.string.backingUp))
                                    .setProgress(numberOfJobs, c, false)
                                    .setContentText(line);
                            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                        }

                        actualProgressBroadcast.putExtra("app_log", line);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                    }


                    while ((line = errorStream.readLine()) != null) {
                        errors.add("RUN: " + line);
                    }

            } else {
                errors.add(context.getString(R.string.errorMakingScripts));
            }

            String zipErr = zipAll(notificationManager, progressNotif);
            if (!zipErr.trim().equals("")) errors.add("ZIP: " + zipErr);

        } catch (Exception e) {
            if (!isCancelled){
                errors.add("INIT: " + e.getMessage());
            }
        }

        progressNotif = createNotificationBuilder();
        progressNotif.setSmallIcon(R.drawable.ic_notification_icon);

        if (isCancelled) finalMessage = context.getString(R.string.backupCancelled);
        else if (errors.size() == 0) finalMessage = context.getString(R.string.noErrors);
        else {
            finalMessage = context.getString(R.string.backupFinishedWithErrors);
            progressNotif.setContentText(errors.get(0));
        }

        endMillis = timeInMillis();

        progressNotif.setContentTitle(finalMessage);

        notificationManager.cancel(NOTIFICATION_ID);

        activityProgressIntent.putExtra("type", "finished").putExtra("finishedMessage", finalMessage.trim())
                .putExtra("total_time", endMillis - startMillis).putExtra("final_process", finalProcess);
        if (errors.size() > 0)
            activityProgressIntent.putStringArrayListExtra("errors", errors);
        activityPendingIntent = PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        progressNotif
                .setProgress(0, 0, false)
                .setContentIntent(activityPendingIntent)
                .setAutoCancel(true)
                .mActions.clear();

        notificationManager.notify(NOTIFICATION_ID + 1, progressNotif.build());


        actualProgressBroadcast.putExtra("type","finished").putExtra("finishedMessage", finalMessage.trim())
                .putExtra("total_time", endMillis - startMillis).putExtra("final_process", finalProcess);
        if (errors.size() > 0) {
            actualProgressBroadcast.putStringArrayListExtra("errors", errors);
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

        if (finalProcess)
            context.stopService(new Intent(context, BackupService.class));
    }

    private String zipAll(NotificationManager notificationManager, NotificationCompat.Builder progressNotif){

        String err = "";
        try {

            progressNotif.setContentTitle(context.getString(R.string.combining))
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

                if (l.startsWith("zip I/O error:") || l.startsWith("zip error:"))
                    err = err + l + "\n";

                int pr = c*100 / n;
                if (pr == 100) pr = 99;
                actualProgressBroadcast.putExtra("type", "zip_progress")
                        .putExtra("progress", pr)
                        .putExtra("zip_log", l);
                LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                progressNotif.setProgress(n, c, false);
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
    }

    void makePackageData(){

        File package_data = new File(destination + "/" + backupName + "/package-data");
        String contents = "";

        package_data.getParentFile().mkdirs();

        contents += "version 1.0" + "\n";
        contents += "backup_name " + backupName + "\n";
        contents += "timestamp " + timeStamp + "\n";
        contents += "device " + Build.DEVICE + "\n";
        contents += "sdk " + Build.VERSION.SDK_INT + "\n";
        contents += "cpu_abi " + Build.SUPPORTED_ABIS[0] + "\n";
        contents += "data_required_size " + dataRequiredSize + "\n";
        contents += "system_required_size " + systemRequiredSize + "\n";

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(package_data));
            writer.write(contents);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void systemAppInstallScript(String sysAppPackageName, String sysApkName, String sysAppPastingDir) {

        String scriptName = sysAppPackageName + ".sh";
        String scriptLocation = destination + "/" + backupName + "/" + scriptName;
        File script = new File(scriptLocation);

        String scriptText = "#!sbin/sh\n\n";

        scriptText = scriptText +
                "mkdir -p " + sysAppPastingDir + "\n" +
                "cd " + sysAppPastingDir + "\n" +
                "mv /system/" + sysAppPackageName + ".apk " + sysApkName + ".apk" + "\n" +
                "cd /system/" + "\n" +
                "rm " + scriptName + "\n";


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

    void makeMetadataFile(String appName, String packageName, String apkName, String dataName, String icon, String version){
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
                "   \"version\" : \"" + version + "\"\n" +
                "}\n";

        (new File(destination + "/" + backupName)).mkdirs();

        BufferedReader reader = new BufferedReader(new StringReader(metadataContent));

        try {
            metadataFile.createNewFile();
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

    File[] makeScripts() {

        zipBinaryFilePath = commonTools.unpackAssetToInternal("zip", "zip");

        if (busyboxBinaryFilePath.equals("") || zipBinaryFilePath.equals(""))
            return new File[]{null, null};

        File script = new File(context.getFilesDir(), "script.sh");
        File updater_script = new File(context.getFilesDir(), "updater-script");
        File helper = new File(context.getFilesDir() + "/system/app/MigrateHelper", "MigrateHelper.apk");

        String update_binary = commonTools.unpackAssetToInternal("update-binary", "update-binary");
        String prepScript = commonTools.unpackAssetToInternal("prep.sh", "prep.sh");

        AssetManager assetManager = context.getAssets();
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
        }

        try {
            BufferedReader backupSummaryReader = new BufferedReader(new FileReader(backupSummary));
            while (backupSummaryReader.readLine() != null)
                numberOfJobs++;
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            BufferedReader backupSummaryReader = new BufferedReader(new FileReader(backupSummary));
            BufferedWriter writer = new BufferedWriter(new FileWriter(script));
            BufferedWriter updater_writer = new BufferedWriter(new FileWriter(updater_script));

            updater_writer.write("show_progress(0, 0);\n");
            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"---------------------------------\");\n");
            updater_writer.write("ui_print(\"      Migrate Flash package      \");\n");
            updater_writer.write("ui_print(\"---------------------------------\");\n");
            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Mounting partition...\");\n");
            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"mount\", \"/system\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"mount\", \"/data\");\n");

            updater_writer.write("package_extract_file(\"" + "prep.sh" + "\", \"" + "/tmp/prep.sh" + "\");\n");
            updater_writer.write("package_extract_file(\"" + "package-data" + "\", \"" + "/tmp/package-data" + "\");\n");
            updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + "/tmp/prep.sh" + "\");\n");
            updater_writer.write("run_program(\"" + "/tmp/prep.sh" + "\");\n");

            updater_writer.write("ifelse(is_mounted(\"/data\") && is_mounted(\"/system\"), ui_print(\"Parameters checked!\") && sleep(2s), abort(\"Exiting...\"));\n");
            updater_writer.write("ui_print(\" \");\n");

            updater_writer.write("ui_print(\"Restoring to Migrate cache...\");\n");
            updater_writer.write("ui_print(\" \");\n");

            String flashDirPath = destination + "/" + backupName + "/META-INF/com/google/android/";

            writer.write("echo \"migrate status: " + "Making package Flash Ready" + "\"\n");
            writer.write("mkdir -p " + flashDirPath + "\n");
            writer.write("mv " + updater_script.getAbsolutePath() + " " + flashDirPath + "\n");
            writer.write("mv " + update_binary + " " + flashDirPath + "\n");
            writer.write("mv " + prepScript + " " + destination + "/" + backupName + "\n");

            writer.write("echo \"migrate status: " + "Including helper" + "\"\n");
            writer.write("cp -r " + context.getFilesDir() + "/system" + " " + destination + "/" + backupName + "\n");
            writer.write("rm -r " + context.getFilesDir() + "/system" + "\n");

            String line;

            String mkdirCommand = "mkdir -p " + destination + "/" + backupName + "/";
            writer.write(mkdirCommand + '\n', 0, mkdirCommand.length() + 1);

            int c = 0;

            while ((line = backupSummaryReader.readLine()) != null) {

                String items[] = line.split(" ");
                String appName = items[0];
                String packageName = items[1];
                String apkPath = items[2].substring(0, items[2].lastIndexOf('/'));
                String apkName = items[2].substring(items[2].lastIndexOf('/')+1);       //has .apk extension
                String dataPath = items[3].trim();
                String dataName = "NULL";
                String appIcon = items[4];
                String version = items[5];

                c++;

                if (!dataPath.equals("NULL")) {
                    dataName = dataPath.substring(dataPath.lastIndexOf('/') + 1);
                    dataPath = dataPath.substring(0, dataPath.lastIndexOf('/'));
                }

                String echoCopyCommand = "echo \"migrate status: " + appName + " (" + c + "/" + numberOfJobs + ") icon: " + appIcon + "\"";

                String command;

                command = "cd " + apkPath + "; cp " + apkName + " " + destination + "/" + backupName + "/" + packageName + ".apk" + "\n";
                if (!dataPath.equals("NULL")){
                    command = command + "cd " + dataPath + "; " + busyboxBinaryFilePath + " tar -cvzpf " + destination + "/" + backupName + "/" + dataName + ".tar.gz " + dataName + "\n";
                }

                updater_writer.write("ui_print(\"" + appName + " (" + c + "/" + numberOfJobs + ")\");\n");

                if (apkPath.startsWith("/system")){
                    updater_writer.write("package_extract_file(\"" + packageName + ".apk" + "\", \"/system/" + packageName + ".apk" + "\");\n");
                    systemAppInstallScript(packageName, appName, apkPath);
                    updater_writer.write("package_extract_file(\"" + packageName + ".sh" + "\", \"/system/" + packageName + ".sh" + "\");\n");
                    updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"/system/" + packageName + ".sh" + "\");\n");
                    updater_writer.write("run_program(\"/system/" + packageName + ".sh" + "\");\n");
                }


                String tempApkName;
                if (apkPath.startsWith("/system")) {
                    tempApkName = "NULL";
                } else {
                    tempApkName = packageName;
                    updater_writer.write("package_extract_file(\"" + packageName + ".apk" + "\", \"" + TEMP_DIR_NAME + "/" + packageName + ".apk" + "\");\n");
                }
                makeMetadataFile(appName, packageName, tempApkName, dataName, appIcon, version);

                if (!dataName.equals("NULL")) {
                    updater_writer.write("package_extract_file(\"" + dataName + ".tar.gz" + "\", \"" + TEMP_DIR_NAME + "/" + dataName + ".tar.gz" + "\");\n");
                }
                updater_writer.write("package_extract_file(\"" + packageName + ".json" + "\", \"" + TEMP_DIR_NAME + "/" + packageName + ".json" + "\");\n");

                updater_writer.write("set_progress(" + String.format("%.4f", ((c * 1.0) / numberOfJobs)) + ");\n");

                writer.write(echoCopyCommand + '\n', 0, echoCopyCommand.length() + 1);
                writer.write(command, 0, command.length());
            }

            if (doBackupContacts || doBackupSms || doBackupCalls) {
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
                updater_writer.write("ui_print(\" \");\n");
            }


            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Unpacking helper\");\n");
            updater_writer.write("package_extract_dir(\"system\", \"/system\");\n");
            updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + "/system/app/MigrateHelper/" + "\");\n");

            updater_writer.write("set_progress(1.0000);\n");
            updater_writer.write("ui_print(\" \");\n");

            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Unmounting partitions...\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"umount\", \"/system\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"umount\", \"/data\");\n");


            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Finished!\");\n");
            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Files have been restored to Migrate cache.\");\n");
            updater_writer.write("ui_print(\"---------------------------------\");\n");
            updater_writer.write("ui_print(\"PLEASE MAKE SURE THAT YOUR ROM IS ROOTED.\");\n");
            updater_writer.write("ui_print(\"YOU WILL BE PROMPTED TO CONTINUE RESTORE AFTER STARTUP!!\");\n");
            updater_writer.write("ui_print(\"---------------------------------\");\n");
            updater_writer.write("ui_print(\" \");\n");

            updater_writer.close();

            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
            script.delete();
            updater_script.delete();
            updater_script = null;
            script = null;
        }
        return new File[]{script, updater_script};
    }

    void cancelProcess() {
        isCancelled = true;
        try {
            suProcess.destroy();
            if (getNumberOfFiles != null) getNumberOfFiles.destroy();
            if (zipProcess != null) zipProcess.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(context, context.getString(R.string.deletingFiles), Toast.LENGTH_SHORT).show();
        fullDelete(destination + "/" + backupName);
        fullDelete(destination + "/" + backupName + ".zip");

        try {
            startBackupTask.cancel(true);
        }
        catch (Exception ignored){}
    }

    void fullDelete(String path){
        File file = new File(path);
        if (file.exists()) {
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

    void setDoContactsBackup(boolean doBackupContacts, Vector<ContactsDataPacket> contactList){
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

    void setDoSmsBackup(boolean doBackupSms, Vector<SmsDataPacket> smsDataPackets){
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

    void setDoCallsBackup(boolean doBackupCalls, Vector<CallsDataPacket> callsDataPackets){
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

    String backupContacts(NotificationManager notificationManager, NotificationCompat.Builder progressNotif){

        StringBuilder errors = new StringBuilder();

        if (!doBackupContacts || isCancelled)
            return errors.toString();

        (new File(destination + "/" + backupName)).mkdirs();


        progressNotif.setContentTitle(context.getString(R.string.backing_contacts))
                .setProgress(0, 0, false)
                .setContentText("");
        notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

        String vcfFilePath = destination + "/" + backupName + "/" + contactsBackupName;

        File vcfFile = new File(vcfFilePath);
        if (vcfFile.exists()) vcfFile.delete();

        if (contactsDataPackets != null){

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

                    progressNotif.setProgress(n, j, false)
                            .setContentText(thisPacket.fullName);
                    notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                    actualProgressBroadcast.putExtra("type", "contact_progress").putExtra("contact_name", thisPacket.fullName).putExtra("progress", (j*100/n));
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
        }

        else {

            VcfTools vcfTools = new VcfTools(context);
            Cursor cursor = vcfTools.getContactsCursor();

            Vector<String[]> vcfDatas = new Vector<>(0);

            int n = -1;

            try {
                n = cursor.getCount();
                cursor.moveToFirst();
            }
            catch (Exception e){
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

                        actualProgressBroadcast.putExtra("type", "contact_progress").putExtra("contact_name", data[0]).putExtra("progress", (j*100/n));
                        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                        vcfDatas.add(data);
                    }

                    progressNotif.setProgress(n, j, false)
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

    boolean isDuplicate(String data[], Vector<String[]> vcfDatas){
        for (String vcfData[] : vcfDatas){
            if (data[0].equals(vcfData[0]) && data[1].equals(vcfData[1]))
                return true;
        }
        return false;
    }

    String backupSms(NotificationManager notificationManager, NotificationCompat.Builder progressNotif){

        StringBuilder errors = new StringBuilder();

        if (!doBackupSms || isCancelled)
            return errors.toString();

        (new File(destination + "/" + backupName)).mkdirs();

        String smsDBFilePath = destination + "/" + backupName + "/" + smsBackupName;

        String DROP_TABLE = "DROP TABLE IF EXISTS sms";
        String CREATE_TABLE = "CREATE TABLE sms ( id INTEGER PRIMARY KEY, smsAddress TEXT, smsBody TEXT, smsType TEXT, smsDate TEXT, smsDateSent TEXT, smsCreator TEXT, smsPerson TEXT, smsProtocol TEXT, smsSeen TEXT, smsServiceCenter TEXT, smsStatus TEXT, smsSubject TEXT, smsThreadId TEXT, smsError INTEGER, smsRead INTEGER, smsLocked INTEGER, smsReplyPathPresent INTEGER )";

        if (smsDataPackets == null){

            smsDataPackets = new Vector<>(0);


            progressNotif.setContentTitle(context.getString(R.string.reading_sms))
                    .setProgress(0, 0, false)
                    .setContentText("");
            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            actualProgressBroadcast.putExtra("type", "sms_reading");
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
            }
            catch (Exception e){
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
            }
            catch (Exception e){
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
            }
            catch (Exception e){
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
            }
            catch (Exception e){
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

        }

        if (smsDataPackets != null || smsDataPackets.size() != 0) {

            progressNotif.setContentTitle(context.getString(R.string.backing_sms))
                    .setProgress(0, 0, false)
                    .setContentText("");
            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            int n = 0;
            SQLiteDatabase db = null;

            try {
                n = smsDataPackets.size();
                db = SQLiteDatabase.openOrCreateDatabase(smsDBFilePath, null);
                db.execSQL(DROP_TABLE);
                db.execSQL(CREATE_TABLE);
            }catch (Exception e){
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

            for (int j = 0; j < n; j++){

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

                    progressNotif.setProgress(n, j, false)
                            .setContentText(dataPacket.smsAddress);
                    notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                    actualProgressBroadcast.putExtra("type", "sms_progress").putExtra("sms_address", dataPacket.smsAddress).putExtra("progress", (j*100/n));
                    LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                }
                catch (Exception e){
                    e.printStackTrace();
                    errors.append(e.getMessage()).append("\n");
                }

            }

            try {
                db.close();
            } catch (Exception ignored){}

        }

        (new File(smsDBFilePath + "-shm")).delete();
        (new File(smsDBFilePath + "-wal")).delete();

        return errors.toString();
    }

    String backupCalls(NotificationManager notificationManager, NotificationCompat.Builder progressNotif){

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

        if (callsDataPackets == null){

            callsDataPackets = new Vector<>(0);


            progressNotif.setContentTitle(context.getString(R.string.reading_calls))
                    .setProgress(0, 0, false)
                    .setContentText("");
            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            actualProgressBroadcast.putExtra("type", "calls_reading");
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
            }
            catch (Exception e){
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

        }

        if (callsDataPackets != null || callsDataPackets.size() != 0) {

            progressNotif.setContentTitle(context.getString(R.string.backing_calls))
                    .setProgress(0, 0, false)
                    .setContentText("");
            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

            int n = 0;
            SQLiteDatabase db = null;

            try {
                n = callsDataPackets.size();
                db = SQLiteDatabase.openOrCreateDatabase(callsDBFilePath, null);
                db.execSQL(DROP_TABLE);
                db.execSQL(CREATE_TABLE);
            }catch (Exception e){
                e.printStackTrace();
                errors.append(e.getMessage()).append("\n");
            }

            for (int j = 0; j < n; j++){

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
                    if (dataPacket.callsCachedName != null && !dataPacket.callsCachedName.equals("")) display = dataPacket.callsCachedName;
                    else display = dataPacket.callsNumber;

                    progressNotif.setProgress(n, j, false)
                            .setContentText(display);
                    notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                    actualProgressBroadcast.putExtra("type", "calls_progress").putExtra("calls_name", display).putExtra("progress", (j*100/n));
                    LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

                }
                catch (Exception e){
                    e.printStackTrace();
                    errors.append(e.getMessage()).append("\n");
                }

            }

            try {
                db.close();
            } catch (Exception ignored){}

        }

        return errors.toString();
    }

}
