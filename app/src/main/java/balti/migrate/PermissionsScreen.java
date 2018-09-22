package balti.migrate;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

/**
 * Created by sayantan on 20/1/18.
 */

public class PermissionsScreen extends AppCompatActivity {

    TextView storagePerm, rootPerm, contactsAccess, smsAccess, callsAccess;
    Button grantPermissions;

    SharedPreferences main;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permission_screen);

        main = getSharedPreferences("main", MODE_PRIVATE);
        editor = main.edit();

        storagePerm = findViewById(R.id.storagePermTextView);
        rootPerm = findViewById(R.id.rootPermTextView);
        contactsAccess = findViewById(R.id.contactsPermTextView);
        smsAccess = findViewById(R.id.smsPermTextView);
        callsAccess = findViewById(R.id.callsPermTextView);

        grantPermissions = findViewById(R.id.grantPermissions);
        grantPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermissions();
                grantPermissions.setEnabled(false);
                grantPermissions.setText(getString(R.string.requesting));
                onResume();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean p[] = isPermissionGranted();


        if (!p[0])
            storagePerm.setVisibility(View.VISIBLE);
        else storagePerm.setVisibility(View.GONE);

        if (!p[1])
            rootPerm.setVisibility(View.VISIBLE);
        else rootPerm.setVisibility(View.GONE);

        if (!p[2])
            contactsAccess.setVisibility(View.VISIBLE);
        else contactsAccess.setVisibility(View.GONE);

        if (!p[3])
            smsAccess.setVisibility(View.VISIBLE);
        else smsAccess.setVisibility(View.GONE);

        if (!p[4])
            callsAccess.setVisibility(View.VISIBLE);
        else callsAccess.setVisibility(View.GONE);

        if (p[0] && p[1] && p[2] && p[3] && p[4])
            startMainActivity();

    }


    boolean[] isPermissionGranted() {
        boolean[] p = new boolean[]{false, false, false, false, false};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            p[0] = true;

        if (main.getBoolean("initialRoot", true)) {
            p[1] = false;
        } else {
            try {
                p[1] = suEcho();
            } catch (Exception e) {
                p[1] = false;
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED)
            p[2] = true;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED)
            p[3] = true;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED)
            p[4] = true;

        return p;
    }

    boolean suEcho() throws IOException, InterruptedException {
        boolean result;
        Process suRequest = Runtime.getRuntime().exec("su -c echo");
        suRequest.waitFor();
        if (suRequest.exitValue() == 0) result = true;
        else result = false;
        return result;
    }

    void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_SMS, Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0){
            boolean p1 = false;

            try {
                p1 = suEcho();

                if (!p1)
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.root_permission_denied)
                            .setMessage(R.string.root_permission_denied_desc)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            if (p1){
                editor.putBoolean("initialRoot", false);
                editor.commit();
            }

            grantPermissions.setText(getString(R.string.grantPermissions));
            grantPermissions.setEnabled(true);
        }
    }

    void startMainActivity(){
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}
