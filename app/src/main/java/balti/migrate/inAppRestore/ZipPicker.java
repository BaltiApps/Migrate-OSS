package balti.migrate.inAppRestore;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import balti.migrate.CommonTools;
import balti.migrate.R;

import static balti.migrate.CommonTools.DEBUG_TAG;
import static balti.migrate.CommonTools.DEFAULT_INTERNAL_STORAGE_DIR;

public class ZipPicker extends AppCompatActivity implements OnZipItemClick {

    ListView zipList;
    LinearLayout up;
    RadioGroup storageSelector;
    RadioButton internal;
    RadioButton external;
    Button next;

    SharedPreferences main;
    SharedPreferences.Editor editor;
    CommonTools commonTools;

    String destination;
    String finalParent;

    static ArrayList<ZipFileItem> zipFileItems;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zip_picker);

        main = getSharedPreferences("main", MODE_PRIVATE);
        editor = main.edit();
        commonTools = new CommonTools(this);

        finalParent = destination = main.getString("defaultBackupPath", DEFAULT_INTERNAL_STORAGE_DIR);
        zipFileItems = new ArrayList<>(0);

        zipList = findViewById(R.id.zip_list);
        up = findViewById(R.id.zipPickerUpButton);
        storageSelector = findViewById(R.id.zipPickerStorageSelectorRadioGroup);
        internal = findViewById(R.id.zipPickerInternal);
        external = findViewById(R.id.zipPickerExternal);
        next = findViewById(R.id.zipPickerNext);

        storageSelector.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == internal.getId()){
                    setDestination(DEFAULT_INTERNAL_STORAGE_DIR);
                }
                else if (checkedId == external.getId()) {

                    ImageView imageView = new ImageView(ZipPicker.this);
                    imageView.setImageResource(R.drawable.ex_sd_card_enabler);
                    imageView.setPadding(10, 10, 10, 10);


                    String sdCardProbableDirs[] = commonTools.getSdCardPaths();
                    final ArrayList<String> sdCardPaths = new ArrayList<>(0);
                    for (String sdDir : sdCardProbableDirs) {
                        if (!sdDir.equals(""))
                            sdCardPaths.add(sdDir);
                    }

                    if (sdCardProbableDirs.length == 0) {
                        new AlertDialog.Builder(ZipPicker.this)
                                .setTitle(R.string.no_sd_card_detected)
                                .setMessage(R.string.no_sd_card_detected_exp)
                                .setPositiveButton(R.string.close, null)
                                .setNegativeButton(R.string.learn_about_sd_card_support, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        commonTools.showSdCardSupportDialog();
                                    }
                                })
                                .setView(imageView)
                                .show();
                        internal.setChecked(true);
                    } else if (sdCardPaths.size() == 1) {
                        setDestination(sdCardPaths.get(0) + "/Migrate");
                    } else if (sdCardPaths.size() > 0) {

                        final RadioGroup sdGroup = new RadioGroup(ZipPicker.this);
                        sdGroup.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        sdGroup.setPadding(20, 20, 20, 20);

                        for (int i = 0; i < sdCardPaths.size(); i++) {

                            final File sdFile = new File(sdCardPaths.get(i));

                            RadioButton button = new RadioButton(ZipPicker.this);
                            button.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            button.setText(sdFile.getName());
                            button.setId(i);

                            sdGroup.addView(button);
                        }

                        sdGroup.check(0);

                        new AlertDialog.Builder(ZipPicker.this)
                                .setTitle(R.string.please_select_sd_card)
                                .setView(sdGroup)
                                .setCancelable(false)
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        internal.setChecked(true);
                                        setDestination(DEFAULT_INTERNAL_STORAGE_DIR);
                                    }
                                })
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        setDestination(sdCardPaths.get(sdGroup.getCheckedRadioButtonId()) + "/Migrate");
                                    }
                                })
                                .show();

                    } else {
                        new AlertDialog.Builder(ZipPicker.this)
                                .setTitle(R.string.sd_card_not_rw)
                                .setMessage(R.string.sd_card_not_rw_exp)
                                .setPositiveButton(R.string.close, null)
                                .setNegativeButton(R.string.learn_about_sd_card_support, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        commonTools.showSdCardSupportDialog();
                                    }
                                })
                                .setView(imageView)
                                .show();
                        internal.setChecked(true);
                    }

                }
            }
        });

        if (finalParent.equals(DEFAULT_INTERNAL_STORAGE_DIR) ||
                !(new File(main.getString("defaultBackupPath", DEFAULT_INTERNAL_STORAGE_DIR)).canWrite()))
            storageSelector.check(internal.getId());
        else {
            storageSelector.check(external.getId());
        }

        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!destination.equals(finalParent)){
                    refreshZipList(destination.substring(0, destination.lastIndexOf('/')));
                }
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder zipReadErrorAd = new AlertDialog.Builder(ZipPicker.this);

                for (ZipFileItem zfi : zipFileItems){

                    try {
                        ZipFile zipFile = new ZipFile(zfi.file);
                        // get an enumeration of the ZIP file entries
                        Enumeration<? extends ZipEntry> e = zipFile.entries();

                        while (e.hasMoreElements()) {

                            ZipEntry entry = e.nextElement();

                            // get the name of the entry
                            String entryName = entry.getName();

                            Log.d(DEBUG_TAG, "zipEntry: " + entryName);

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }
        });

        refreshZipList(destination);
    }

    private void setDestination(String path) {

        File d = new File(destination);
        if (!d.exists()) d.mkdirs();

        destination = path;
        refreshZipList(destination);
        editor.putString("defaultBackupPath", destination);
        editor.commit();
    }

    private void refreshZipList(String destination){
        File files[] = new File(destination).listFiles(
                new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isDirectory() || pathname.getName().endsWith(".zip");
                    }
                });

        ZipFileItem[] zfis = new ZipFileItem[files.length];

        if (zfis.length == 0) {
            zfis = new ZipFileItem[1];

            ZipFileItem empty = new ZipFileItem();
            empty.file = null;
            empty.isSelected = false;

            zfis[0] = empty;
        }
        else {
            for (int i = 0; i < files.length; i++) {
                zfis[i] = new ZipFileItem();
                zfis[i].file = files[i];
                zfis[i].isSelected = false;
            }
        }
        ZipItemAdapter zipItemAdapter = new ZipItemAdapter(this, zfis);
        zipList.setAdapter(zipItemAdapter);
        this.destination = destination;
    }

    @Override
    public void onZipItemClick(ZipFileItem zipFileItem) {
        if (zipFileItem.file.isDirectory()){
            refreshZipList(zipFileItem.file.getAbsolutePath());
        }
        else {
            if (!zipFileItem.file.isDirectory()) {
                boolean isPresent = false;
                for (ZipFileItem z : ZipPicker.zipFileItems){
                    if (zipFileItem.file.getAbsolutePath().equals(z.file.getAbsolutePath())){
                        z.isSelected = zipFileItem.isSelected;
                        isPresent = true;
                        break;
                    }
                }
                if (!isPresent)
                    zipFileItems.add(zipFileItem);
            }
        }
    }
}
