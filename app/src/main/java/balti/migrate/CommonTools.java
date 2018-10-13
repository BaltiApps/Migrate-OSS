package balti.migrate;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class CommonTools {

    Context context;
    public static String DEBUG_TAG = "migrate_tag";

    public CommonTools(Context context) {
        this.context = context;
    }

    String unpackAssetToInternal(String assetFileName, String targetFileName){

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

    void reportLogs(boolean isErrorLogMandatory){
        final File progressLog = new File(context.getExternalCacheDir(), "progressLog");
        final File errorLog = new File(context.getExternalCacheDir(), "errorLog");

        if (errorLog.exists() && progressLog.exists()) {

            View errorReportView = View.inflate(context, R.layout.error_report_layout, null);

            final CheckBox shareProgress, shareErrors;
            shareProgress = errorReportView.findViewById(R.id.share_progress_checkbox);
            shareErrors = errorReportView.findViewById(R.id.share_errors_checkbox);

            if (isErrorLogMandatory){
                shareErrors.setChecked(true);
                shareErrors.setEnabled(false);
            }
            else {
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

                            if (shareErrors.isChecked()) uris.add(FileProvider.getUriForFile(context, "migrate.provider", errorLog));
                            if (shareProgress.isChecked()) uris.add(FileProvider.getUriForFile(context, "migrate.provider", progressLog));

                            emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

                            try {
                                context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.select_mail)));
                                Toast.makeText(context, context.getString(R.string.select_mail), Toast.LENGTH_SHORT).show();
                            } catch (Exception e) { Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show(); }

                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
        else {

            String msg = "";
            if (!progressLog.exists())
                msg += context.getString(R.string.progress_log_does_not_exist) + "\n";
            if (!errorLog.exists())
                msg += context.getString(R.string.error_log_does_not_exist) + "\n";

            new AlertDialog.Builder(context)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }


    }

    String getDeviceSpecifications(){

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

    boolean isServiceRunning(String name){
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if (name.equals(service.service.getClassName()))
                return true;
        }
        return false;
    }
}
