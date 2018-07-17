package balti.migrate;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by sayantan on 9/10/17.
 */

public class BackupEngine {

    public static final int NOTIFICATION_ID = 100;

    private String backupSummary, backupPackageNames;
    private String destination, backupName;
    private Context context;

    SharedPreferences main;

    private Process suProcess = null;
    private int n = 0, compressionLevel;
    private String finalMessage = "";
    private ArrayList<String> errors;

    private Process getNumberOfFiles;
    private Process zipProcess;

    private Intent logBroadcast;

    private boolean isCancelled = false;

    final String TEMP_DIR_NAME = "/data/balti.migrate";

    BackupEngine(String backupSummary, String backupPackageNames, String backupName, int compressionLevel, String destination, Context context) {
        this.backupSummary = backupSummary;
        this.backupPackageNames = backupPackageNames;
        this.backupName = backupName;
        this.destination = destination;
        this.compressionLevel = compressionLevel;
        (new File(destination)).mkdirs();
        this.context = context;

        getNumberOfFiles = zipProcess = null;

        main = context.getSharedPreferences("main", Context.MODE_PRIVATE);
        logBroadcast = new Intent("Migrate log broadcast");

        errors = new ArrayList<>(0);
        n = backupSummary.split("\r\n|\r|\n").length;
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

    void initiateBackup() {

        errors = new ArrayList<>(0);

        Intent progressBroadcast = new Intent("Migrate progress broadcast");
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder progressNotif = createNotificationBuilder();

        notificationManager.cancel(NOTIFICATION_ID + 1);
        progressNotif.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.backingUp));

        Intent cancelIntent = new Intent("Migrate backup cancel broadcast");
        Intent activityIntent = new Intent(context, BackupProgressLayout.class);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, 0, cancelIntent, 0);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(context, 1, activityIntent, 0);

        progressNotif.setContentIntent(activityPendingIntent);

        NotificationCompat.Action cancelAction = new NotificationCompat.Action(0, context.getString(android.R.string.cancel), cancelPendingIntent);

        try {
            File receivedFiles[] = makeScripts();
            if (isCancelled) throw new InterruptedIOException();
            if (receivedFiles[0] != null && receivedFiles[1] != null) {
                suProcess = Runtime.getRuntime().exec("su -c sh " + receivedFiles[0].getAbsolutePath());
                BufferedReader outputStream = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));
                BufferedReader errorStream = new BufferedReader(new InputStreamReader(suProcess.getErrorStream()));
                String line;
                int c = 0, p;
                progressNotif.addAction(cancelAction);

                logBroadcast.putExtra("type", "progress");

                while ((line = outputStream.readLine()) != null) {

                    if (line.trim().startsWith("migrate status:")) {
                        line = line.substring(16);
                        p = c++ * 100 / n;
                        progressBroadcast.putExtra("task", line);

                        if (p < 100) {

                            progressNotif.setProgress(n, c - 1, false)
                                    .setContentIntent(activityPendingIntent)
                                    .setContentText(line);
                            notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                            progressBroadcast.putExtra("progress", p);
                            context.sendBroadcast(progressBroadcast);
                        }
                    }

                    logBroadcast.putExtra("content", line);
                    context.sendBroadcast(logBroadcast);
                }

                while ((line = errorStream.readLine()) != null) {
                    errors.add(line);
                }
            } else {
                errors.add(context.getString(R.string.errorMakingScripts));
            }

            String zipErr = zipAll(progressBroadcast, notificationManager, progressNotif);
            if (!zipErr.equals("")) errors.add(zipErr);

        } catch (Exception e) {
            if (!isCancelled){
                errors.add(e.getMessage());
            }
        }

        progressNotif = createNotificationBuilder();
        progressNotif.setSmallIcon(R.mipmap.ic_launcher);

        if (isCancelled) finalMessage = context.getString(R.string.backupCancelled);
        else if (errors.size() == 0) finalMessage = context.getString(R.string.noErrors);
        else {
            finalMessage = context.getString(R.string.backupFinishedWithErrors);
            progressNotif.setContentText(errors.get(0));
        }

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


        activityIntent.setAction("finished").putExtra("finishedMessage", finalMessage).putStringArrayListExtra("errors", errors);
        activityPendingIntent = PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        progressNotif
                .setProgress(0, 0, false)
                .setContentIntent(activityPendingIntent)
                .setAutoCancel(true)
                .mActions.clear();

        notificationManager.notify(NOTIFICATION_ID + 1, progressNotif.build());

        logBroadcast.putExtra("type", "progress");
        logBroadcast.putExtra("content", "\n\n" + finalMessage.trim());
        context.sendBroadcast(logBroadcast);

        logBroadcast.putExtra("type", "errors");
        logBroadcast.putStringArrayListExtra("content", errors);
        context.sendBroadcast(logBroadcast);

        progressBroadcast.putExtra("progress", 100);
        progressBroadcast.putExtra("task", finalMessage.trim());
        context.sendBroadcast(progressBroadcast);
    }

    private String zipAll(Intent progressBroadcast, NotificationManager notificationManager, NotificationCompat.Builder progressNotif){

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
            String cmds = "#!/sbin/sh\n\ncd " + destination + "/" + backupName + "\nzip " + backupName + ".zip -" + compressionLevel + "vurm *\nmv " + backupName + ".zip " + destination + "\nrm -r " + destination + "/" + backupName + "\nrm " + tempScript.getAbsolutePath() + "\n";
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
                progressBroadcast.putExtra("task", context.getString(R.string.combining))
                        .putExtra("progress", pr);
                context.sendBroadcast(progressBroadcast);

                progressNotif.setProgress(n, c, false);
                notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                logBroadcast.putExtra("content", l);
                context.sendBroadcast(logBroadcast);
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

    Vector<File> getAllFiles(File directory, Vector<File> allFiles){
        File files[] = directory.listFiles();
        for (File f : files){
            if (f.isFile())
                allFiles.addElement(f);
            else {
                allFiles.addElement(f);
                getAllFiles(f, allFiles);
            }
        }
        return allFiles;
    }

    private String javaZipAll(Intent progressBroadcast, NotificationManager notificationManager, NotificationCompat.Builder progressNotif){

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
            int c = 0;

            File directory = new File(destination + "/" + backupName);
            Vector<File> files = getAllFiles(directory, new Vector<File>(1));

            File zipFile = new File(destination + "/" + backupName + ".zip");

            FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

            int part = 0;

            for (File file : files)
            {

                String fn = file.getAbsolutePath();
                fn = fn.substring(directory.getAbsolutePath().length() + 1);
                ZipEntry zipEntry;

                if (file.isDirectory()) {
                    zipEntry = new ZipEntry(fn + "/");
                    zipOutputStream.putNextEntry(zipEntry);
                    zipOutputStream.closeEntry();
                    continue;
                }
                else {
                    zipEntry = new ZipEntry(fn);
                    zipOutputStream.putNextEntry(zipEntry);

                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fileInputStream.read(buffer)) > 0)
                    {
                        zipOutputStream.write(buffer, 0, read);
                    }
                    zipOutputStream.closeEntry();
                    fileInputStream.close();
                    file.delete();
                }

                if (zipFile.length() >= 1000000){
                    zipOutputStream.close();
                    zipFile = new File(destination + "/" + backupName + "_part" + ++part + ".zip");
                    fileOutputStream = new FileOutputStream(zipFile);
                    zipOutputStream = new ZipOutputStream(fileOutputStream);
                }

                int pr = c++*100 / n;
                if (pr == 100) pr = 99;
                progressBroadcast.putExtra("task", context.getString(R.string.combining))
                        .putExtra("progress", pr);
                context.sendBroadcast(progressBroadcast);

                progressNotif.setProgress(n, c, false);
                notificationManager.notify(NOTIFICATION_ID, progressNotif.build());

                logBroadcast.putExtra("content", "compressed: " +file.getName());
                context.sendBroadcast(logBroadcast);
            }
            zipOutputStream.close();
            fullDelete(directory.getAbsolutePath());

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

    void makeRemoteScript(String filename, boolean isApp) {

        String scriptName = filename + ".sh";
        String scriptLocation = destination + "/" + backupName + "/" + scriptName;
        File script = new File(scriptLocation);

        String scriptText;

        (new File(destination + "/" + backupName)).mkdirs();

        if (isApp){

            filename = filename + ".apk";

            scriptText = "#!sbin/sh\n\n" +
                    "cd " + TEMP_DIR_NAME + "\n" +
                    "pm install -r " + filename + "\n" +
                    "rm " + filename + "\n" +
                    "rm " + scriptName + "\n";

        }

        else {

            String dirName = filename.substring(0, filename.lastIndexOf('-'));

            filename = filename + ".tar.gz";

            scriptText = "#!sbin/sh\n\n" +
                    "cp " + "/data/balti.migrate/" + filename + " /data/data/" + "\n" +
                    "cd /data/data/" + "\n" +
                    "rm -r " + dirName + "\n" +
                    "tar -xzpf " + filename + "\n" +
                    "chmod 755 " + dirName + "\n" +
                    "chmod +r -R " + dirName + "\n" +
                    "rm " + TEMP_DIR_NAME + "/" + filename + "\n" +
                    "rm " + TEMP_DIR_NAME + "/" + scriptName + "\n";

        }

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

    String makePermissionList(){
        File permissionList = new File(context.getFilesDir() + "/permissionList");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(permissionList));
            BufferedReader reader = new BufferedReader(new StringReader(backupPackageNames));
            String line;
            while ((line = reader.readLine()) != null)
                writer.write(line + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return permissionList.getAbsolutePath();
    }

    File[] makeScripts() {
        File script = new File(context.getFilesDir(), "script.sh");
        File updater_script = new File(context.getFilesDir(), "updater-script");
        File update_binary = new File(context.getFilesDir(), "update-binary");
        File helper = new File(context.getFilesDir() + "/system/app/MigrateHelper", "MigrateHelper.apk");
        File prepScript = new File(context.getFilesDir(), "prep.sh");

        AssetManager assetManager = context.getAssets();
        int read;
        byte buffer[] = new byte[4096];
        try {
            InputStream inputStream = assetManager.open("update-binary");
            FileOutputStream writer = new FileOutputStream(update_binary);
            while ((read = inputStream.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            InputStream inputStream = assetManager.open("prep.sh");
            FileOutputStream writer = new FileOutputStream(prepScript);
            while ((read = inputStream.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            BufferedReader reader = new BufferedReader(new StringReader(backupSummary));
            BufferedWriter writer = new BufferedWriter(new FileWriter(script));
            BufferedWriter updater_writer = new BufferedWriter(new FileWriter(updater_script));

            updater_writer.write("show_progress(0, 0);\n");
            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"----------------------\");\n");
            updater_writer.write("ui_print(\"Migrate Flash package\");\n");
            updater_writer.write("ui_print(\"----------------------\");\n");
            updater_writer.write("ui_print(\" \");\n");
/*
            String system, systemType, data, dataType;
            boolean isMountDataAvailable = true;

            system = main.getString("system", "");
            systemType = main.getString("systemType", "");
            data = main.getString("data", "");
            dataType = main.getString("dataType", "");

            if (system.equals("") || systemType.equals("") || data.equals("") || dataType.equals(""))
                isMountDataAvailable = false;*/

            updater_writer.write("ui_print(\"Mounting partition...\");\n");
            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"mount\", \"/system\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"mount\", \"/data\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"mount\", \"/cache\");\n");

            updater_writer.write("ui_print(\"Making directories...\");\n");
            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("package_extract_file(\"" + "prep.sh" + "\", \"" + "/cache/prep.sh" + "\");\n");
            updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + "/cache/prep.sh" + "\");\n");
            updater_writer.write("run_program(\"" + "/cache/prep.sh" + "\");\n");

            updater_writer.write("ui_print(\"Restoring...\");\n");
            updater_writer.write("ui_print(\" \");\n");

            String line;

            String mkdirCommand = "mkdir -p " + destination + "/" + backupName + "/";
            writer.write(mkdirCommand + '\n', 0, mkdirCommand.length() + 1);

            int c = 0;

            while ((line = reader.readLine()) != null) {
                String echoCopyCommand = "echo \"migrate status: " + line.substring(0, line.lastIndexOf(' ')) + "\"";

                String path = line.substring(line.lastIndexOf(' ') + 1);
                String zipOrCopy;
                String fileName = getFileName(line);
                String pastingDir = line.substring(line.lastIndexOf(' ') + 1, line.lastIndexOf('/'));

                boolean isApp = isApp(line);
                if (isApp) zipOrCopy = "cd " + path.substring(0, path.lastIndexOf('/')) + "; cp " + path.substring(path.lastIndexOf('/') + 1) + "/*.apk " + destination + "/" + backupName + "/" + fileName + ".apk";
                else zipOrCopy = "cd " + path.substring(0, path.lastIndexOf('/')) + "; tar -cvzpf " + destination + "/" + backupName + "/" + fileName + ".tar.gz " + path.substring(path.lastIndexOf('/') + 1);

                String display = line.substring(0, line.lastIndexOf(' '));
                updater_writer.write("ui_print(\"" + display + "\");\n");

                if (isApp && path.startsWith("/system")){
                    updater_writer.write("package_extract_file(\"" + fileName + ".apk" + "\", \"" + pastingDir + "/" + fileName + ".apk" + "\");\n");
                }
                else if (isApp){
                    updater_writer.write("package_extract_file(\"" + fileName + ".apk" + "\", \"" + TEMP_DIR_NAME + "/" + fileName + ".apk" + "\");\n");
                    makeRemoteScript(fileName, true);
                    updater_writer.write("package_extract_file(\"" + fileName + ".sh" + "\", \"" + TEMP_DIR_NAME + "/" + fileName + ".sh" + "\");\n");
                    updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + TEMP_DIR_NAME + "/" + fileName + ".sh" + "\");\n");
                }
                else {
                    updater_writer.write("package_extract_file(\"" + fileName + ".tar.gz" + "\", \"" + TEMP_DIR_NAME + "/" + fileName + ".tar.gz" + "\");\n");
                    makeRemoteScript(fileName, false);
                    updater_writer.write("package_extract_file(\"" + fileName + ".sh" + "\", \"" + TEMP_DIR_NAME + "/" + fileName + ".sh" + "\");\n");
                    updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + TEMP_DIR_NAME + "/" + fileName + ".sh" + "\");\n");
                }


                updater_writer.write("set_progress(" + String.format("%.4f", ((++c * 1.0) / n)) + ");\n");


                writer.write(echoCopyCommand + '\n', 0, echoCopyCommand.length() + 1);
                writer.write(zipOrCopy + '\n', 0, zipOrCopy.length() + 1);
            }

            /*BufferedReader reader2 = new BufferedReader(new StringReader(backupSummary));
            while ((line = reader2.readLine()) != null) {
                String display = line.substring(0, line.lastIndexOf(' '));
                updater_writer.write("ui_print(\"" + display + "\");\n");

                String pastingDir = getPastingDir(line);
                String fileName = getFileName(line);

                updater_writer.write("package_extract_file(\"" + fileName + ".tar.gz" + "\", \"" + pastingDir + "/" + fileName + ".tar.gz" + "\");\n");
                updater_writer.write("package_extract_file(\"" + fileName + ".sh" + "\", \"" + pastingDir + "/" + fileName + ".sh" + "\");\n");
                updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + pastingDir + "/" + fileName + ".sh" + "\");\n");
                updater_writer.write("run_program(\"" + pastingDir + "/" + fileName + ".sh" + "\");\n");

                updater_writer.write("set_progress(" + String.format("%.4f", ((++c * 1.0) / n)) + ");\n");
            }*/

            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Unpacking helper\");\n");
            updater_writer.write("package_extract_dir(\"system\", \"/system\");\n");
            updater_writer.write("set_perm_recursive(0, 0, 0777, 0777,  \"" + "/system/app/MigrateHelper/" + "\");\n");
            updater_writer.write("package_extract_file(\"permissionList\", \"/cache/permissionList\");\n");
            updater_writer.write("set_progress(1.0000);\n");
            updater_writer.write("ui_print(\" \");\n");

            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Unmounting partitions...\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"umount\", \"/system\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"umount\", \"/data\");\n");
            updater_writer.write("run_program(\"/sbin/busybox\", \"umount\", \"/cache\");\n");


            updater_writer.write("ui_print(\" \");\n");
            updater_writer.write("ui_print(\"Finished!\");\n");
            updater_writer.write("ui_print(\"*****\");\n");
            updater_writer.write("ui_print(\"YOU WILL BE PROMPTED TO CONTINUE RESTORE AFTER STARTUP!!\");\n");
            updater_writer.write("ui_print(\"*****\");\n");
            updater_writer.write("ui_print(\" \");\n");

            updater_writer.close();

            String flashDirPath = destination + "/" + backupName + "/META-INF/com/google/android/";

            writer.write("echo \"migrate status: " + "Making package Flash Ready" + "\"\n");
            writer.write("mkdir -p " + flashDirPath + "\n");
            /*writer.write("cp " + updater_script.getAbsolutePath() + " " + flashDirPath + "\n");
            writer.write("cp " + update_binary.getAbsolutePath() + " " + flashDirPath + "\n");*/

            writer.write("echo \"migrate status: " + "Including helper" + "\"\n");
            writer.write("cp -r " + context.getFilesDir() + "/system" + " " + destination + "/" + backupName + "\n");
            writer.write("mv " + makePermissionList() + " " + destination + "/" + backupName + "\n");
            writer.write("rm -r " + context.getFilesDir() + "/system" + "\n");

            writer.write("mkdir -p " + flashDirPath + "\n");
            writer.write("mv " + updater_script.getAbsolutePath() + " " + flashDirPath + "\n");
            writer.write("mv " + update_binary.getAbsolutePath() + " " + flashDirPath + "\n");
            writer.write("mv " + prepScript.getAbsolutePath() + " " + destination + "/" + backupName + "\n");

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

    boolean isApp(String line){
        return line.substring(0, line.indexOf(':')).equals("APP");
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
        //finalMessage = context.getString(R.string.backupCancelled);
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

    /*String getPastingDir(String line) {
        String pastingDir = line.substring(line.lastIndexOf(' ') + 1, line.lastIndexOf('/'));
        String type = line.substring(0, line.indexOf(':'));
        if (type.equals("DATA") || pastingDir.startsWith("/data"))
            pastingDir = TEMP_DIR_NAME;
        pastingDir = pastingDir + '/';
        return pastingDir;
    }*/

    String getFileName(String line) {
        String path = line.substring(line.lastIndexOf(' ') + 1);
        return path.substring(path.lastIndexOf('/') + 1) + "-" + line.substring(0, line.indexOf(':')).toLowerCase();
    }
}
