package balti.migrate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class ExtraBackups extends AppCompatActivity {


    private String destination;
    private static boolean isAnyAppSelected = false;
    private static List<BackupDataPacket> appList;

    private TextView startBackup;
    private ImageButton back;

    private LayoutInflater layoutInflater;

    private SharedPreferences main;
    private BroadcastReceiver progressReceiver;
    private AlertDialog ad;
    private PackageManager pm;

    class MakeBackupSummary extends AsyncTask<Void, String, Object[]> {

        String backupName;
        View dialogView;
        TextView waitingHead, waitingProgress;

        MakeBackupSummary(String backupName) {
            this.backupName = backupName;
            dialogView = layoutInflater.inflate(R.layout.please_wait, null);
            waitingHead = dialogView.findViewById(R.id.waiting_head);
            waitingProgress = dialogView.findViewById(R.id.waiting_progress);
            waitingProgress.setVisibility(View.GONE);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            ad = new AlertDialog.Builder(ExtraBackups.this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();

            ad.show();
            waitingHead.setText(R.string.reading_data);
        }

        @Override
        protected Object[] doInBackground(Void... voids) {

            File backupSummary = new File(getFilesDir(), "backup_summary");

            try {

                BufferedWriter writer = new BufferedWriter(new FileWriter(backupSummary.getAbsolutePath()));
                int n = appList.size();

                for (int i = 0; i < n; i++) {

                    if (appList.get(i).APP) {

                        publishProgress((i+1) + " of " + n);

                        String appName = pm.getApplicationLabel(appList.get(i).PACKAGE_INFO.applicationInfo).toString();
                        appName = appName.replace(' ', '_');

                        String packageName = appList.get(i).PACKAGE_INFO.packageName;
                        String apkPath = appList.get(i).PACKAGE_INFO.applicationInfo.sourceDir;
                        String dataPath = "NULL";
                        if (appList.get(i).DATA)
                            dataPath = appList.get(i).PACKAGE_INFO.applicationInfo.dataDir;
                        String versionName = appList.get(i).PACKAGE_INFO.versionName;

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        Bitmap icon = getBitmapFromDrawable(pm.getApplicationIcon(appList.get(i).PACKAGE_INFO.applicationInfo));
                        icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        String appIcon = byteToString(stream.toByteArray());

                        String line = appName + " " + packageName + " " + apkPath + " " + dataPath + " " + appIcon + " " + versionName;

                        writer.write(line + "\n");

                    }

                }
                writer.close();
                return new Object[]{true};
            }
            catch (Exception e){
                e.printStackTrace();
                return new Object[]{false, e.getMessage()};
            }
        }

        @Override
        protected void onProgressUpdate(String ... strings) {
            super.onProgressUpdate(strings);
            waitingProgress.setVisibility(View.VISIBLE);
            waitingProgress.setText(strings[0]);
        }

        @Override
        protected void onPostExecute(Object[] o) {
            super.onPostExecute(o);
            waitingHead.setText(R.string.just_a_minute);
            waitingProgress.setText(R.string.starting_engine);
            if ((boolean)o[0]){
                Intent bService = new Intent(ExtraBackups.this, BackupService.class)
                        .putExtra("backupName", backupName)
                        .putExtra("compressionLevel", main.getInt("compressionLevel", 0))
                        .putExtra("destination", destination);


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(bService);
                else startService(bService);
            }
            else {
                Toast.makeText(ExtraBackups.this, (String)o[1], Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.extra_backups);

        pm = getPackageManager();

        main = getSharedPreferences("main", MODE_PRIVATE);
        destination = main.getString("defaultBackupPath", Environment.getExternalStorageDirectory() + "/Migrate");

        layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        startBackup = findViewById(R.id.startBackupButton);

        startBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isAnyAppSelected)
                askForBackupName();
            }
        });

        back = findViewById(R.id.extraBackupsBackButton);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ExtraBackups.this, BackupActivity.class));
                finish();
            }
        });

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent progressActivityStartIntent = new Intent(ExtraBackups.this, BackupProgressLayout.class);
                progressActivityStartIntent.putExtras(Objects.requireNonNull(intent.getExtras()));
                progressActivityStartIntent.setAction("Migrate progress broadcast");
                startActivity(progressActivityStartIntent);
                finish();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter("Migrate progress broadcast"));

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("get data"));
    }

    static void setAppList(List<BackupDataPacket> al){
        appList = al;
        for (BackupDataPacket packet : appList){
            if (isAnyAppSelected = packet.APP)
                break;
        }
    }

    void askForBackupName(){
        final EditText editText = new EditText(this);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
        editText.setText(getString(R.string.backupLabel) + "_" + sdf.format(Calendar.getInstance().getTime()));

        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.setBackupName))
                .setView(editText)
                .setPositiveButton(getString(android.R.string.ok), null)
                .setNegativeButton(getString(android.R.string.cancel), null)
                .setCancelable(false)
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button okButton = ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String backupName = editText.getText().toString().trim().replace(' ', '_');
                        if (!backupName.equals(""))
                            checkOverwrite(backupName, alertDialog);
                        else Toast.makeText(ExtraBackups.this, getString(R.string.empty), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        alertDialog.show();

    }

    void checkOverwrite(final String name, final AlertDialog alertDialog){
        final File dir = new File(destination + "/" + name);
        final File zip = new File(destination + "/" + name + ".zip");
        if (dir.exists() || zip.exists()){
            new AlertDialog.Builder(ExtraBackups.this)
                    .setTitle(getString(R.string.overwrite))
                    .setMessage(getString(R.string.overwriteMessage))
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (dir.exists()) dirDelete(dir.getAbsolutePath());
                            if (zip.exists()) zip.delete();
                            alertDialog.dismiss();
                            startBackup(name);
                        }
                    })
                    .setNegativeButton(getString(R.string.rename), null)
                    .show();
        }
        else {
            alertDialog.dismiss();
            startBackup(name);
        }
    }

    void startBackup(String backupName){

        MakeBackupSummary obj = new MakeBackupSummary(backupName);
        obj.execute();
    }

    void dirDelete(String path){
        File file = new File(path);
        if (file.exists()) {
            if (!file.isDirectory())
                file.delete();
            else {
                File files[] = file.listFiles();
                for (int i = 0; i < files.length; i++)
                    dirDelete(files[i].getAbsolutePath());
                file.delete();
            }
        }
    }


    String byteToString(byte[] bytes){
        StringBuilder res = new StringBuilder();
        for (byte b : bytes){
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            ad.dismiss();
        }
        catch (Exception ignored){}
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
        }
        catch (Exception ignored){}
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ExtraBackups.this, BackupActivity.class));
    }
}
