package balti.migrate;

import android.os.Bundle;

public class PreferenceScreen extends balti.migrate.AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
