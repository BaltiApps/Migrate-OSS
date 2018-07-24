package balti.migrate;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

public class MainActivity extends AppCompatActivity {

    SharedPreferences main;
    SharedPreferences.Editor editor;

    Button backup, restore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        main = getSharedPreferences("main", MODE_PRIVATE);
        editor = main.edit();

        if (main.getBoolean("firstRun", true)){

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                NotificationChannel channel = new NotificationChannel(BackupService.CHANNEL, BackupService.CHANNEL, NotificationManager.IMPORTANCE_DEFAULT);
                channel.setSound(null, null);
                assert notificationManager != null;
                notificationManager.createNotificationChannel(channel);
            }
        }

        backup = (Button) findViewById(R.id.backupMain);
        backup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, BackupActivity.class));
            }
        });

        restore = (Button) findViewById(R.id.restoreMain);

    }

    boolean[] isPermissionGranted() {
        boolean[] p = new boolean[]{false, false};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            p[0] = true;
        if (main.getBoolean("initialRoot", true)) {
            p[1] = false;
        } else {
            try {
                Process suRequest = Runtime.getRuntime().exec("su -c echo");
                suRequest.waitFor();
                if (suRequest.exitValue() == 0) p[1] = true;
                else p[1] = false;
            } catch (Exception e) {
                p[1] = false;
            }
        }
        return p;
    }


    @Override
    protected void onResume() {
        super.onResume();
        boolean[] p = isPermissionGranted();
        if (!(p[0] && p[1])) {
            startActivity(new Intent(this, PermissionsScreen.class));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        boolean p[] = isPermissionGranted();
        if (p[0] && p[1]) {
            if (main.getBoolean("firstRun", true)) {
                editor.putBoolean("firstRun", false);
                editor.commit();
            }
        }
    }
}
