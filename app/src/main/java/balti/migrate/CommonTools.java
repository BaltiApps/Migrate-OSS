package balti.migrate;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import static android.os.Environment.getExternalStorageDirectory;

public class CommonTools {

    Context context;
    public static String DEBUG_TAG = "migrate_tag";

    static String MAIN_ACTIVITY_AD_ID = "ca-app-pub-6582325651261661/6749792408";
    static String BACKUP_ACTIVITY_AD_ID = "ca-app-pub-6582325651261661/5791933954";
    static String EXTRA_BACKUPS_ACTIVITY_AD_ID = "ca-app-pub-6582325651261661/5217218882";
    static String BACKUP_PROGRESS_ACTIVITY_AD_ID = "ca-app-pub-6582325651261661/2755664936";

    static String UNIVERSAL_TEST_ID = "ca-app-pub-3940256099942544/6300978111";

    public static String DEFAULT_INTERNAL_STORAGE_DIR = "/sdcard/Migrate";
    static final String TEMP_DIR_NAME = "/data/local/tmp/migrate_cache";

    public CommonTools(Context context) {
        this.context = context;
    }

    String unpackAssetToInternal(String assetFileName, String targetFileName, boolean toInternal) {

        AssetManager assetManager = context.getAssets();
        File unpackFile = null;
        if (toInternal)
            unpackFile = new File(context.getFilesDir(), targetFileName);
        else unpackFile = new File(context.getExternalCacheDir(), targetFileName);
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

    public void reportLogs(boolean isErrorLogMandatory) {
        final File progressLog = new File(context.getExternalCacheDir(), "progressLog.txt");
        final File errorLog = new File(context.getExternalCacheDir(), "errorLog.txt");
        //final File theBackupScript = new File(context.getExternalCacheDir(), "the_backup_script.sh");
        final File[] backupScripts = context.getExternalCacheDir().listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return (f.getName().startsWith("the_backup_script_") || f.getName().startsWith("retry_script_")) && f.getName().endsWith(".sh");
            }
        });

        if (isErrorLogMandatory && !errorLog.exists()) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(context.getString(R.string.error_log_does_not_exist))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else if (errorLog.exists() || progressLog.exists() || backupScripts.length > 0) {

            View errorReportView = View.inflate(context, R.layout.error_report_layout, null);

            final CheckBox shareProgress, shareScript, shareErrors;
            shareProgress = errorReportView.findViewById(R.id.share_progress_checkbox);
            shareErrors = errorReportView.findViewById(R.id.share_errors_checkbox);
            shareScript = errorReportView.findViewById(R.id.share_script_checkbox);

            if (!progressLog.exists()) {
                shareProgress.setChecked(false);
                shareProgress.setEnabled(false);
            } else {
                shareProgress.setEnabled(true);
                shareProgress.setChecked(true);
            }

            if (backupScripts.length == 0) {
                shareScript.setChecked(false);
                shareScript.setEnabled(false);
            } else {
                shareScript.setEnabled(true);
                shareScript.setChecked(true);
            }

            if (isErrorLogMandatory && errorLog.exists()) {
                shareErrors.setChecked(true);
                shareErrors.setEnabled(false);
            } else if (!errorLog.exists()) {
                shareErrors.setChecked(false);
                shareErrors.setEnabled(false);
            } else {
                shareErrors.setChecked(true);
                shareErrors.setEnabled(true);
            }

            new AlertDialog.Builder(context)
                    .setView(errorReportView)
                    .setPositiveButton(R.string.agree_and_send, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            String body = getDeviceSpecifications();

                            Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                            emailIntent.setType("text/plain");
                            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"help.baltiapps@gmail.com"});
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Log report for Migrate");
                            emailIntent.putExtra(Intent.EXTRA_TEXT, body);

                            ArrayList<Uri> uris = new ArrayList<>(0);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                if (shareErrors.isChecked())
                                    uris.add(FileProvider.getUriForFile(context, "migrate.provider", errorLog));
                                if (shareProgress.isChecked())
                                    uris.add(FileProvider.getUriForFile(context, "migrate.provider", progressLog));
                                if (shareScript.isChecked()) {
                                    for (File f : backupScripts)
                                        uris.add(FileProvider.getUriForFile(context, "migrate.provider", f));
                                }
                            } else {
                                if (shareErrors.isChecked())
                                    uris.add(Uri.fromFile(errorLog));
                                if (shareProgress.isChecked())
                                    uris.add(Uri.fromFile(progressLog));
                                if (shareScript.isChecked()) {
                                    for (File f : backupScripts)
                                        uris.add(Uri.fromFile(f));
                                }
                            }

                            emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

                            try {
                                context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.select_mail)));
                                Toast.makeText(context, context.getString(R.string.select_mail), Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }

                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {

            String msg = "";
            if (!progressLog.exists())
                msg += context.getString(R.string.progress_log_does_not_exist) + "\n";
            if (!errorLog.exists())
                msg += context.getString(R.string.error_log_does_not_exist) + "\n";
            msg += context.getString(R.string.backup_script_does_not_exist) + "\n";

            new AlertDialog.Builder(context)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }


    }

    public String getDeviceSpecifications() {

        String body = "";

        body = body + "CPU_ABI: " + Build.SUPPORTED_ABIS[0] + "\n";
        body = body + "Brand: " + Build.BRAND + "\n";
        body = body + "Manufacturer: " + Build.MANUFACTURER + "\n";
        body = body + "Model: " + Build.MODEL + "\n";
        body = body + "Device: " + Build.DEVICE + "\n";
        body = body + "SDK: " + Build.VERSION.SDK_INT + "\n";
        body = body + "Board: " + Build.BOARD + "\n";
        body = body + "Hardware: " + Build.HARDWARE;

        return body;
    }

    boolean isServiceRunning(String name) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (name.equals(service.service.getClassName()))
                return true;
        }
        return false;
    }

    public Object[] suEcho() throws IOException, InterruptedException {
        Process suRequest = Runtime.getRuntime().exec("su");

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(suRequest.getOutputStream()));

        //writer.write("pm grant " + context.getPackageName() + " android.permission.DUMP\n" );
        //writer.write("pm grant " + context.getPackageName() + " android.permission.PACKAGE_USAGE_STATS\n" );
        writer.write("exit\n");
        writer.flush();

        BufferedReader errorReader = new BufferedReader(new InputStreamReader(suRequest.getErrorStream()));
        BufferedReader outputReader = new BufferedReader(new InputStreamReader(suRequest.getInputStream()));

        String line;
        String errorMessage = "";

        while ((line = outputReader.readLine()) != null) {
            errorMessage = errorMessage + line + "\n";
        }
        errorMessage = errorMessage + "Error:\n\n";
        while ((line = errorReader.readLine()) != null) {
            errorMessage = errorMessage + line + "\n";
        }

        suRequest.waitFor();
        return new Object[]{suRequest.exitValue() == 0, errorMessage};
    }


    long getDirLength(String directoryPath) {
        File file = new File(directoryPath);
        if (file.exists()) {
            if (!file.isDirectory())
                return file.length();
            else {
                File files[] = file.listFiles();
                long sum = 0;
                for (int i = 0; i < files.length; i++)
                    sum += getDirLength(files[i].getAbsolutePath());
                return sum;
            }
        } else return 0;
    }

    public String getHumanReadableStorageSpace(long space) {
        String res = "KB";

        double s = space;

        if (s > 1024) {
            s = s / 1024.0;
            res = "MB";
        }
        if (s > 1024) {
            s = s / 1024.0;
            res = "GB";
        }

        return String.format("%.2f", s) + " " + res;
    }

    public String[] getSdCardPaths() {
        String possibleSDCards[] = new String[0];
        File storage = new File("/storage/");
        if (storage.exists() && storage.canRead()) {
            File[] files = storage.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory() && pathname.canRead()
                            && !pathname.getAbsolutePath().equals(getExternalStorageDirectory().getAbsolutePath());
                }
            });
            possibleSDCards = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                File sd_dir = new File("/mnt/media_rw/" + file.getName());
                if (sd_dir.exists() && sd_dir.isDirectory() && sd_dir.canWrite())
                    possibleSDCards[i] = sd_dir.getAbsolutePath();
                else possibleSDCards[i] = "";
            }
        }
        return possibleSDCards;
    }

    public void showSdCardSupportDialog(){
        View view = View.inflate(context, R.layout.learn_about_sd_card, null);
        new AlertDialog.Builder(context)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public void openWeblink(String url){
        if (!url.equals("")) {
            Intent page = new Intent(Intent.ACTION_VIEW);
            page.setData(Uri.parse(url));
            context.startActivity(page);
        }
    }

    String applyNamingCorrectionForShell(String name){
        name = name.replace("(", "\\(");
        name = name.replace(")", "\\)");
        name = name.replace(" ", "\\ ");
        return name;
    }
}
