package balti.migrate;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

/**
 * Created by sayantan on 8/10/17.
 */

public class AppListAdapter extends BaseAdapter {
    Vector<BackupDataPacket> appList;
    LayoutInflater layoutInflater;
    Context context;


    PackageManager packageManager;

    OnCheck onCheck;
    Exclusions exclusions;

    AppListAdapter(Context context, Vector<BackupDataPacket> appList) {
        packageManager = context.getPackageManager();
        this.context = context;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        onCheck = (OnCheck) context;
        this.appList = sortByAppName(appList);
        exclusions = new Exclusions(context);
    }

    @Override
    public int getCount() {
        return appList.size();
    }

    @Override
    public Object getItem(int i) {
        return appList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return getCount();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        view = layoutInflater.inflate(R.layout.app_item, null);

        final BackupDataPacket appItem = appList.get(i);

        final TextView appName = view.findViewById(R.id.appName);
        appName.setText(packageManager.getApplicationLabel(appItem.PACKAGE_INFO.applicationInfo));

        ImageView icon = view.findViewById(R.id.appIcon);
        icon.setImageDrawable(packageManager.getApplicationIcon(appItem.PACKAGE_INFO.applicationInfo));

        ImageView appInfo = view.findViewById(R.id.appInfo);
        appInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                        .setTitle(appName.getText())
                        .setMessage(context.getString(R.string.packageName) + " " + appItem.PACKAGE_INFO.packageName + "\n" +
                                context.getString(R.string.versionName) + " " + appItem.PACKAGE_INFO.versionName + "\n" +
                                context.getString(R.string.uid) + " " + appItem.PACKAGE_INFO.applicationInfo.uid + "\n" +
                                context.getString(R.string.sourceDir) + " " + appItem.PACKAGE_INFO.applicationInfo.sourceDir + "\n" +
                                context.getString(R.string.dataDir) + " " + appItem.PACKAGE_INFO.applicationInfo.dataDir
                        )
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });

        final CheckBox app, data, permission;

        app = view.findViewById(R.id.appCheckbox);
        app.setChecked(appItem.APP);

        data = view.findViewById(R.id.dataCheckbox);
        data.setChecked(appItem.DATA);

        permission = view.findViewById(R.id.permissionCheckbox);
        permission.setChecked(appItem.PERMISSIONS);

        final int p = exclusions.returnExclusionState(appList.get(i).PACKAGE_INFO.packageName);

        if (p == Exclusions.EXCLUDE_DATA)
            data.setEnabled(false);
        else if (p == Exclusions.EXCLUDE_APP_DATA) {
            app.setEnabled(false);
            data.setEnabled(false);
            permission.setEnabled(false);
            appItem.IS_PERMISSIBLE = false;
        }

        if (data.isChecked()) {
            app.setChecked(true);
            app.setEnabled(false);
        } else if (p != Exclusions.EXCLUDE_APP_DATA) app.setEnabled(true);

        permission.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                appItem.PERMISSIONS = isChecked;
                onCheck.onCheck(appList);
            }
        });

        if (app.isChecked()) {
            permission.setEnabled(true);
            appItem.IS_PERMISSIBLE = true;
        } else {
            permission.setEnabled(false);
            permission.setChecked(false);
            appItem.IS_PERMISSIBLE = false;
        }

        app.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                appItem.APP = b;
                if (!b) {
                    appItem.IS_PERMISSIBLE = false;
                    permission.setChecked(false);
                    permission.setEnabled(false);
                } else {
                    appItem.IS_PERMISSIBLE = true;
                    permission.setEnabled(true);
                }
                onCheck.onCheck(appList);
            }
        });

        data.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                appItem.DATA = b;
                if (b) {
                    app.setChecked(true);
                    app.setEnabled(false);
                } else {
                    app.setEnabled(true);
                }
                onCheck.onCheck(appList);
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (appItem.APP && appItem.DATA && appItem.PERMISSIONS) {
                    data.setChecked(false);
                    app.setChecked(false);
                    permission.setChecked(false);
                } else {

                    if (p == Exclusions.NOT_EXCLUDED) {
                        app.setChecked(true);
                        data.setChecked(true);
                        permission.setChecked(true);
                    } else if (p == Exclusions.EXCLUDE_DATA) {
                        app.setChecked(true);
                        permission.setChecked(true);
                    }
                }
            }
        });

        if (appItem.PACKAGE_INFO.applicationInfo.sourceDir.startsWith("/system"))
            appName.setTextColor(Color.RED);

        return view;
    }

    void checkAllData(boolean check) {
        for (int i = 0; i < appList.size(); i++) {
            int p = exclusions.returnExclusionState(appList.get(i).PACKAGE_INFO.packageName);
            if (p == Exclusions.NOT_EXCLUDED) {
                appList.elementAt(i).DATA = check;
            }
        }
        onCheck.onCheck(appList);
    }

    void checkAllApp(boolean check) {
        for (int i = 0; i < appList.size(); i++) {
            int p = exclusions.returnExclusionState(appList.get(i).PACKAGE_INFO.packageName);
            if (p == Exclusions.NOT_EXCLUDED || p == Exclusions.EXCLUDE_DATA) {
                appList.elementAt(i).APP = check;
                appList.elementAt(i).IS_PERMISSIBLE = check;
            }
        }
        onCheck.onCheck(appList);
    }

    void checkAllPermissions(boolean check) {
        for (int i = 0; i < appList.size(); i++) {
            if (check) {
                if (appList.elementAt(i).APP) appList.elementAt(i).PERMISSIONS = true;
            } else appList.elementAt(i).PERMISSIONS = false;
        }
        onCheck.onCheck(appList);
    }

    Vector<BackupDataPacket> sortByAppName(Vector<BackupDataPacket> appList) {
        Vector<BackupDataPacket> sortedAppList = appList;
        Collections.sort(sortedAppList, new Comparator<BackupDataPacket>() {
            @Override
            public int compare(BackupDataPacket o1, BackupDataPacket o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(packageManager.getApplicationLabel(o1.PACKAGE_INFO.applicationInfo).toString(), packageManager.getApplicationLabel(o2.PACKAGE_INFO.applicationInfo).toString());
            }
        });
        return sortedAppList;
    }

}
