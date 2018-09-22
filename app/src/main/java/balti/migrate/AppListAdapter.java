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

    AppListAdapter(Context context, Vector<BackupDataPacket> appList)
    {
        packageManager = context.getPackageManager();
        this.context = context;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        onCheck = (OnCheck)context;
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
    public View getView(final int i, View view, ViewGroup viewGroup) {

        view = layoutInflater.inflate(R.layout.app_item, null);

        TextView appName = view.findViewById(R.id.appName);
        appName.setText(packageManager.getApplicationLabel(appList.get(i).PACKAGE_INFO.applicationInfo));

        TextView packageName = view.findViewById(R.id.packageName);
        packageName.setText(appList.get(i).PACKAGE_INFO.packageName);

        ImageView icon = view.findViewById(R.id.appIcon);
        icon.setImageDrawable(packageManager.getApplicationIcon(appList.get(i).PACKAGE_INFO.applicationInfo));

        final CheckBox app, data;
        app = view.findViewById(R.id.appCheckbox);
        app.setChecked(appList.get(i).APP);
        app.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                appList.get(i).APP = b;
                onCheck.onCheck(appList);
            }
        });
        data = view.findViewById(R.id.dataCheckbox);
        data.setChecked(appList.get(i).DATA);

        final int p = exclusions.returnExclusionState(appList.get(i).PACKAGE_INFO.packageName);
        if (p == Exclusions.EXCLUDE_DATA)
            data.setEnabled(false);
        else if (p == Exclusions.EXCLUDE_APP_DATA){
            app.setEnabled(false);
            data.setEnabled(false);
        }

        if (data.isChecked()) {
            app.setChecked(true);
            app.setEnabled(false);
        }
        else app.setEnabled(true);
        data.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                appList.get(i).DATA = b;
                if (b)
                {
                    app.setChecked(true);
                    app.setEnabled(false);
                }
                else {
                    app.setEnabled(true);
                }
                onCheck.onCheck(appList);
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (appList.get(i).APP && appList.get(i).DATA){
                        data.setChecked(false);
                        app.setChecked(false);
                }
                else {

                    if (p == Exclusions.NOT_EXCLUDED) {
                        app.setChecked(true);
                        data.setChecked(true);
                    }
                    else if (p == Exclusions.EXCLUDE_DATA){
                        app.setChecked(true);
                    }
                }
            }
        });

        if (appList.get(i).PACKAGE_INFO.applicationInfo.sourceDir.startsWith("/system"))
            appName.setTextColor(Color.RED);

        return view;
    }

    void checkAllData(boolean check)
    {
        for (int i = 0; i < appList.size(); i++)
        {
            int p = exclusions.returnExclusionState(appList.get(i).PACKAGE_INFO.packageName);
            if (p == Exclusions.NOT_EXCLUDED) appList.elementAt(i).DATA = check;
        }
    }

    void checkAllApp(boolean check){
        for (int i = 0; i < appList.size(); i++)
        {
            int p = exclusions.returnExclusionState(appList.get(i).PACKAGE_INFO.packageName);
            if (p == Exclusions.NOT_EXCLUDED || p == Exclusions.EXCLUDE_DATA) appList.elementAt(i).APP = check;
        }
    }

    Vector<BackupDataPacket> sortByAppName(Vector<BackupDataPacket> appList){
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
