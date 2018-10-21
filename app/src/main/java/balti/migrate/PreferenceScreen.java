package balti.migrate;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.Preference;
import android.provider.Settings;
import android.widget.Toast;

import java.util.Objects;

public class PreferenceScreen extends balti.migrate.AppCompatPreferenceActivity {

    Preference disableBatteryOptimisation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

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

    }

    boolean isBatteryOptimisationDisabled(){
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Objects.requireNonNull(pm).isIgnoringBatteryOptimizations(getPackageName());
        }
        else return false;
    }

    void askToDisbleBattery(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
}
