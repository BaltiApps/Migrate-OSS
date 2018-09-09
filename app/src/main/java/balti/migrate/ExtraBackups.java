package balti.migrate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import java.util.Vector;

import static android.os.Environment.getExternalStorageDirectory;

public class ExtraBackups extends AppCompatActivity {


    private String destination;
    private static boolean isAnyAppSelected = false;
    private static boolean isAllAppSelected = false;
    private static List<BackupDataPacket> appList;

    private TextView startBackup;
    private ImageButton back;

    private LinearLayout contactsMainItem;
    private ProgressBar contactsReadProgressBar;
    private TextView contactsSelectedStatus;
    private CheckBox doBackupContacts;

    private Vector<ContactsDataPacket> contactsList;

    private ReadContacts contactsReader;
    private AlertDialog ContactsSelectorDialog;

    private LinearLayout smsMainItem;
    private ProgressBar smsReadProgressBar;
    private TextView smsSelectedStatus;
    private CheckBox doBackupSms;

    private Vector<SmsDataPacket> smsList;

    private ReadSms smsReader;
    private AlertDialog SmsSelectorDialog;

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

                try {
                    contactsReader.cancel(true);
                }
                catch (Exception ignored){}

                try {
                    smsReader.cancel(true);
                }
                catch (Exception ignored){}

                Intent bService = new Intent(ExtraBackups.this, BackupService.class)
                        .putExtra("backupName", backupName)
                        .putExtra("compressionLevel", main.getInt("compressionLevel", 0))
                        .putExtra("destination", destination);


                BackupService.backupEngine = new BackupEngine(backupName, main.getInt("compressionLevel", 0), destination, ExtraBackups.this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(bService);
                    BackupService.backupEngine.startBackup(doBackupContacts.isChecked(), contactsList, doBackupSms.isChecked(), smsList);
                }
                else{
                    startService(bService);
                    BackupService.backupEngine.startBackup(doBackupContacts.isChecked(), contactsList, doBackupSms.isChecked(), smsList);
                }

            }
            else {
                Toast.makeText(ExtraBackups.this, (String)o[1], Toast.LENGTH_SHORT).show();
            }
        }
    }

    class ReadContacts extends AsyncTask<Void, Object, Vector<ContactsDataPacket>>{

        int contactsCount = 0;
        Cursor cursor;
        VcfTools vcfTools;

        ReadContacts(){
            vcfTools = new VcfTools(ExtraBackups.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            contactsSelectedStatus.setVisibility(View.VISIBLE);
            contactsReadProgressBar.setVisibility(View.VISIBLE);
            cursor = vcfTools.getContactsCursor();
            if (cursor != null) {
                contactsCount = cursor.getCount();
                contactsReadProgressBar.setMax(contactsCount);
                doBackupContacts.setEnabled(true);
            }
            else {
                doBackupContacts.setChecked(false);
                doBackupContacts.setEnabled(false);
                contactsSelectedStatus.setText(R.string.reading_error);
                contactsReadProgressBar.setVisibility(View.GONE);
            }
            contactsMainItem.setClickable(false);
            contactsList = null;
        }

        @Override
        protected Vector<ContactsDataPacket> doInBackground(Void... voids) {
            Vector<ContactsDataPacket> tempContactsStorage = new Vector<>(0);

            try {
                if (cursor != null) {
                    cursor.moveToFirst();
                    for (int i = 0; i < contactsCount; i++) {
                        String temp[] = vcfTools.getVcfData(cursor);
                        ContactsDataPacket obj = new ContactsDataPacket(temp[0], temp[1]);
                        if (!isDuplicate(obj, tempContactsStorage))
                            tempContactsStorage.add(obj);
                        publishProgress(i, getString(R.string.filtering_duplicates) + "\n" + i);
                        cursor.moveToNext();
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }

            return tempContactsStorage;
        }

        boolean isDuplicate(ContactsDataPacket cdp, Vector<ContactsDataPacket> dataPackets){
            for (ContactsDataPacket dataPacket : dataPackets){
                if (cdp.fullName.equals(dataPacket.fullName) && cdp.vcfData.equals(dataPacket.vcfData))
                    return true;
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            contactsReadProgressBar.setProgress((int)values[0]);
            contactsSelectedStatus.setText((String)values[1]);
        }

        @Override
        protected void onPostExecute(Vector<ContactsDataPacket> receivedDataPackets) {
            super.onPostExecute(receivedDataPackets);

            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception ignored) {
                }
                updateContactsList(receivedDataPackets);
            }


            contactsMainItem.setClickable(true);
            contactsMainItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {


                    new LoadContactsForSelection().execute();

                }
            });
        }

    }

    class LoadContactsForSelection extends AsyncTask{

        ProgressBar progressBar;
        ListView listView;
        Button ok, cancel;
        Vector<ContactsDataPacket> dataPackets;
        View contactsSelectorView;
        RelativeLayout topBar, bottomBar;
        ImageView selectAll, clearAll;

        ContactListAdapter adapter;

        public LoadContactsForSelection() {

            contactsSelectorView = layoutInflater.inflate(R.layout.contacts_selector, null);
            contactsSelectorView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            dataPackets = new Vector<>(0);

            this.topBar = contactsSelectorView.findViewById(R.id.contacts_top_bar);
            this.selectAll = contactsSelectorView.findViewById(R.id.contacts_select_all);
            this.clearAll = contactsSelectorView.findViewById(R.id.contacts_clear_all);
            this.progressBar = contactsSelectorView.findViewById(R.id.contacts_selector_round_progress);
            this.listView = contactsSelectorView.findViewById(R.id.show_contacts);
            this.bottomBar = contactsSelectorView.findViewById(R.id.contacts_button_bar);
            this.ok = contactsSelectorView.findViewById(R.id.contact_selector_ok);
            this.cancel = contactsSelectorView.findViewById(R.id.contact_selector_cancel);

            ok.setOnClickListener(null);
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ContactsSelectorDialog.dismiss();
                }
            });

            ContactsSelectorDialog = new AlertDialog.Builder(ExtraBackups.this)
                    .setView(contactsSelectorView)
                    .setCancelable(false)
                    .create();

        }

        @Override
        protected void onPreExecute() {
            ContactsSelectorDialog.show();
            super.onPreExecute();
            topBar.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            listView.setVisibility(View.INVISIBLE);
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            for (int i = 0; i < contactsList.size(); i++){
                ContactsDataPacket dataPacket = new ContactsDataPacket(contactsList.get(i).fullName, contactsList.get(i).vcfData);
                dataPacket.selected = contactsList.get(i).selected;
                dataPackets.add(dataPacket);
            }
            adapter = new ContactListAdapter(ExtraBackups.this, dataPackets);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            listView.setAdapter(adapter);

            topBar.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);

            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    updateContactsList(dataPackets);
                    ContactsSelectorDialog.dismiss();
                }
            });

            selectAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    adapter.checkAll(true);
                    adapter.notifyDataSetChanged();
                }
            });

            clearAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    adapter.checkAll(false);
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    class ReadSms extends AsyncTask<Void, Object, Vector<SmsDataPacket>>{

        int smsCount = 0;
        Cursor inboxCursor, outboxCursor, sentCursor, draftCursor;
        SmsTools smsTools;

        ReadSms(){
            smsTools = new SmsTools(ExtraBackups.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            smsSelectedStatus.setVisibility(View.VISIBLE);
            smsReadProgressBar.setVisibility(View.VISIBLE);
            inboxCursor = smsTools.getSmsInboxCursor();
            outboxCursor = smsTools.getSmsOutboxCursor();
            sentCursor = smsTools.getSmsSentCursor();
            draftCursor = smsTools.getSmsDraftCursor();
            if (inboxCursor != null || outboxCursor != null || sentCursor != null || draftCursor != null) {
                smsReadProgressBar.setMax(smsCount);
                doBackupSms.setEnabled(true);

                if (inboxCursor != null)
                    smsCount += inboxCursor.getCount();

                if (outboxCursor != null)
                    smsCount += outboxCursor.getCount();

                if (sentCursor != null)
                    smsCount += sentCursor.getCount();

                if (draftCursor != null)
                    smsCount += draftCursor.getCount();
            }
            else {
                doBackupSms.setChecked(false);
                doBackupSms.setEnabled(false);
                smsSelectedStatus.setText(R.string.reading_error);
                smsReadProgressBar.setVisibility(View.GONE);
            }
            smsMainItem.setClickable(false);
            smsList = null;
        }

        @Override
        protected Vector<SmsDataPacket> doInBackground(Void... voids) {
            Vector<SmsDataPacket> tempSmsStorage = new Vector<>(0);

            int c = 0;

            try {
                if (inboxCursor != null && inboxCursor.getCount() > 0) {
                    inboxCursor.moveToFirst();
                    do {
                        tempSmsStorage.add(smsTools.getSmsPacket(inboxCursor, doBackupSms.isChecked()));
                        publishProgress(c, getString(R.string.reading_sms) + "\n" + c++);
                    }
                    while (inboxCursor.moveToNext());
                }

                if (outboxCursor != null && outboxCursor.getCount() > 0) {
                    outboxCursor.moveToFirst();
                    do {
                        tempSmsStorage.add(smsTools.getSmsPacket(outboxCursor, doBackupSms.isChecked()));
                        publishProgress(c, getString(R.string.reading_sms) + "\n" + c++);
                    }
                    while (outboxCursor.moveToNext());
                }

                if (sentCursor != null && sentCursor.getCount() > 0) {
                    sentCursor.moveToFirst();
                    do {
                        tempSmsStorage.add(smsTools.getSmsPacket(sentCursor, doBackupSms.isChecked()));
                        publishProgress(c, getString(R.string.reading_sms) + "\n" + c++);
                    }
                    while (sentCursor.moveToNext());
                }

                if (draftCursor != null && draftCursor.getCount() > 0) {
                    draftCursor.moveToFirst();
                    do {
                        tempSmsStorage.add(smsTools.getSmsPacket(draftCursor, doBackupSms.isChecked()));
                        publishProgress(c, getString(R.string.reading_sms) + "\n" + c++);
                    }
                    while (draftCursor.moveToNext());
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }

            return tempSmsStorage;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            contactsReadProgressBar.setProgress((int)values[0]);
            contactsSelectedStatus.setText((String)values[1]);
        }

        @Override
        protected void onPostExecute(Vector<SmsDataPacket> receivedDataPackets) {
            super.onPostExecute(receivedDataPackets);

            if (inboxCursor != null) {
                try {
                    inboxCursor.close();
                } catch (Exception ignored) {
                }
            }

            if (outboxCursor != null) {
                try {
                    outboxCursor.close();
                } catch (Exception ignored) {
                }
            }

            if (sentCursor != null) {
                try {
                    sentCursor.close();
                } catch (Exception ignored) {
                }
            }

            if (draftCursor != null) {
                try {
                    draftCursor.close();
                } catch (Exception ignored) {
                }
            }

            updateSmsList(receivedDataPackets);

            smsMainItem.setClickable(true);
            smsMainItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {


                    new LoadSmsForSelection().execute();

                }
            });
        }

    }

    class LoadSmsForSelection extends AsyncTask{

        ProgressBar progressBar;
        ListView listView;
        Button ok, cancel;
        Vector<SmsDataPacket> dataPackets;
        View smsSelectorView;
        RelativeLayout topBar, bottomBar;
        ImageView selectAll, clearAll;

        SmsListAdapter adapter;

        public LoadSmsForSelection() {

            smsSelectorView = layoutInflater.inflate(R.layout.sms_selector, null);
            smsSelectorView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            dataPackets = new Vector<>(0);

            this.topBar = smsSelectorView.findViewById(R.id.sms_top_bar);
            this.selectAll = smsSelectorView.findViewById(R.id.sms_select_all);
            this.clearAll = smsSelectorView.findViewById(R.id.sms_clear_all);
            this.progressBar = smsSelectorView.findViewById(R.id.sms_selector_round_progress);
            this.listView = smsSelectorView.findViewById(R.id.show_sms);
            this.bottomBar = smsSelectorView.findViewById(R.id.sms_button_bar);
            this.ok = smsSelectorView.findViewById(R.id.sms_selector_ok);
            this.cancel = smsSelectorView.findViewById(R.id.sms_selector_cancel);

            ok.setOnClickListener(null);
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SmsSelectorDialog.dismiss();
                }
            });

            SmsSelectorDialog = new AlertDialog.Builder(ExtraBackups.this)
                    .setView(smsSelectorView)
                    .setCancelable(false)
                    .create();

        }

        @Override
        protected void onPreExecute() {
            SmsSelectorDialog.show();
            super.onPreExecute();
            topBar.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            listView.setVisibility(View.INVISIBLE);
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            for (int i = 0; i < smsList.size(); i++){
                SmsDataPacket dataPacket = new SmsDataPacket(smsList.get(i));
                dataPackets.add(dataPacket);
            }
            adapter = new SmsListAdapter(ExtraBackups.this, dataPackets);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            listView.setAdapter(adapter);

            topBar.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);

            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    updateSmsList(dataPackets);
                    SmsSelectorDialog.dismiss();
                }
            });

            selectAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    adapter.checkAll(true);
                    adapter.notifyDataSetChanged();
                }
            });

            clearAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    adapter.checkAll(false);
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.extra_backups);

        pm = getPackageManager();

        main = getSharedPreferences("main", MODE_PRIVATE);
        destination = main.getString("defaultBackupPath", getExternalStorageDirectory() + "/Migrate");

        layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        contactsMainItem = findViewById(R.id.extra_item_contacts);
        contactsSelectedStatus = findViewById(R.id.contacts_selected_status);
        contactsReadProgressBar = findViewById(R.id.contacts_read_progress);
        doBackupContacts = findViewById(R.id.do_backup_contacts);

        smsMainItem = findViewById(R.id.extra_item_sms);
        smsSelectedStatus = findViewById(R.id.sms_selected_status);
        smsReadProgressBar = findViewById(R.id.sms_read_progress);
        doBackupSms = findViewById(R.id.do_backup_sms);

        startBackup = findViewById(R.id.startBackupButton);

        startBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isAnyAppSelected || doBackupContacts.isChecked() || doBackupSms.isChecked())
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
                try {
                    LocalBroadcastManager.getInstance(ExtraBackups.this).unregisterReceiver(progressReceiver);
                }catch (Exception e){e.printStackTrace();}
                finish();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter("Migrate progress broadcast"));

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("get data"));

        doBackupContacts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if(b) {

                    new AlertDialog.Builder(ExtraBackups.this)
                            .setTitle(R.string.not_recommended)
                            .setMessage(getText(R.string.contacts_not_recommended))
                            .setPositiveButton(R.string.dont_backup, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    doBackupContacts.setChecked(false);
                                }
                            })
                            .setNegativeButton(R.string.backup_contacts_anyway, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        contactsReader = new ReadContacts();
                                        contactsReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .setCancelable(false)
                            .show();

                }
                else {
                    contactsMainItem.setClickable(false);
                    contactsReadProgressBar.setVisibility(View.GONE);
                    contactsSelectedStatus.setVisibility(View.GONE);
                    try {
                        contactsReader.cancel(true);
                    }
                    catch (Exception ignored){}
                }
            }
        });

        doBackupSms.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (b){
                    try {
                        smsReader = new ReadSms();
                        smsReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    smsMainItem.setClickable(false);
                    smsReadProgressBar.setVisibility(View.GONE);
                    smsSelectedStatus.setVisibility(View.GONE);
                    try {
                        smsReader.cancel(true);
                    }
                    catch (Exception ignored){}
                }

            }
        });

        doBackupSms.setChecked(true);

    }

    static void setAppList(List<BackupDataPacket> al, boolean isDataAllSelected){
        appList = al;
        for (BackupDataPacket packet : appList){
            if (isAnyAppSelected = packet.APP)
                break;
        }
        isAllAppSelected = isDataAllSelected;
    }

    void askForBackupName(){
        final EditText editText = new EditText(this);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
        if (isAllAppSelected && doBackupSms.isChecked())
            editText.setText(getString(R.string.fullBackupLabel) + "_" + sdf.format(Calendar.getInstance().getTime()));
        else
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
        try {
            contactsReader.cancel(true);
        }
        catch (Exception ignored){}
        try {
            smsReader.cancel(true);
        }
        catch (Exception ignored){}
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ExtraBackups.this, BackupActivity.class));
    }

    void updateContactsList(Vector<ContactsDataPacket> newContactsDataPackets){

        if (newContactsDataPackets == null) {
            doBackupContacts.setChecked(false);
            return;
        }

        int l = newContactsDataPackets.size();
        contactsReadProgressBar.setVisibility(View.VISIBLE);
        contactsSelectedStatus.setText(R.string.reading);
        contactsReadProgressBar.setMax(l);
        int n = 0;
        for (int i = 0; i < l; i++){
            if (newContactsDataPackets.get(i).selected)
                n++;
        }

        if (n == 0){
            doBackupContacts.setChecked(false);
        }
        else {
            contactsList = newContactsDataPackets;
            contactsReadProgressBar.setVisibility(View.GONE);
            contactsSelectedStatus.setText(n + " of " + contactsList.size());
        }

    }

    void updateSmsList(Vector<SmsDataPacket> newSmsDataPackets){

        if (newSmsDataPackets == null) {
            doBackupSms.setChecked(false);
            return;
        }

        int l = newSmsDataPackets.size();
        smsReadProgressBar.setVisibility(View.VISIBLE);
        smsSelectedStatus.setText(R.string.reading);
        smsReadProgressBar.setMax(l);
        int n = 0;
        for (int i = 0; i < l; i++){
            if (newSmsDataPackets.get(i).selected)
                n++;
        }

        if (n == 0){
            doBackupSms.setChecked(false);
        }
        else {
            smsList = newSmsDataPackets;
            smsReadProgressBar.setVisibility(View.GONE);
            smsSelectedStatus.setText(n + " of " + smsList.size());
        }
    }

}

//9307