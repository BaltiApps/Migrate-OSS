package balti.migrate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class ExtraBackups extends AppCompatActivity {


    private String destination;
    private static boolean isAnyAppSelected = false;
    private static List<BackupDataPacket> appList;

    private TextView startBackup;
    private ImageButton back;

    private LinearLayout contactsMainItem;
    private ProgressBar contactsReadProgressBar;
    private TextView contactsSelectedStatus;

    private LayoutInflater layoutInflater;

    private SharedPreferences main;
    private BroadcastReceiver progressReceiver;
    private AlertDialog ad;
    private PackageManager pm;

    private Vector<ContactsDataPacket> contactsList;

    private ReadContacts contactsReader;
    private AlertDialog ContactsSelectorDialog;

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

    class LoadContactsForSelection extends AsyncTask{

        ProgressBar progressBar;
        ListView listView;
        Button ok, cancel;
        Vector<ContactsDataPacket> dataPackets;
        View contactsSelectorView;

        ContactListAdapter adapter;

        public LoadContactsForSelection() {

            contactsSelectorView = layoutInflater.inflate(R.layout.contacts_selector, null);
            contactsSelectorView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            dataPackets = new Vector<>(0);

            this.progressBar = contactsSelectorView.findViewById(R.id.contacts_selector_round_progress);
            this.listView = contactsSelectorView.findViewById(R.id.show_contacts);
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
            listView.setAdapter(adapter);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    updateContactsList(dataPackets);
                    ContactsSelectorDialog.dismiss();
                }
            });
        }
    }

    class ReadContacts extends AsyncTask<Void, Integer, Vector<ContactsDataPacket>>{

        int contactsCount = 0;
        Cursor cursor;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            contactsSelectedStatus.setText(R.string.reading);
            contactsReadProgressBar.setVisibility(View.VISIBLE);
            cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                contactsCount = cursor.getCount();
                contactsReadProgressBar.setMax(contactsCount);
            }
            contactsMainItem.setClickable(false);
        }

        @Override
        protected Vector<ContactsDataPacket> doInBackground(Void... voids) {
            Vector<ContactsDataPacket> tempContactsStorage = new Vector<>(0);

            try {
                if (cursor != null) {
                    cursor.moveToFirst();
                    for (int i = 0; i < contactsCount; i++) {
                        String temp[] = getVcfData(cursor);
                        ContactsDataPacket obj = new ContactsDataPacket(temp[0], temp[1]);
                        tempContactsStorage.add(obj);
                        publishProgress(i);
                        cursor.moveToNext();
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }

            return tempContactsStorage;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            contactsReadProgressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Vector<ContactsDataPacket> receivedDataPackets) {
            super.onPostExecute(contactsList);

            if (cursor != null) try {
                cursor.close();
            }catch (Exception ignored){}

            updateContactsList(receivedDataPackets);

            contactsMainItem.setClickable(true);
            contactsMainItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {


                    new LoadContactsForSelection().execute();

                }
            });
        }

        private String[] getVcfData(Cursor cursor) {
            String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            String vcardstring;
            String fullName;
            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
            AssetFileDescriptor fd;
            try {
                fd = ExtraBackups.this.getContentResolver().openAssetFileDescriptor(uri, "r");

                FileInputStream fis = fd.createInputStream();
                byte[] buf = readBytes(fis);
                fis.read(buf);
                vcardstring= new String(buf);
                fullName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            } catch (Exception e1)
            {
                e1.printStackTrace();
                fullName = "";
                vcardstring = "";
            }
            return new String[]{fullName, vcardstring};
        }

        byte[] readBytes(InputStream stream) throws IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int len;
            byte[] buffer = new byte[2048];
            while ((len = stream.read(buffer)) != -1){
                byteArrayOutputStream.write(buffer, 0, len);
            }
            return byteArrayOutputStream.toByteArray();
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

        contactsMainItem = findViewById(R.id.extra_item_contacts);
        contactsSelectedStatus = findViewById(R.id.contacts_selected_status);
        contactsReadProgressBar = findViewById(R.id.contacts_read_progress);

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

        try {
            contactsReader = new ReadContacts();
            contactsReader.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        try {
            contactsReader.cancel(true);
        }
        catch (Exception ignored){}
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ExtraBackups.this, BackupActivity.class));
    }

    void updateContactsList(Vector<ContactsDataPacket> newContactsDataPackets){

        if (newContactsDataPackets == null)
            return;

        int l = newContactsDataPackets.size();
        contactsReadProgressBar.setVisibility(View.VISIBLE);
        contactsSelectedStatus.setText(R.string.reading);
        contactsReadProgressBar.setMax(l);
        int n = 0;
        for (int i = 0; i < l; i++){
            if (newContactsDataPackets.get(i).selected)
                n++;
        }

        contactsList = newContactsDataPackets;
        contactsReadProgressBar.setVisibility(View.GONE);
        contactsSelectedStatus.setText(n + " of " + contactsList.size());


    }

    /*private void getVcardString() throws IOException {

        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if(cursor!=null&&cursor.getCount()>0)
        {
            int i;
            String storage_path = Environment.getExternalStorageDirectory().toString() + File.separator + "vFile.vcf";
            FileOutputStream mFileOutputStream = new FileOutputStream(storage_path, false);
            cursor.moveToFirst();
            for(i = 0;i<cursor.getCount();i++)
            {
                String vcard = get(cursor);
                cursor.moveToNext();
                mFileOutputStream.write(vcard.getBytes());
            }
            mFileOutputStream.close();
            cursor.close();
        }
        else
        {
            Log.d("TAG", "No Contacts in Your Phone");
        }
    }*/

    /*private void restoreContacts(){
        final MimeTypeMap mime = MimeTypeMap.getSingleton();
        String tmptype = mime.getMimeTypeFromExtension("vcf");
        final File file = new File(Environment.getExternalStorageDirectory().toString()
                + "/vFile.vcf");
        Intent i = new Intent();
        i.setAction(android.content.Intent.ACTION_VIEW);
        i.setDataAndType(Uri.fromFile(file), "text/x-vcard");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(i);
    }*/

}
