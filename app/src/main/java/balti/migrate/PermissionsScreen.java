package balti.migrate;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by sayantan on 20/1/18.
 */

public class PermissionsScreen extends AppCompatActivity {

    TextView storagePerm, rootPerm;
    Button grantPermissions;

    LinearLayout header;
    TextView permissionDesc;

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

        header = findViewById(R.id.permission_screen_header);
        permissionDesc = findViewById(R.id.permission_explanation);

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

        permissionDesc.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
        permissionDesc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(PermissionsScreen.this)
                        .setMessage(R.string.permissions_desc)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        final String cpu_abi = Build.SUPPORTED_ABIS[0];

        if (!(cpu_abi.equals("armeabi-v7a") || cpu_abi.equals("arm64-v8a") || cpu_abi.equals("x86") || cpu_abi.equals("x86_64"))){

            grantPermissions.setVisibility(View.GONE);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.unsupported_device)
                    .setMessage(getString(R.string.cpu_arch_is) + "\n" + cpu_abi + "\n\n" + getString(R.string.currently_supported_cpu))
                    .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.contact, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            String body = new CommonTools(PermissionsScreen.this).getDeviceSpecifications();

                            Intent email = new Intent(Intent.ACTION_SENDTO);
                            email.setData(Uri.parse("mailto:"));
                            email.putExtra(Intent.EXTRA_EMAIL, new String[]{"help.baltiapps@gmail.com"});
                            email.putExtra(Intent.EXTRA_SUBJECT, "Unsupported device");
                            email.putExtra(Intent.EXTRA_TEXT, body);

                            try {
                                startActivity(Intent.createChooser(email, getString(R.string.select_mail)));
                            }
                            catch (Exception e)
                            {
                                Toast.makeText(PermissionsScreen.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setCancelable(false)
                    .show();
        }

        else {

            grantPermissions.setVisibility(View.VISIBLE);

            boolean p[] = isPermissionGranted();


            if (!p[0])
                storagePerm.setVisibility(View.VISIBLE);
            else storagePerm.setVisibility(View.GONE);

            if (!p[1])
                rootPerm.setVisibility(View.VISIBLE);
            else rootPerm.setVisibility(View.GONE);


            if (p[0] && p[1]) {
                header.setVisibility(View.GONE);
                grantPermissions.setText(R.string.please_wait);
                startMainActivity();
            }
        }

    }


    boolean[] isPermissionGranted() {
        boolean[] p = new boolean[]{false, false};
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

        return p;
    }

    boolean suEcho() throws IOException, InterruptedException {
        boolean result;
        Process suRequest = Runtime.getRuntime().exec("su -c pm grant " + getPackageName() + " android.permission.DUMP && pm grant " + getPackageName() + " android.permission.PACKAGE_USAGE_STATS");
        suRequest.waitFor();
        if (suRequest.exitValue() == 0) result = true;
        else result = false;
        return result;
    }

    void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 0);
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
