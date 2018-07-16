package balti.migrate;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

/**
 * Created by sayantan on 29/10/17.
 */

public class Exclusions {
    Vector<String> predefinedPackageNames;
    Vector<String> manualPackageNames;
    Context context;

    final String EXCLUSION_FILE_NAME = "Exclusions";
    static final int EXCLUDE_DATA = 1;
    static final int EXCLUDE_APP_DATA = 2;
    static final int NOT_EXCLUDED = 0;
    static final int REMOVE_EXCLUSION = -1;

    public Exclusions(Context context) {
        this.context = context;
        predefinedPackageNames = new Vector<>(1);
        manualPackageNames = new Vector<>(1);
        predefinedPackageNames.addElement("com.topjohnwu.magisk " + EXCLUDE_DATA);
        predefinedPackageNames.addElement("eu.chainfire.supersu " + EXCLUDE_DATA);
        predefinedPackageNames.addElement("com.noshufou.android.su " + EXCLUDE_DATA);
        predefinedPackageNames.addElement("me.phh.superuser " + EXCLUDE_DATA);
        predefinedPackageNames.addElement("com.bitcubate.android.su.installer " + EXCLUDE_DATA);
        predefinedPackageNames.addElement("com.gorserapp.superuser " + EXCLUDE_DATA);
        predefinedPackageNames.addElement("com.farma.superuser " + EXCLUDE_DATA);
        predefinedPackageNames.addElement("com.koushikdutta.superuser " + EXCLUDE_DATA);
        predefinedPackageNames.addElement("de.robv.android.xposed.installer " + EXCLUDE_DATA);
        predefinedPackageNames.addElement("balti.migratehelper " + EXCLUDE_APP_DATA);
        readManuallyExcludedPackages();
    }

    void addNewExclusion(String packageName, int mode){
        manualPackageNames.addElement(packageName + " " + mode);
    }

    private void readManuallyExcludedPackages(){
        File exclusions = new File(context.getFilesDir().getAbsolutePath(), EXCLUSION_FILE_NAME);
        if (exclusions.exists()){
            try {
                BufferedReader reader = new BufferedReader(new FileReader(exclusions));
                String line;
                while ((line = reader.readLine()) != null){
                    manualPackageNames.addElement(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void changeExclusion(String packageName, int mode){
        for (int i = 0; i < manualPackageNames.size(); i++){
            String l = manualPackageNames.elementAt(i);
            if (l.indexOf(' ') != -1 && l.substring(0, l.indexOf(' ')).equals(packageName)){
                if (mode != REMOVE_EXCLUSION){
                    manualPackageNames.remove(i);
                    manualPackageNames.insertElementAt(packageName + " " + mode, i);
                }
                else manualPackageNames.remove(i);
                break;
            }
        }
    }

    void saveChanges() {
        File exclusions = new File(context.getFilesDir().getAbsolutePath() + EXCLUSION_FILE_NAME);
        if (manualPackageNames.size() == 0) exclusions.delete();
        else {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(exclusions));
                for (int i = 0; i < manualPackageNames.size(); i++) {
                    writer.write(manualPackageNames.elementAt(i) + "\n");
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    int returnExclusionState(String packageName){
        int p = 0;
        String dataExcluded = packageName + " " + EXCLUDE_DATA;
        String appDataExcluded = packageName + " " + EXCLUDE_APP_DATA;
        if (predefinedPackageNames.contains(dataExcluded) || manualPackageNames.contains(dataExcluded))
            p = EXCLUDE_DATA;
        else if (predefinedPackageNames.contains(appDataExcluded) || manualPackageNames.contains(appDataExcluded))
            p = EXCLUDE_APP_DATA;
        return p;
    }

}
