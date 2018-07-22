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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

/**
 * Created by sayantan on 8/10/17.
 */

public class BackupActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, OnCheck {

    ListView listView;
    CheckBox appAllSelect, dataAllSelect;
    Spinner appType;
    Button startBackupButton;
    ImageButton selectAll, clearAll;

    PackageManager pm;
    Vector<BackupDataPacket> appList;
    AppListAdapter adapter;

    String destination;


    LayoutInflater layoutInflater;
    SharedPreferences main;

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
            selectAll.setBackgroundColor(getResources().getColor(R.color.lightGray));
            clearAll.setBackgroundColor(getResources().getColor(R.color.lightGray));

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
            selectAll.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            clearAll.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

            (findViewById(R.id.appLoadingView)).setVisibility(View.GONE);
            listView.setAdapter(adapter);
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
    protected void onRestart() {
        super.onRestart();
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
        String backupSummary = "";
        String backupPackageNames = "";
        int count = 0;
        for (int i = 0; i < appList.size(); i++){
            if (appList.elementAt(i).APP) ++count;
            if (appList.elementAt(i).DATA) ++count;
        }
        int n = 0;
        for (int i = 0; i < appList.size(); i++) {
            if (appList.elementAt(i).APP || appList.elementAt(i).DATA) {
                backupPackageNames = backupPackageNames + appList.elementAt(i).PACKAGE_INFO.applicationInfo.packageName + "\n";
                if (appList.elementAt(i).APP) {
                    String path = appList.elementAt(i).PACKAGE_INFO.applicationInfo.sourceDir;
                    backupSummary = backupSummary + "APP: " + pm.getApplicationLabel(appList.get(i).PACKAGE_INFO.applicationInfo) + " (" + ++n + "/" + count + ") " + (new File(path)).getParent() + '\n';
                }
                if (appList.elementAt(i).DATA) {
                    String path = appList.elementAt(i).PACKAGE_INFO.applicationInfo.dataDir;
                    backupSummary = backupSummary + "DATA: " + pm.getApplicationLabel(appList.get(i).PACKAGE_INFO.applicationInfo) + " (" + ++n + "/" + count + ") " + path + '\n';
                }
            }
        }

        Intent bService = new Intent(BackupActivity.this, BackupService.class)
                .setAction("start service")
                .putExtra("backupSummary", backupSummary)
                .putExtra("backupName", backupName)
                .putExtra("backupPackageNames", backupPackageNames)
                .putExtra("compressionLevel", main.getInt("compressionLevel", 0))
                .putExtra("destination", destination);

        /*if (!wasProgressDialogShown)
            createOnProgressDialog();*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(bService);
        else startService(bService);

        startActivity(new Intent(this, BackupProgressLayout.class));
        finish();
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
        if (isEnabled)
            startBackupButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        else startBackupButton.setBackgroundColor(getResources().getColor(R.color.lightGray));
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
}
