package balti.migrate;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

/**
 * Created by sayantan on 8/10/17.
 */

public class BackupActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, OnCheck {

    ListView listView;
    CheckBox appAllSelect, dataAllSelect;
    Spinner appType;
    Button startBackupButton;
    ImageButton selectAll, clearAll, backButton;

    PackageManager pm;
    Vector<BackupDataPacket> appList;
    AppListAdapter adapter;

    String destination;

    LayoutInflater layoutInflater;
    SharedPreferences main;

    BroadcastReceiver progressReceiver;

    class AppUpdate extends AsyncTask{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setBackupEnabled(false);
            listView.setAdapter(null);

            appAllSelect.setEnabled(false);
            dataAllSelect.setEnabled(false);
            selectAll.setEnabled(false);
            clearAll.setEnabled(false);

            (findViewById(R.id.appLoadingView)).setVisibility(View.VISIBLE);
        }

        @Override
        protected Object doInBackground(Object[] params) {

            if (isBackupRunning()){
                startActivity(new Intent(BackupActivity.this, BackupProgressLayout.class));
                finish();
            }
            updateAppsList((int)params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            appAllSelect.setEnabled(true);
            dataAllSelect.setEnabled(true);
            selectAll.setEnabled(true);
            clearAll.setEnabled(true);

            (findViewById(R.id.appLoadingView)).setVisibility(View.GONE);
            listView.setAdapter(adapter);
        }
    }

    class MakeBackupSummary extends AsyncTask<Void, Void, Object[]>{

        AlertDialog ad;
        String backupName;

        MakeBackupSummary(String backupName) {
            this.backupName = backupName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            ad = new AlertDialog.Builder(BackupActivity.this)
                    .setView(layoutInflater.inflate(R.layout.please_wait, null))
                    .setCancelable(false)
                    .create();

            ad.show();
        }

        @Override
        protected Object[] doInBackground(Void... voids) {

            File backupSummary = new File(getFilesDir(), "backup_summary");

            try {

                BufferedWriter writer = new BufferedWriter(new FileWriter(backupSummary.getAbsolutePath()));

                for (int i = 0; i < appList.size(); i++) {

                    if (appList.elementAt(i).APP) {

                        String appName = pm.getApplicationLabel(appList.get(i).PACKAGE_INFO.applicationInfo).toString();
                        appName = appName.replace(' ', '_');

                        String packageName = appList.get(i).PACKAGE_INFO.packageName;
                        String apkPath = appList.elementAt(i).PACKAGE_INFO.applicationInfo.sourceDir;
                        String dataPath = "NULL";
                        if (appList.get(i).DATA)
                            dataPath = appList.elementAt(i).PACKAGE_INFO.applicationInfo.dataDir;
                        String versionName = appList.elementAt(i).PACKAGE_INFO.versionName;

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
        protected void onPostExecute(Object[] o) {
            super.onPostExecute(o);
            ad.dismiss();
            if ((boolean)o[0]){
                Intent bService = new Intent(BackupActivity.this, BackupService.class)
                        .putExtra("backupName", backupName)
                        .putExtra("compressionLevel", main.getInt("compressionLevel", 0))
                        .putExtra("destination", destination);


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(bService);
                else startService(bService);
            }
            else {
                Toast.makeText(BackupActivity.this, (String)o[1], Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backup_layout);

        layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        main = getSharedPreferences("main", MODE_PRIVATE);

        destination = main.getString("defaultBackupPath", Environment.getExternalStorageDirectory() + "/Migrate");

        pm = getPackageManager();

        listView = findViewById(R.id.appBackupList);

        appAllSelect = findViewById(R.id.appAllSelect);
        dataAllSelect = findViewById(R.id.dataAllSelect);

        startBackupButton = findViewById(R.id.backupStartButton);
        selectAll = findViewById(R.id.selectAll);
        clearAll = findViewById(R.id.clearAll);
        backButton = findViewById(R.id.backupLayoutBackButton);

        appType = findViewById(R.id.appType);
        appType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                AppUpdate appUpdate = new AppUpdate();
                appUpdate.execute(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        appAllSelect.setOnCheckedChangeListener(this);
        dataAllSelect.setOnCheckedChangeListener(this);

        startBackupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askForBackupName();
            }
        });

        selectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataAllSelect.setChecked(true);
            }
        });

        clearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataAllSelect.setChecked(true);
                dataAllSelect.setChecked(false);
                appAllSelect.setChecked(false);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent progressActivityStartIntent = new Intent(BackupActivity.this, BackupProgressLayout.class);
                progressActivityStartIntent.putExtras(Objects.requireNonNull(intent.getExtras()));
                progressActivityStartIntent.setAction("Migrate progress broadcast");
                startActivity(progressActivityStartIntent);
                finish();
            }
        };
        registerReceiver(progressReceiver, new IntentFilter("Migrate progress broadcast"));

        sendBroadcast(new Intent("get data"));

    }



    void updateAppsList(int i) {
        appAllSelect.setOnCheckedChangeListener(null);
        appAllSelect.setChecked(false);
        appAllSelect.setOnCheckedChangeListener(this);
        dataAllSelect.setOnCheckedChangeListener(null);
        dataAllSelect.setChecked(false);
        dataAllSelect.setOnCheckedChangeListener(this);

        if (i == 2) {
            List<PackageInfo> tempAppList = pm.getInstalledPackages(0);
            appList = new Vector<>(1);
            for (int k = 0; k < tempAppList.size(); k++) {
                BackupDataPacket packet = new BackupDataPacket();
                packet.PACKAGE_INFO = tempAppList.get(k);
                packet.APP = false;
                packet.DATA = false;
                appList.add(packet);
            }
            adapter = new AppListAdapter(BackupActivity.this, appList);
        } else if (i == 1) {
            List<PackageInfo> tempAppList = pm.getInstalledPackages(0);
            appList = new Vector<>(1);
            for (int k = 0; k < tempAppList.size(); k++) {
                if ((tempAppList.get(k).applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) > 0) {
                    BackupDataPacket packet = new BackupDataPacket();
                    packet.PACKAGE_INFO = tempAppList.get(k);
                    packet.APP = false;
                    packet.DATA = false;
                    appList.add(packet);
                }
            }
            adapter = new AppListAdapter(BackupActivity.this, appList);
        } else if (i == 0) {
            List<PackageInfo> tempAppList = pm.getInstalledPackages(0);
            appList = new Vector<>(1);
            for (int k = 0; k < tempAppList.size(); k++) {
                if ((tempAppList.get(k).applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0) {
                    BackupDataPacket packet = new BackupDataPacket();
                    packet.PACKAGE_INFO = tempAppList.get(k);
                    packet.APP = false;
                    packet.DATA = false;
                    appList.add(packet);
                }
            }
            adapter = new AppListAdapter(BackupActivity.this, appList);
        }
    }


    @Override
    public void onCheck(Vector<BackupDataPacket> backupDataPackets) {
        appList = backupDataPackets;
        boolean app, data;
        boolean enable = false;
        if (appList.size() > 0)
            app = data = true;
        else app = data = false;
        for (int i = 0; i < backupDataPackets.size(); i++) {
            app = app && appList.elementAt(i).APP;
            data = data && appList.elementAt(i).DATA;
            enable = enable || appList.elementAt(i).APP || appList.elementAt(i).DATA;
        }

        setBackupEnabled(enable);

        dataAllSelect.setOnCheckedChangeListener(null);
        dataAllSelect.setChecked(data);
        dataAllSelect.setOnCheckedChangeListener(this);
        appAllSelect.setOnCheckedChangeListener(null);
        appAllSelect.setChecked(app);
        if (dataAllSelect.isChecked())
            appAllSelect.setEnabled(false);
        else appAllSelect.setEnabled(true);
        appAllSelect.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        if (compoundButton == appAllSelect) {
            adapter.checkAllApp(b);
            adapter.notifyDataSetChanged();
        } else if (compoundButton == dataAllSelect) {
            if (b){
                appAllSelect.setChecked(true);
                appAllSelect.setEnabled(false);
            }
            else {
                appAllSelect.setEnabled(true);
            }
            adapter.checkAllData(b);
            adapter.notifyDataSetChanged();
        }

        setBackupEnabled(dataAllSelect.isChecked() || appAllSelect.isChecked());
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
                        else Toast.makeText(BackupActivity.this, getString(R.string.empty), Toast.LENGTH_SHORT).show();
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
            new AlertDialog.Builder(BackupActivity.this)
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


        /*Intent bService = new Intent(BackupActivity.this, BackupService.class)
                .setAction("start service")
                .putExtra("backupSummary", backupSummary)
                .putExtra("backupName", backupName)
                .putExtra("backupPackageNames", backupPackageNames)
                .putExtra("compressionLevel", main.getInt("compressionLevel", 0))
                .putExtra("destination", destination);

        *//*if (!wasProgressDialogShown)
            createOnProgressDialog();*//*

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(bService);
        else startService(bService);

        startActivity(new Intent(this, BackupProgressLayout.class));
        finish();*/
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


    void setBackupEnabled(boolean isEnabled){
        startBackupButton.setEnabled(isEnabled);
    }

    boolean isBackupRunning(){
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if(serviceInfo.service.getClassName().equalsIgnoreCase("balti.migrate.BackupService"))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(progressReceiver);
        }
        catch (Exception ignored){}
    }
}
