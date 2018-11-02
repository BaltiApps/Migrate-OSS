package balti.migrate;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
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
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class ExtraBackups extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {


    private String destination;
    private static boolean isAllAppSelected = false;
    private static boolean isAnyAppSelected = false;
    private static List<BackupDataPacket> appList;

    private Button startBackup;
    private ImageButton back, helpButton;

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

    private LinearLayout callsMainItem;
    private ProgressBar callsReadProgressBar;
    private TextView callsSelectedStatus;
    private CheckBox doBackupCalls;

    private Vector<CallsDataPacket> callsList;

    private ReadCalls callsReader;
    private AlertDialog CallsSelectorDialog;

    private LinearLayout dpiMainItem;
    private ProgressBar dpiReadProgressBar;
    private TextView dpiSelectedStatus;
    private CheckBox doBackupDpi;

    String dpiText = "";

    private ReadDpi dpiReader;

    private LinearLayout keyboardMainItem;
    private TextView keyboardSelectedStatus;
    private CheckBox doBackupKeyboard;

    String keyboardText = "";
    boolean doBackupKeyboardBoolean = false;

    private LoadKeyboardForSelection keyboardSelector;
    private AlertDialog KeyboardSelectorDialog;

    private LayoutInflater layoutInflater;

    private SharedPreferences main;
    private BroadcastReceiver progressReceiver, serviceStartedReceiver;
    private AlertDialog ad;
    private PackageManager pm;

    private long MAX_TWRP_ZIP_SIZE = 4194300;

    MakeBackupSummary makeBackupSummary;
    static int totalSelectedApps = 0;

    Vector<BackupBatch> backupBatches;
    Vector<File> backupSummaries;
    String backupName = "generic_backup_name";
    String busyboxBinaryFile = "";

    int CONTACT_PERMISSION = 933;
    int SMS_PERMISSION = 944;
    int CALLS_PERMISSION = 676;
    int SMS_AND_CALLS_PERMISSION = 511;

    class MakeBackupSummary extends AsyncTask<Void, String, Object[]> {

        String backupName;
        View dialogView;
        TextView waitingHead, waitingProgress, waitingDetails;
        Button cancel;

        long totalSize = 0, availableKb, totalMemory = 0;

        int parts = 1;

        String duBinaryFilePath = "";

        MakeBackupSummary(String backupName) {
            this.backupName = backupName;
            dialogView = layoutInflater.inflate(R.layout.please_wait, null);
            waitingHead = dialogView.findViewById(R.id.waiting_head);
            waitingProgress = dialogView.findViewById(R.id.waiting_progress);
            waitingDetails = dialogView.findViewById(R.id.waiting_details);
            cancel = dialogView.findViewById(R.id.waiting_cancel);

            backupBatches = new Vector<>(0);

            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cancel(true);
                    try {
                        ad.dismiss();
                    }
                    catch (Exception ignored){}
                }
            });

            waitingProgress.setVisibility(View.GONE);
            waitingDetails.setVisibility(View.GONE);

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            ad = new AlertDialog.Builder(ExtraBackups.this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();

            ad.show();

            NotificationManager manager = (NotificationManager)(getSystemService(NOTIFICATION_SERVICE));
            assert manager != null;
            manager.cancelAll();

        }

        @Override
        protected Object[] doInBackground(Void... voids) {

            final String cpu_abi = Build.SUPPORTED_ABIS[0];

            if (cpu_abi.equals("armeabi-v7a") || cpu_abi.equals("arm64-v8a")) {
                busyboxBinaryFile = new CommonTools(ExtraBackups.this).unpackAssetToInternal("busybox", "busybox");
            }
            else if (cpu_abi.equals("x86") || cpu_abi.equals("x86_64")){
                busyboxBinaryFile = new CommonTools(ExtraBackups.this).unpackAssetToInternal("busybox-x86", "busybox");
            }
            duBinaryFilePath = busyboxBinaryFile + " du";

            int length = appList.size();
            publishProgress(getString(R.string.filtering_apps), "", "");

            for (int i = 0, c = 0; i < length; i++){
                BackupDataPacket packet = appList.get(c);
                if (packet.APP || packet.DATA){
                    c++;
                }
                else {
                    appList.remove(c);
                }
            }


            Vector<BackupDataPacketWithSize> appsWithSize = new Vector<>(0);
            long totalBackupSize = 0;

            try {

                publishProgress(getString(R.string.calculating_size), "", "");

                Process memoryFinder = Runtime.getRuntime().exec("su");
                BufferedReader processReader = new BufferedReader( new InputStreamReader(memoryFinder.getInputStream()));
                BufferedWriter processWriter = new BufferedWriter(new OutputStreamWriter(memoryFinder.getOutputStream()));

                totalSelectedApps = appList.size();
                for (int i = 0; i < totalSelectedApps; i++){

                    BackupDataPacket packet = appList.get(i);

                    long dataSize = 0, systemSize = 0;

                    String apkPath = packet.PACKAGE_INFO.applicationInfo.sourceDir;
                    String dataPath = "NULL";
                    if (packet.DATA)
                        dataPath = packet.PACKAGE_INFO.applicationInfo.dataDir;

                    processWriter.write(duBinaryFilePath + " -s " + apkPath + "\n");
                    processWriter.flush();
                    String memoryReaderRes;
                    try {
                        long s;
                        memoryReaderRes = processReader.readLine();
                        s = Long.parseLong(memoryReaderRes.substring(0, memoryReaderRes.indexOf("/")).trim());
                        if (apkPath.startsWith("/system"))
                            systemSize += s;
                        else dataSize += s;
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }

                    if (!dataPath.equals("NULL")){
                        processWriter.write( duBinaryFilePath + " -s " + dataPath + "\n");
                        processWriter.flush();
                        try {
                            memoryReaderRes = processReader.readLine();
                            dataSize += Long.parseLong(memoryReaderRes.substring(0, memoryReaderRes.indexOf("/")).trim());
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                    totalBackupSize = totalBackupSize + dataSize + systemSize;

                    publishProgress(getString(R.string.calculating_size),
                            (i + 1) + " of " + totalSelectedApps,
                            getString(R.string.files_size) + " " + getHumanReadableStorageSpace(totalBackupSize) + "\n");

                    appsWithSize.add(new BackupDataPacketWithSize(packet, dataSize, systemSize));

                }

                processWriter.write("exit\n");
                processWriter.flush();

            }
            catch (Exception e){
                e.printStackTrace();
                return new Object[]{false, getString(R.string.error_calculating_size), e.getMessage()};
            }

            publishProgress(getString(R.string.making_batches), "", "");

            File d = new File(destination);
            if (!d.exists() && !d.mkdirs() && !d.canWrite()){
                return new Object[]{false, getString(R.string.could_not_create_destination),
                        destination + "\n\n" + getString(R.string.make_sure_destination_exists)};
            }

            StatFs statFs = new StatFs(destination);
            availableKb = statFs.getBlockSizeLong() * statFs.getAvailableBlocksLong();
            availableKb = availableKb / 1024;

            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/meminfo"));

                String line;
                while ((line = bufferedReader.readLine()) != null){
                    String parts[] = line.trim().split("\\s+");
                    if (parts[0].equals("MemTotal:"))
                    {
                        totalMemory = Long.parseLong(parts[1]);
                        break;
                    }
                }

            }
            catch (Exception e){
                e.printStackTrace();
                return new Object[]{false, getString(R.string.error_detecting_memory), e.getMessage()};
            }

            if (totalMemory > MAX_TWRP_ZIP_SIZE)
                totalMemory = MAX_TWRP_ZIP_SIZE;

            totalSize = totalBackupSize;

            if (totalBackupSize > availableKb){
                return new Object[]{true};
            }

            parts = (int) Math.ceil((totalBackupSize*1.0)/totalMemory);

            for (int i = 1; i <= parts; i++) {

                Vector<BackupDataPacketWithSize> batchPackets = new Vector<>(0);
                long batchSize = 0, dataSize = 0, systemSize = 0;

                for (int j = 0; j < appsWithSize.size() && batchSize < totalMemory; j++){

                    BackupDataPacketWithSize packetWithSize = appsWithSize.get(j);

                    if (batchSize + packetWithSize.totalSize <= totalMemory){
                        batchPackets.add(packetWithSize);
                        batchSize += packetWithSize.totalSize;

                        dataSize += packetWithSize.dataSize;
                        systemSize += packetWithSize.systemSize;

                        appsWithSize.remove(j);
                        j--;
                    }

                }

                if (batchSize == 0 && appsWithSize.size() != 0){
                    // signifies that all apps were considered but none could be added to a batch due to memory constraints

                    return new Object[]{false, getString(R.string.cannot_split), concatNames(appsWithSize)};
                }
                else {
                    backupBatches.add(new BackupBatch(batchPackets, dataSize, systemSize));
                }
            }

            backupSummaries = new Vector<>(0);

            int c = 0;
            for (int j = 0; j < backupBatches.size(); j++){

                File backupSummary = new File(getFilesDir(), "backup_summary_part"+j);
                try {

                    BufferedWriter writer = new BufferedWriter(new FileWriter(backupSummary.getAbsolutePath()));

                    Vector<BackupDataPacketWithSize> backupDataPacketWithSizes = backupBatches.get(j).appListWithSize;

                    int n = backupDataPacketWithSizes.size();

                    for (int i = 0; i < n; i++) {

                        BackupDataPacket packet = backupDataPacketWithSizes.get(i).packet;

                        if (packet.APP) {

                            String appName = pm.getApplicationLabel(packet.PACKAGE_INFO.applicationInfo).toString().replace(' ', '_');
                            String packageName = packet.PACKAGE_INFO.packageName;
                            String apkPath = packet.PACKAGE_INFO.applicationInfo.sourceDir;
                            String dataPath = "NULL";
                            if (packet.DATA)
                                dataPath = packet.PACKAGE_INFO.applicationInfo.dataDir;
                            String versionName = packet.PACKAGE_INFO.versionName.replace(' ', '_');

                            publishProgress(getString(R.string.reading_data),
                                    ++c + " " + getString(R.string.of) + " " + totalSelectedApps, "");

                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            Bitmap icon = getBitmapFromDrawable(pm.getApplicationIcon(packet.PACKAGE_INFO.applicationInfo));
                            icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            String appIcon = byteToString(stream.toByteArray());

                            String line = appName + " " + packageName + " " + apkPath + " " + dataPath + " " + appIcon + " " + versionName +
                                    " " + packet.PERMISSIONS;

                            writer.write(line + "\n");

                        }

                    }
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    return new Object[]{false, getString(R.string.error_reading_backup_data), e.getMessage()};
                }

                backupSummaries.add(backupSummary);

            }

            return new Object[]{true};
        }

        String concatNames(Vector<BackupDataPacketWithSize> backupDataPacketWithSizes){

            String res = "";

            for (BackupDataPacketWithSize backupDataPacketWithSize : backupDataPacketWithSizes){
                res = res + pm.getApplicationLabel(backupDataPacketWithSize.packet.PACKAGE_INFO.applicationInfo) + "\n";
            }

            return res.trim();

        }

        @Override
        protected void onProgressUpdate(String... strings) {
            super.onProgressUpdate(strings);

                waitingProgress.setVisibility(View.VISIBLE);
                waitingDetails.setVisibility(View.VISIBLE);
                waitingHead.setText(strings[0].trim());
                waitingProgress.setText(strings[1].trim());
                waitingDetails.setText(strings[2].trim());

        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Object[] o) {
            super.onPostExecute(o);

            cancel.setVisibility(View.GONE);

            if ((boolean) o[0] && availableKb < totalSize) {

                try {
                    ad.dismiss();
                }
                catch (Exception ignored){}

                new AlertDialog.Builder(ExtraBackups.this)
                        .setTitle(R.string.insufficient_storage)
                        .setMessage(getString(R.string.files_size) + " " + getHumanReadableStorageSpace(totalSize) + "\n" +
                                getString(R.string.available_space) + " " + getHumanReadableStorageSpace(availableKb) + "\n\n" +
                                getString(R.string.required_storage) + " " + getHumanReadableStorageSpace(totalSize - availableKb) + "\n\n" +
                                getString(R.string.will_be_compressed))
                        .setNegativeButton(R.string.close, null)
                        .setIcon(R.drawable.ic_combine)
                        .show();


            }
            else if (!(boolean) o[0]){
                try {
                    ad.dismiss();
                }
                catch (Exception ignored){}

                new AlertDialog.Builder(ExtraBackups.this)
                        .setTitle((String) o[1])
                        .setMessage((String) o[2])
                        .setPositiveButton(R.string.close, null)
                        .show();

            }
            else {

                waitingHead.setText(R.string.just_a_minute);
                waitingProgress.setText(R.string.starting_engine);
                waitingDetails.setText("");

                Intent bService = new Intent(ExtraBackups.this, BackupService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(bService);
                } else {
                    startService(bService);
                }
            }

        }

        String getHumanReadableStorageSpace(long space){
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
    }

    class ReadContacts extends AsyncTask<Void, Object, Vector<ContactsDataPacket>> {

        int contactsCount = 0;
        Cursor cursor;
        VcfTools vcfTools;

        ReadContacts() {
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
            } else {
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
            Vector<ContactsDataPacket> tempContactsStorage = null;

            try {
                if (cursor != null) {

                    tempContactsStorage = new Vector<>(0);

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
            } catch (Exception e) {
                e.printStackTrace();
            }

            return tempContactsStorage;
        }

        boolean isDuplicate(ContactsDataPacket cdp, Vector<ContactsDataPacket> dataPackets) {
            for (ContactsDataPacket dataPacket : dataPackets) {
                if (cdp.fullName.equals(dataPacket.fullName) && cdp.vcfData.equals(dataPacket.vcfData))
                    return true;
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            contactsReadProgressBar.setProgress((int) values[0]);
            contactsSelectedStatus.setText((String) values[1]);
        }

        @Override
        protected void onPostExecute(Vector<ContactsDataPacket> receivedDataPackets) {
            super.onPostExecute(receivedDataPackets);

            updateContactsList(receivedDataPackets);

            if (cursor != null && contactsCount > 0) {
                contactsMainItem.setClickable(true);
                contactsMainItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {


                        new LoadContactsForSelection().execute();

                    }
                });
            }

            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception ignored) {
                }
            }
        }

    }

    class LoadContactsForSelection extends AsyncTask {

        ProgressBar progressBar;
        ListView listView;
        Button ok, cancel;
        Vector<ContactsDataPacket> dataPackets;
        View itemSelectorView;
        RelativeLayout topBar, bottomBar;
        ImageView selectAll, clearAll;
        TextView title;

        ContactListAdapter adapter;

        public LoadContactsForSelection() {

            itemSelectorView = layoutInflater.inflate(R.layout.extra_item_selector, null);
            itemSelectorView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            dataPackets = new Vector<>(0);

            this.topBar = itemSelectorView.findViewById(R.id.extra_item_selector_top_bar);
            this.selectAll = itemSelectorView.findViewById(R.id.extra_item_selector_select_all);
            this.clearAll = itemSelectorView.findViewById(R.id.extra_item_selector_clear_all);
            this.progressBar = itemSelectorView.findViewById(R.id.extra_item_selector_round_progress);
            this.listView = itemSelectorView.findViewById(R.id.extra_item_selector_item_holder);
            this.bottomBar = itemSelectorView.findViewById(R.id.extra_item_selector_button_bar);
            this.ok = itemSelectorView.findViewById(R.id.extra_item_selector_ok);
            this.cancel = itemSelectorView.findViewById(R.id.extra_item_selector_cancel);
            this.title = itemSelectorView.findViewById(R.id.extra_item_selector_title);

            title.setText(R.string.contacts_selector_label);

            ok.setOnClickListener(null);
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ContactsSelectorDialog.dismiss();
                }
            });

            ContactsSelectorDialog = new AlertDialog.Builder(ExtraBackups.this)
                    .setView(itemSelectorView)
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
            for (int i = 0; i < contactsList.size(); i++) {
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

    class ReadSms extends AsyncTask<Void, Object, Vector<SmsDataPacket>> {

        int smsCount = 0;
        Cursor inboxCursor, outboxCursor, sentCursor, draftCursor;
        SmsTools smsTools;

        ReadSms() {
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

                doBackupSms.setEnabled(true);

                if (inboxCursor != null)
                    smsCount += inboxCursor.getCount();

                if (outboxCursor != null)
                    smsCount += outboxCursor.getCount();

                if (sentCursor != null)
                    smsCount += sentCursor.getCount();

                if (draftCursor != null)
                    smsCount += draftCursor.getCount();

                smsReadProgressBar.setMax(smsCount);

            } else {
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
            Vector<SmsDataPacket> tempSmsStorage = null;

            int c = 0;

            if (inboxCursor != null || outboxCursor != null || sentCursor != null || draftCursor != null) {
                tempSmsStorage = new Vector<>(0);
            }

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
            } catch (Exception e) {
                e.printStackTrace();
            }

            return tempSmsStorage;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            smsReadProgressBar.setProgress((int) values[0]);
            smsSelectedStatus.setText((String) values[1]);
        }

        @Override
        protected void onPostExecute(Vector<SmsDataPacket> receivedDataPackets) {
            super.onPostExecute(receivedDataPackets);

            updateSmsList(receivedDataPackets);

            if ((inboxCursor != null || outboxCursor != null || sentCursor != null || draftCursor != null) && smsCount > 0) {
                smsMainItem.setClickable(true);
                smsMainItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {


                        new LoadSmsForSelection().execute();

                    }
                });
            }

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
        }

    }

    class LoadSmsForSelection extends AsyncTask {

        ProgressBar progressBar;
        ListView listView;
        Button ok, cancel;
        Vector<SmsDataPacket> dataPackets;
        View itemSelectorView;
        RelativeLayout topBar, bottomBar;
        ImageView selectAll, clearAll;
        TextView title;

        SmsListAdapter adapter;

        public LoadSmsForSelection() {

            itemSelectorView = layoutInflater.inflate(R.layout.extra_item_selector, null);
            itemSelectorView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            dataPackets = new Vector<>(0);

            this.topBar = itemSelectorView.findViewById(R.id.extra_item_selector_top_bar);
            this.selectAll = itemSelectorView.findViewById(R.id.extra_item_selector_select_all);
            this.clearAll = itemSelectorView.findViewById(R.id.extra_item_selector_clear_all);
            this.progressBar = itemSelectorView.findViewById(R.id.extra_item_selector_round_progress);
            this.listView = itemSelectorView.findViewById(R.id.extra_item_selector_item_holder);
            this.bottomBar = itemSelectorView.findViewById(R.id.extra_item_selector_button_bar);
            this.ok = itemSelectorView.findViewById(R.id.extra_item_selector_ok);
            this.cancel = itemSelectorView.findViewById(R.id.extra_item_selector_cancel);
            this.title = itemSelectorView.findViewById(R.id.extra_item_selector_title);

            title.setText(R.string.sms_selector_label);

            ok.setOnClickListener(null);
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SmsSelectorDialog.dismiss();
                }
            });

            SmsSelectorDialog = new AlertDialog.Builder(ExtraBackups.this)
                    .setView(itemSelectorView)
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
            for (int i = 0; i < smsList.size(); i++) {
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

    class ReadCalls extends AsyncTask<Void, Object, Vector<CallsDataPacket>> {

        int callsCount = 0;
        Cursor callsCursor;
        CallsTools callsTools;

        ReadCalls() {
            callsTools = new CallsTools(ExtraBackups.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            callsSelectedStatus.setVisibility(View.VISIBLE);
            callsReadProgressBar.setVisibility(View.VISIBLE);
            callsCursor = callsTools.getCallsCursor();
            if (callsCursor != null) {
                doBackupCalls.setEnabled(true);
                callsCount = callsCursor.getCount();
                callsReadProgressBar.setMax(callsCount);
            } else {
                doBackupCalls.setChecked(false);
                doBackupCalls.setEnabled(false);
                callsSelectedStatus.setText(R.string.reading_error);
                callsReadProgressBar.setVisibility(View.GONE);
            }
            callsMainItem.setClickable(false);
            callsList = null;
        }

        @Override
        protected Vector<CallsDataPacket> doInBackground(Void... voids) {
            Vector<CallsDataPacket> tempCallsStorage = null;

            int c = 0;

            if (callsCursor != null)
                tempCallsStorage = new Vector<>(0);

            try {
                if (callsCursor != null && callsCursor.getCount() > 0) {

                    callsCursor.moveToFirst();
                    do {
                        tempCallsStorage.add(callsTools.getCallsPacket(callsCursor, doBackupCalls.isChecked()));
                        publishProgress(c, getString(R.string.reading_calls) + "\n" + c++);
                    }
                    while (callsCursor.moveToNext());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return tempCallsStorage;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            callsReadProgressBar.setProgress((int) values[0]);
            callsSelectedStatus.setText((String) values[1]);
        }

        @Override
        protected void onPostExecute(Vector<CallsDataPacket> receivedDataPackets) {
            super.onPostExecute(receivedDataPackets);

            updateCallsList(receivedDataPackets);

            if (callsCursor != null && callsCount > 0) {
                callsMainItem.setClickable(true);
                callsMainItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {


                        new LoadCallsForSelection().execute();

                    }
                });
            }

        }

    }

    class LoadCallsForSelection extends AsyncTask {

        ProgressBar progressBar;
        ListView listView;
        Button ok, cancel;
        Vector<CallsDataPacket> dataPackets;
        View itemSelectorView;
        RelativeLayout topBar, bottomBar;
        ImageView selectAll, clearAll;
        TextView title;

        CallsListAdapter adapter;

        public LoadCallsForSelection() {

            itemSelectorView = layoutInflater.inflate(R.layout.extra_item_selector, null);
            itemSelectorView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            dataPackets = new Vector<>(0);

            this.topBar = itemSelectorView.findViewById(R.id.extra_item_selector_top_bar);
            this.selectAll = itemSelectorView.findViewById(R.id.extra_item_selector_select_all);
            this.clearAll = itemSelectorView.findViewById(R.id.extra_item_selector_clear_all);
            this.progressBar = itemSelectorView.findViewById(R.id.extra_item_selector_round_progress);
            this.listView = itemSelectorView.findViewById(R.id.extra_item_selector_item_holder);
            this.bottomBar = itemSelectorView.findViewById(R.id.extra_item_selector_button_bar);
            this.ok = itemSelectorView.findViewById(R.id.extra_item_selector_ok);
            this.cancel = itemSelectorView.findViewById(R.id.extra_item_selector_cancel);
            this.title = itemSelectorView.findViewById(R.id.extra_item_selector_title);

            title.setText(R.string.calls_selector_label);

            ok.setOnClickListener(null);
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CallsSelectorDialog.dismiss();
                }
            });

            CallsSelectorDialog = new AlertDialog.Builder(ExtraBackups.this)
                    .setView(itemSelectorView)
                    .setCancelable(false)
                    .create();

        }

        @Override
        protected void onPreExecute() {
            CallsSelectorDialog.show();
            super.onPreExecute();
            topBar.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            listView.setVisibility(View.INVISIBLE);
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            for (int i = 0; i < callsList.size(); i++) {
                CallsDataPacket dataPacket = new CallsDataPacket(callsList.get(i));
                dataPackets.add(dataPacket);
            }
            adapter = new CallsListAdapter(ExtraBackups.this, dataPackets);
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
                    updateCallsList(dataPackets);
                    CallsSelectorDialog.dismiss();
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

    class ReadDpi extends AsyncTask{

        Process dpiReader;
        BufferedReader outputReader;
        BufferedReader errorReader;
        String err;

        String localDpiText;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dpiText = err = localDpiText = "";
            dpiMainItem.setClickable(false);
            doBackupDpi.setEnabled(false);
            dpiSelectedStatus.setVisibility(View.GONE);
            dpiReadProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            try {
                dpiReader = Runtime.getRuntime().exec("su");
                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(dpiReader.getOutputStream()));
                w.write("wm density\n");
                w.write("exit\n");
                w.flush();

                outputReader = new BufferedReader(new InputStreamReader(dpiReader.getInputStream()));
                errorReader = new BufferedReader(new InputStreamReader(dpiReader.getErrorStream()));

                String line;
                while ((line = outputReader.readLine()) != null){
                    localDpiText = localDpiText + line + "\n";
                }

                while ((line = errorReader.readLine()) != null){
                    err = err + line + "\n";
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            doBackupDpi.setEnabled(true);
            dpiReadProgressBar.setVisibility(View.GONE);

            if (!err.equals(""))
            {
                doBackupDpi.setChecked(false);
                new AlertDialog.Builder(ExtraBackups.this)
                        .setTitle(R.string.error_reading_dpi)
                        .setMessage(err)
                        .setNegativeButton(R.string.close, null)
                        .show();


            }
            else {
                dpiText = localDpiText;
                dpiSelectedStatus.setVisibility(View.VISIBLE);
                dpiMainItem.setClickable(true);
                dpiMainItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(ExtraBackups.this)
                                .setTitle(R.string.dpi_label)
                                .setMessage(dpiText)
                                .setNegativeButton(R.string.close, null)
                                .show();
                    }
                });
            }
        }
    }

    class LoadKeyboardForSelection extends AsyncTask{

        Process keyboardReader;
        BufferedReader outputReader;
        BufferedReader errorReader;
        String err;

        ArrayList<String> enabledKeyboards;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            keyboardText = err = "";
            keyboardMainItem.setClickable(false);
            doBackupKeyboard.setEnabled(false);
            keyboardSelectedStatus.setVisibility(View.GONE);
            enabledKeyboards = new ArrayList<>(0);
            doBackupKeyboardBoolean = false;
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            try {
                keyboardReader = Runtime.getRuntime().exec("ime list -s");
                outputReader = new BufferedReader(new InputStreamReader(keyboardReader.getInputStream()));
                errorReader = new BufferedReader(new InputStreamReader(keyboardReader.getErrorStream()));

                String line;
                while ((line = outputReader.readLine()) != null){
                    enabledKeyboards.add(line);
                }

                while ((line = errorReader.readLine()) != null){
                    err = err + line + "\n";
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            doBackupKeyboard.setEnabled(true);
            doBackupKeyboardBoolean = true;

            if (!err.trim().equals("")){

                onKeyboardError(getString(R.string.error_reading_keyboard_list), err);

            }
            else if (enabledKeyboards.size() == 0){

                onKeyboardError(getString(R.string.no_keyboard_enabled), getString(R.string.no_keyboard_enabled_desc));

            }
            else if (enabledKeyboards.size() == 1){

                String packageName = enabledKeyboards.get(0);
                if (packageName.contains("/")){
                    packageName = packageName.split("/")[0];
                }

                try {
                    String kName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();

                    if (!isKeyboardInAppList(packageName)){

                        onKeyboardError(kName + " " + getString(R.string.selected_keyboard_not_present_in_backup),
                                getString(R.string.selected_keyboard_not_present_in_backup_desc));

                    }
                    else {
                        keyboardSelectedStatus.setVisibility(View.VISIBLE);
                        keyboardSelectedStatus.setText(kName);
                        keyboardText = enabledKeyboards.get(0);
                    }

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    onKeyboardError(getString(R.string.error_reading_keyboard_list), e.getMessage());
                }



            }
            else {

                View kView = View.inflate(ExtraBackups.this, R.layout.keyboard_selector, null);
                LinearLayout holder = kView.findViewById(R.id.keyboard_options_holder);

                for (String packageName : enabledKeyboards){

                    final String kText = packageName;

                    if (packageName.contains("/")){
                        packageName = packageName.split("/")[0];
                    }

                    View kItem = View.inflate(ExtraBackups.this, R.layout.keyboard_item, null);
                    ImageView kIcon = kItem.findViewById(R.id.keyboard_icon);
                    final TextView kName = kItem.findViewById(R.id.keyboard_name);
                    TextView kPresent = kItem.findViewById(R.id.keyboard_present_in_backup_label);

                    try {

                        kIcon.setImageDrawable(pm.getApplicationIcon(packageName));
                        kName.setText(pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)));

                        if (isKeyboardInAppList(packageName))
                            kPresent.setVisibility(View.VISIBLE);
                        else kPresent.setVisibility(View.GONE);

                        final String finalPackageName = packageName;
                        kItem.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                if (!isKeyboardInAppList(finalPackageName)){

                                    new AlertDialog.Builder(ExtraBackups.this)
                                            .setTitle(kName.getText() + " " + getString(R.string.selected_keyboard_not_present_in_backup))
                                            .setMessage(R.string.selected_keyboard_not_present_in_backup_desc)
                                            .setNegativeButton(R.string.close, null)
                                            .show();

                                }
                                else {
                                    keyboardSelectedStatus.setVisibility(View.VISIBLE);
                                    keyboardSelectedStatus.setText(kName.getText());
                                    keyboardText = kText;
                                    KeyboardSelectorDialog.dismiss();
                                }

                            }
                        });

                        holder.addView(kItem);

                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        onKeyboardError(getString(R.string.error_reading_keyboard_list), e.getMessage());
                    }
                }

                KeyboardSelectorDialog = new AlertDialog.Builder(ExtraBackups.this, R.style.DarkAlert)
                        .setView(kView)
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doBackupKeyboard.setChecked(false);
                            }
                        })
                        .setCancelable(false)
                        .create();

                KeyboardSelectorDialog.show();

                keyboardMainItem.setClickable(true);
                keyboardMainItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (KeyboardSelectorDialog != null)
                                KeyboardSelectorDialog.show();
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            onKeyboardError(getString(R.string.error_reading_keyboard_list), e.getMessage());
                        }
                    }
                });
            }
        }

        boolean isKeyboardInAppList(String packageName){
            for (BackupDataPacket packet : appList){
                if (packet.PACKAGE_INFO.packageName.equals(packageName))
                    return packet.APP;
            }
            return false;
        }

        void onKeyboardError(String title, String errorMessage){
            doBackupKeyboard.setChecked(false);
            keyboardSelectedStatus.setText("");
            keyboardSelectedStatus.setVisibility(View.GONE);

            new AlertDialog.Builder(ExtraBackups.this)
                    .setTitle(title)
                    .setMessage(errorMessage)
                    .setNegativeButton(R.string.close, null)
                    .show();
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.extra_backups);

        pm = getPackageManager();

        main = getSharedPreferences("main", MODE_PRIVATE);

        destination = main.getString("defaultBackupPath", "/sdcard/Migrate");

        layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        contactsMainItem = findViewById(R.id.extra_item_contacts);
        contactsSelectedStatus = findViewById(R.id.contacts_selected_status);
        contactsReadProgressBar = findViewById(R.id.contacts_read_progress);
        doBackupContacts = findViewById(R.id.do_backup_contacts);

        smsMainItem = findViewById(R.id.extra_item_sms);
        smsSelectedStatus = findViewById(R.id.sms_selected_status);
        smsReadProgressBar = findViewById(R.id.sms_read_progress);
        doBackupSms = findViewById(R.id.do_backup_sms);

        callsMainItem = findViewById(R.id.extra_item_calls);
        callsSelectedStatus = findViewById(R.id.calls_selected_status);
        callsReadProgressBar = findViewById(R.id.calls_read_progress);
        doBackupCalls = findViewById(R.id.do_backup_calls);

        dpiMainItem = findViewById(R.id.extra_item_dpi);
        dpiReadProgressBar = findViewById(R.id.dpi_read_progress);
        dpiSelectedStatus = findViewById(R.id.dpi_selected_status);
        doBackupDpi = findViewById(R.id.do_backup_dpi);

        keyboardMainItem = findViewById(R.id.extra_item_keyboard);
        keyboardSelectedStatus = findViewById(R.id.keyboard_selected_status);
        doBackupKeyboard = findViewById(R.id.do_backup_keyboard);

        contactsMainItem.setClickable(false);
        smsMainItem.setClickable(false);
        callsMainItem.setClickable(false);
        dpiMainItem.setClickable(false);
        keyboardMainItem.setClickable(false);

        startBackup = findViewById(R.id.startBackupButton);

        startBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isAnyAppSelected || doBackupContacts.isChecked() || doBackupSms.isChecked() ||
                        doBackupCalls.isChecked() || doBackupDpi.isChecked() || doBackupKeyboardBoolean)
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

        helpButton = findViewById(R.id.extra_backups_help);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ExtraBackups.this)
                        .setMessage(R.string.extra_backups_help)
                        .setPositiveButton(R.string.close, null)
                        .show();
            }
        });

        serviceStartedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BackupService.setBackupBatches(backupBatches, backupName, destination, busyboxBinaryFile,
                        contactsList, doBackupContacts.isChecked(),
                        callsList, doBackupCalls.isChecked(),
                        smsList, doBackupSms.isChecked(),
                        dpiText, doBackupDpi.isChecked(),
                        keyboardText, doBackupKeyboardBoolean,
                        backupSummaries);
                LocalBroadcastManager.getInstance(ExtraBackups.this).sendBroadcast(new Intent("start batch backup"));
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceStartedReceiver, new IntentFilter("backup service started"));

        progressReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent progressActivityStartIntent = new Intent(ExtraBackups.this, BackupProgressLayout.class);
                progressActivityStartIntent.putExtras(Objects.requireNonNull(intent.getExtras()));
                progressActivityStartIntent.setAction("Migrate progress broadcast");
                startActivity(progressActivityStartIntent);
                try {
                    LocalBroadcastManager.getInstance(ExtraBackups.this).unregisterReceiver(progressReceiver);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, new IntentFilter("Migrate progress broadcast"));

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("get data"));

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent("extraBackupsStarted"));

        doBackupContacts.setOnCheckedChangeListener(this);
        doBackupSms.setOnCheckedChangeListener(this);
        doBackupCalls.setOnCheckedChangeListener(this);
        doBackupDpi.setOnCheckedChangeListener(this);
        doBackupKeyboard.setOnCheckedChangeListener(this);

        boolean isSmsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean isCallsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
        boolean isSmsAndCallsGranted = isSmsGranted && isCallsGranted;

        if (isSmsAndCallsGranted) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.READ_CALL_LOG}, SMS_AND_CALLS_PERMISSION);
        }
        else if (isSmsGranted) {
            doBackupSms.setChecked(true);
        }
        else if (isCallsGranted) {
            doBackupCalls.setChecked(true);
        }


        /*final AdView adView = findViewById(R.id.extra_backups_activity_adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        adView.setAdListener(new AdListener(){
            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                adView.setVisibility(View.GONE);
            }
        });*/

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (buttonView == doBackupContacts){

            if (isChecked) {

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

                                ActivityCompat.requestPermissions(ExtraBackups.this,
                                        new String[]{Manifest.permission.READ_CONTACTS},
                                        CONTACT_PERMISSION);

                            }
                        })
                        .setCancelable(false)
                        .show();

            } else {
                contactsMainItem.setClickable(false);
                contactsReadProgressBar.setVisibility(View.GONE);
                contactsSelectedStatus.setVisibility(View.GONE);
                try {
                    contactsReader.cancel(true);
                } catch (Exception ignored) {
                }
            }


        }
        else if (buttonView == doBackupSms){

            if (isChecked) {

                ActivityCompat.requestPermissions(ExtraBackups.this,
                        new String[]{Manifest.permission.READ_SMS},
                        SMS_PERMISSION);

            } else {
                smsMainItem.setClickable(false);
                smsReadProgressBar.setVisibility(View.GONE);
                smsSelectedStatus.setVisibility(View.GONE);
                try {
                    smsReader.cancel(true);
                } catch (Exception ignored) {
                }
            }

        }
        else if (buttonView == doBackupCalls){

            if (isChecked) {

                ActivityCompat.requestPermissions(ExtraBackups.this,
                    new String[]{Manifest.permission.READ_CALL_LOG},
                    CALLS_PERMISSION);


            } else {
                callsMainItem.setClickable(false);
                callsReadProgressBar.setVisibility(View.GONE);
                callsSelectedStatus.setVisibility(View.GONE);
                try {
                    callsReader.cancel(true);
                } catch (Exception ignored) {
                }
            }

        }
        else if (buttonView == doBackupDpi){

            if (isChecked) {

                new AlertDialog.Builder(ExtraBackups.this)
                        .setTitle(R.string.dragons_ahead)
                        .setMessage(R.string.dpi_backup_warning_desc)
                        .setPositiveButton(R.string.go_ahead, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                dpiReader = new ReadDpi();
                                dpiReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doBackupDpi.setChecked(false);
                            }
                        })
                        .setCancelable(false)
                        .show();


            } else {
                dpiMainItem.setClickable(false);
                dpiSelectedStatus.setVisibility(View.GONE);
                dpiReadProgressBar.setVisibility(View.GONE);
                try {
                    dpiReader.cancel(true);
                } catch (Exception ignored) {
                }
            }

        }
        else if (buttonView == doBackupKeyboard){

            if (isChecked) {

                keyboardSelector = new LoadKeyboardForSelection();
                keyboardSelector.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


            } else {
                keyboardText = "";
                doBackupKeyboardBoolean = false;
                keyboardMainItem.setClickable(false);
                keyboardSelectedStatus.setVisibility(View.GONE);
                try {
                    keyboardSelector.cancel(true);
                } catch (Exception ignored) {
                }
            }

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_AND_CALLS_PERMISSION){

            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){

                doBackupSms.setOnCheckedChangeListener(null);
                doBackupCalls.setOnCheckedChangeListener(null);

                doBackupSms.setChecked(true);
                doBackupCalls.setChecked(true);

                try {
                    callsReader = new ReadCalls();
                    callsReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    smsReader = new ReadSms();
                    smsReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                doBackupSms.setOnCheckedChangeListener(this);
                doBackupCalls.setOnCheckedChangeListener(this);

            }

        }
        else if (requestCode == CONTACT_PERMISSION){

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                try {
                    contactsReader = new ReadContacts();
                    contactsReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
            else {

                Toast.makeText(this, R.string.contacts_access_needed, Toast.LENGTH_SHORT).show();
                doBackupContacts.setChecked(false);

            }

        }
        else if (requestCode == SMS_PERMISSION){

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                try {
                    smsReader = new ReadSms();
                    smsReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            else {

                Toast.makeText(this, R.string.sms_access_needed, Toast.LENGTH_SHORT).show();
                doBackupSms.setChecked(false);

            }

        }
        else if (requestCode == CALLS_PERMISSION){

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                try {
                    callsReader = new ReadCalls();
                    callsReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            else {

                Toast.makeText(this, R.string.calls_access_needed, Toast.LENGTH_SHORT).show();
                doBackupCalls.setChecked(false);

            }

        }

    }

    static void setAppList(List<BackupDataPacket> al, boolean isDataAllSelected, boolean anyAppSelected) {
        appList = al;
        isAllAppSelected = isDataAllSelected;
        isAnyAppSelected = anyAppSelected;
    }

    void askForBackupName() {
        final EditText editText = new EditText(this);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss");
        if (isAllAppSelected && doBackupSms.isChecked() && doBackupCalls.isChecked())
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
                Button okButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String backupName = editText.getText().toString().trim().replace(' ', '_');
                        if (!backupName.equals(""))
                            checkOverwrite(backupName, alertDialog);
                        else
                            Toast.makeText(ExtraBackups.this, getString(R.string.empty), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        alertDialog.show();

    }

    void checkOverwrite(final String name, final AlertDialog alertDialog) {
        final File dir = new File(destination + "/" + name);
        final File zip = new File(destination + "/" + name + ".zip");
        if (dir.exists() || zip.exists()) {
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
        } else {
            alertDialog.dismiss();
            startBackup(backupName = name);
        }
    }

    void startBackup(String backupName) {

        makeBackupSummary = new MakeBackupSummary(backupName);
        makeBackupSummary.execute();
    }

    void dirDelete(String path) {
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


    String byteToString(byte[] bytes) {
        StringBuilder res = new StringBuilder();
        for (byte b : bytes) {
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
        } catch (Exception ignored) { }
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver);
        } catch (Exception ignored) { }
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceStartedReceiver);
        } catch (Exception ignored) { }
        try {
            contactsReader.cancel(true);
        } catch (Exception ignored) { }
        try {
            smsReader.cancel(true);
        } catch (Exception ignored) { }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ExtraBackups.this, BackupActivity.class));
    }

    void updateContactsList(Vector<ContactsDataPacket> newContactsDataPackets) {

        if (newContactsDataPackets == null) {
            doBackupContacts.setChecked(false);
            return;
        }

        int l = newContactsDataPackets.size();
        contactsReadProgressBar.setVisibility(View.VISIBLE);
        contactsSelectedStatus.setText(R.string.reading);
        contactsReadProgressBar.setMax(l);
        int n = 0;
        for (int i = 0; i < l; i++) {
            if (newContactsDataPackets.get(i).selected)
                n++;
        }

        if (n == 0) {
            doBackupContacts.setChecked(false);
        } else {
            contactsList = newContactsDataPackets;
            contactsReadProgressBar.setVisibility(View.GONE);
            contactsSelectedStatus.setText(n + " of " + contactsList.size());
        }

    }

    void updateSmsList(Vector<SmsDataPacket> newSmsDataPackets) {

        if (newSmsDataPackets == null) {
            doBackupSms.setChecked(false);
            return;
        }

        int l = newSmsDataPackets.size();
        smsReadProgressBar.setVisibility(View.VISIBLE);
        smsSelectedStatus.setText(R.string.reading);
        smsReadProgressBar.setMax(l);
        int n = 0;
        for (int i = 0; i < l; i++) {
            if (newSmsDataPackets.get(i).selected)
                n++;
        }

        if (n == 0) {
            doBackupSms.setChecked(false);
        } else {
            smsList = newSmsDataPackets;
            smsReadProgressBar.setVisibility(View.GONE);
            smsSelectedStatus.setText(n + " of " + smsList.size());
        }
    }

    void updateCallsList(Vector<CallsDataPacket> newCallsDataPackets) {

        if (newCallsDataPackets == null) {
            doBackupCalls.setChecked(false);
            return;
        }

        int l = newCallsDataPackets.size();
        callsReadProgressBar.setVisibility(View.VISIBLE);
        callsSelectedStatus.setText(R.string.reading);
        callsReadProgressBar.setMax(l);
        int n = 0;
        for (int i = 0; i < l; i++) {
            if (newCallsDataPackets.get(i).selected)
                n++;
        }

        if (n == 0) {
            doBackupCalls.setChecked(false);
        } else {
            callsList = newCallsDataPackets;
            callsReadProgressBar.setVisibility(View.GONE);
            callsSelectedStatus.setText(n + " of " + callsList.size());
        }
    }

}