package balti.migrate;

import android.os.Bundle;

public class PreferenceScreen extends balti.bgvideo.AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
