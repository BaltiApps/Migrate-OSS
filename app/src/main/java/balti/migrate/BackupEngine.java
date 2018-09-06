package balti.migrate;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
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

    private boolean doBackupContacts = false;
    private Vector<ContactsDataPacket> contactsDataPackets;

    private Process getNumberOfFiles;
    private Process zipProcess;

    private Intent actualProgressBroadcast;

    private boolean isCancelled = false;
    private long startMillis;
    private long endMillis;

    private String zipBinaryFilePath, busyboxBinaryFilePath;

    private final String TEMP_DIR_NAME = "/data/balti.migrate";

    StartBackup startBackupTask;

    class StartBackup extends AsyncTask{

        boolean doBackupContacts;
        Vector<ContactsDataPacket> contactsDataPackets;

        public StartBackup(boolean doContactsBackup, Vector<ContactsDataPacket> contactsDataPackets) {
            this.doBackupContacts = doContactsBackup;
            this.contactsDataPackets = contactsDataPackets;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            setDoContactsBackup(doBackupContacts, contactsDataPackets);
            initiateBackup();
            return null;
        }
    }

    BackupEngine(String backupName, int compressionLevel, String destination, Context context) {
        this.backupName = backupName;
        this.destination = destination;
        this.compressionLevel = compressionLevel;
        (new File(destination)).mkdirs();
        this.context = context;

        getNumberOfFiles = zipProcess = null;

        main = context.getSharedPreferences("main", Context.MODE_PRIVATE);
        actualProgressBroadcast = new Intent("Migrate progress broadcast");

        errors = new ArrayList<>(0);

        zipBinaryFilePath = "";
        busyboxBinaryFilePath = "";


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

    void startBackup(boolean doBackupContacts, Vector<ContactsDataPacket> contactsDataPackets){
        try {
            startBackupTask.cancel(true);
        }
        catch (Exception ignored){}
        startBackupTask = new StartBackup(doBackupContacts, contactsDataPackets);
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

                String contactsErr = backupContacts(notificationManager, progressNotif);
                if (!contactsErr.equals("")) errors.add("CONTACTS: " + contactsErr);

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
            if (!zipErr.equals("")) errors.add("ZIP: " + zipErr);

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

        /*if (finalMessage.equals(context.getString(R.string.backupCancelled))) {
            progressNotif.setContentTitle(finalMessage);
        } else if (finalMessage.equals(context.getString(R.string.noErrors))) {
            progressNotif.setContentTitle(context.getString(R.string.noErrors));
        } else {
            finalMessage = context.getString(R.string.backupFinishedWithErrors);
            progressNotif.setContentTitle(context.getString(R.string.backupFinishedWithErrors));
            progressNotif.setContentText(errors.get(0));
        }*/


        activityProgressIntent.putExtra("type", "finished").putExtra("finishedMessage", finalMessage.trim() + "\n(" + calendarDifference(startMillis, endMillis) + ")");
        if (errors.size() > 0)
            activityProgressIntent.putStringArrayListExtra("errors", errors);
        activityPendingIntent = PendingIntent.getActivity(context, 1, activityProgressIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        progressNotif
                .setProgress(0, 0, false)
                .setContentIntent(activityPendingIntent)
                .setAutoCancel(true)
                .mActions.clear();

        notificationManager.notify(NOTIFICATION_ID + 1, progressNotif.build());


        actualProgressBroadcast.putExtra("type","finished")
                .putExtra("finishedMessage", finalMessage.trim() + "\n(" + calendarDifference(startMillis, endMillis) + ")");
        if (errors.size() > 0) {
            actualProgressBroadcast.putStringArrayListExtra("errors", errors);
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(actualProgressBroadcast);

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

            if (c < n){
                finalMessage = context.getString(R.string.notEnoughSpace) + "\n";
            }
        }
        catch (Exception e) {
            if (!isCancelled)
                err = err + e.getMessage() + "\n";
        }
        return err;
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

        /*if (isApp){

            filename = filename + ".apk";

            if (!isSystem) {

                scriptText = scriptText +
                        "cd " + TEMP_DIR_NAME + "\n" +
                        "pm install -r " + filename + "\n" +
                        "rm " + filename + "\n" +
                        "rm " + scriptName + "\n";
            }
            else {

                String sysAppNameDir = sysAppPastinDir + "/" + filename.substring(0, filename.lastIndexOf('-')) + "/";

                scriptText = scriptText +
                        "mkdir -p " + sysAppNameDir + "\n" +
                        "cd /system/app/" + "\n" +
                        "mv " + filename + " " + sysAppNameDir + "\n" +
                        "rm " + scriptName + "\n";

            }

        }

        else {

            String dirName = filename.substring(0, filename.lastIndexOf('-'));

            filename = filename + ".tar.gz";

            scriptText = scriptText +
                    "cp " + "/data/balti.migrate/" + filename + " /data/data/" + "\n" +
                    "cd /data/data/" + "\n" +
                    "rm -r " + dirName + "\n" +
                    TEMP_DIR_NAME +"/busybox tar -xzpf " + filename + "\n" +
                    "rm " + filename + "\n" +
                    "chmod 755 " + dirName + "\n" +
                    "chmod +r -R " + dirName + "\n" +
                    "rm " + TEMP_DIR_NAME + "/" + filename + "\n" +
                    "rm " + TEMP_DIR_NAME + "/" + scriptName + "\n";

        }*/

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

    void unpackBinaries(){

        zipBinaryFilePath = unpackAssetToInternal("zip", "zip");
        busyboxBinaryFilePath = unpackAssetToInternal("busybox", "busybox");
    }


    private String unpackAssetToInternal(String assetFileName, String targetFileName){

        AssetManager assetManager = context.getAssets();
        File unpackFile = new File(context.getFilesDir(), targetFileName);
        String path = "";

        int read;
        byte buffer[] = new byte[4096];
        try {
            InputStream inputStream = assetManager.open(assetFileName);
            FileOutputStream writer = new FileOutputStream(unpackFile);
            while ((read = inputStream.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            writer.close();
            unpackFile.setExecutable(true);
            path = unpackFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            path = "";
        }

        return path;
    }

    File[] makeScripts() {

        unpackBinaries();
        if (busyboxBinaryFilePath.equals("") || zipBinaryFilePath.equals(""))
            return new File[]{null, null};

        File backupSummary = new File(context.getFilesDir(), "backup_summary");
        File script = new File(context.getFilesDir(), "script.sh");
        File updater_script = new File(context.getFilesDir(), "updater-script");
        File helper = new File(context.getFilesDir() + "/system/app/MigrateHelper", "MigrateHelper.apk");

        String update_binary = unpackAssetToInternal("update-binary", "update-binary");
        String prepScript = unpackAssetToInternal("prep.sh", "prep.sh");

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
            updater_writer.write("ui_print(\"----------------------\");\n");
            updater_writer.write("ui_print(\"Migrate Flash package\");\n");
            updater_writer.write("ui_print(\"----------------------\");\n");
            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Mounting partition...\");\n");
            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"mount\", \"/system\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"mount\", \"/data\");\n");

            updater_writer.write("ui_print(\"Making Migrate cache directories...\");\n");
            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("package_extract_file(\"" + "prep.sh" + "\", \"" + "/tmp/prep.sh" + "\");\n");
            updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + "/tmp/prep.sh" + "\");\n");
            updater_writer.write("run_program(\"" + "/tmp/prep.sh" + "\");\n");

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

            String contactsFileName = "Contacts_" + new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Calendar.getInstance().getTime()) + ".vcf";
            if (doBackupContacts) {
                updater_writer.write("ui_print(\" \");\n");
                updater_writer.write("ui_print(\"Extracting contacts: " + contactsFileName + "\");\n");
                updater_writer.write("package_extract_file(\"" + contactsFileName + "\", \"" + TEMP_DIR_NAME + "/" + contactsFileName + "\");\n");
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
            updater_writer.write("ui_print(\"*****\");\n");
            updater_writer.write("ui_print(\"Files have been restored to Migrate cache.\");\n");
            updater_writer.write("ui_print(\"PLEASE MAKE SURE THAT YOUR ROM IS ROOTED.\");\n");
            updater_writer.write("ui_print(\"YOU WILL BE PROMPTED TO CONTINUE RESTORE AFTER STARTUP!!\");\n");
            updater_writer.write("ui_print(\"*****\");\n");
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

    String backupContacts(NotificationManager notificationManager, NotificationCompat.Builder progressNotif){

        StringBuilder errors = new StringBuilder();

        if (!doBackupContacts)
            return errors.toString();

        (new File(destination + "/" + backupName)).mkdirs();


        progressNotif.setContentTitle(context.getString(R.string.backing_contacts))
                .setProgress(0, 0, false)
                .setContentText("");
        notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
        String vcfFileName = "Contacts_" + sdf.format(Calendar.getInstance().getTime()) + ".vcf";
        String vcfFilePath = destination + "/" + backupName + "/" + vcfFileName;

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

}
