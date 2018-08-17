package balti.migrate;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

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
    TextView nextButton;
    ImageButton selectAll, clearAll, backButton;

    PackageManager pm;
    Vector<BackupDataPacket> appList;
    AppListAdapter adapter;

    BroadcastReceiver progressReceiver;

    static String APP_LIST_PARCEL_KEY = "app_list";

    class AppUpdate extends AsyncTask{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setNextButtonEnabled(false);
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

            dataAllSelect.setChecked(true);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backup_layout);

        pm = getPackageManager();

        listView = findViewById(R.id.appBackupList);

        appAllSelect = findViewById(R.id.appAllSelect);
        dataAllSelect = findViewById(R.id.dataAllSelect);

        nextButton = findViewById(R.id.backupActivityNext);
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

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //askForBackupName();
                startExtraBackupsStartingActivity();
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
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter("Migrate progress broadcast"));

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("get data"));

    }

    void startExtraBackupsStartingActivity(){
        Intent intent = new Intent(BackupActivity.this, ExtraBackups.class);
        startActivity(intent);
        ExtraBackups.setAppList(appList);
        finish();
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

        setNextButtonEnabled(enable);

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

        setNextButtonEnabled(dataAllSelect.isChecked() || appAllSelect.isChecked());
    }

    void setNextButtonEnabled(boolean isEnabled){
        //nextButton.setEnabled(isEnabled);
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
            LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
        }
        catch (Exception ignored){}
    }
}
