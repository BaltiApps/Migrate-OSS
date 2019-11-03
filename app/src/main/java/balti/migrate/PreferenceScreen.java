package balti.migrate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.widget.Toast;

import java.util.Objects;

public class PreferenceScreen extends balti.migrate.AppCompatPreferenceActivity {

    Preference disableBatteryOptimisation;
    CheckBoxPreference useNewSizingMethod;

    SharedPreferences main;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_old);

        main = getSharedPreferences("main", MODE_PRIVATE);
        editor = main.edit();

        disableBatteryOptimisation = findPreference("disable_battery_optimisation");

        disableBatteryOptimisation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                if (isBatteryOptimisationDisabled())
                    Toast.makeText(PreferenceScreen.this, R.string.battery_optimisation_already_disabled, Toast.LENGTH_SHORT).show();
                else
                    askToDisbleBattery();

                return true;
            }
        });

        useNewSizingMethod = (CheckBoxPreference) findPreference("use_new_sizing_method");

        int method = main.getInt("calculating_size_method", 2);
        if (method == 2) {
            useNewSizingMethod.setChecked(true);
            useNewSizingMethod.setSummary(R.string.new_method_will_be_used);
        } else {
            useNewSizingMethod.setChecked(false);
            useNewSizingMethod.setSummary(R.string.old_method_will_be_used);
        }

        useNewSizingMethod.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                if ((boolean) newValue) {
                    editor.putInt("calculating_size_method", 2);
                    useNewSizingMethod.setSummary(R.string.new_method_will_be_used);
                } else {
                    editor.putInt("calculating_size_method", 1);
                    useNewSizingMethod.setSummary(R.string.old_method_will_be_used);
                }
                editor.commit();

                return true;
            }
        });

    }

    boolean isBatteryOptimisationDisabled() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Objects.requireNonNull(pm).isIgnoringBatteryOptimizations(getPackageName());
        } else return false;
    }

    void askToDisbleBattery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
}
