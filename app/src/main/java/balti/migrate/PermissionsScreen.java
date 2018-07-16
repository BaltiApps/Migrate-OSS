package balti.migrate;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by sayantan on 20/1/18.
 */

public class PermissionsScreen extends AppCompatActivity {

    TextView storagePerm, rootPerm, injectPerm;
    Button grantPermissions;

    SharedPreferences main;
    SharedPreferences.Editor editor;

    boolean isInjectDialogShown = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permission_screen);

        main = getSharedPreferences("main", MODE_PRIVATE);
        editor = main.edit();

        storagePerm = (TextView) findViewById(R.id.storagePermTextView);
        rootPerm = (TextView) findViewById(R.id.rootPermTextView);
        injectPerm = (TextView) findViewById(R.id.injectTextView);

        grantPermissions = (Button) findViewById(R.id.grantPermissions);
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

    void showInjectDialog() {
        final boolean r[] = checkZipAndTar();
        if (!(r[0] && r[1])) {
            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.injectTitle))
                    .setMessage(getString(R.string.injectMessage))
                    .create();
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.proceed), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            try {
                                if (injectProcess(r)) {
                                    dialog.dismiss();
                                    isInjectDialogShown = false;
                                    startActivity(new Intent(PermissionsScreen.this, MainActivity.class));
                                    finish();
                                }
                                else dialog.setMessage(getString(R.string.injectFailed));
                            } catch (IOException | InterruptedException e) {
                                dialog.setMessage(getString(R.string.injectFailed));
                                e.printStackTrace();
                            }
                        }
                    });
            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            dialog.show();

        }
        else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    boolean injectProcess(boolean r[]) throws IOException, InterruptedException {

        Process suCopy = Runtime.getRuntime().exec("su");
        DataOutputStream outputStream = new DataOutputStream(suCopy.getOutputStream());

        outputStream.writeBytes("mount -o rw,remount,rw /system\n");
        if (!r[0]) {
            extractZip();
            outputStream.writeBytes("mkdir -p /system/xbin; mv " + getFilesDir() + "/zip /system/xbin/zip\n");
            outputStream.writeBytes("chmod 777 /system/xbin/zip\n");
        }
        if (!r[1]) {
            extractTar();
            outputStream.writeBytes("mkdir -p /system/bin; mv " + getFilesDir() + "/tar /system/bin/tar\n");
            outputStream.writeBytes("chmod 777 /system/bin/tar\n");
        }
        outputStream.writeBytes("mount -o ro,remount,ro /system\n");
        outputStream.close();
        suCopy.waitFor();
        if (suCopy.exitValue() == 0) {
            return true;
        } else {
            return false;
        }
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
            injectPerm.setVisibility(View.VISIBLE);
        else injectPerm.setVisibility(View.GONE);


    }

    void extractZip() {
        AssetManager manager = getAssets();
        File zipScript = new File(getFilesDir(), "zip");
        int read;
        byte buffer[] = new byte[4096];
        try {
            InputStream inputStream = manager.open("zip");
            FileOutputStream writer = new FileOutputStream(zipScript);
            while ((read = inputStream.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void extractTar() {
        AssetManager manager = getAssets();
        File zipScript = new File(getFilesDir(), "tar");
        int read;
        byte buffer[] = new byte[4096];
        try {
            InputStream inputStream = manager.open("tar");
            FileOutputStream writer = new FileOutputStream(zipScript);
            while ((read = inputStream.read(buffer)) > 0) {
                writer.write(buffer, 0, read);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    boolean[] isPermissionGranted() {
        boolean[] p = new boolean[]{false, false, false};
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
        boolean p1[] = checkZipAndTar();
        p[2] = p1[0] && p1[1];
        return p;
    }

    boolean suEcho() throws IOException, InterruptedException {
        boolean result = false;
        Process suRequest = Runtime.getRuntime().exec("su -c echo");
        suRequest.waitFor();
        if (suRequest.exitValue() == 0) result = true;
        else result = false;
        return result;
    }

    boolean[] checkZipAndTar(){
        Process checkZip = null;
        Process checkTar = null;
        boolean r[] = {false, false};
        try {
            checkZip = Runtime.getRuntime().exec("zip --help");
            checkZip.waitFor();
            r[0] = true;
        } catch (IOException | InterruptedException e) {
            r[0] = false;
        }
        try {
            checkTar = Runtime.getRuntime().exec("tar --help");
            checkTar.waitFor();
            r[1] = true;
        } catch (IOException | InterruptedException e) {
            r[1] = false;
        }
        return r;
    }


    void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0){
            boolean p1 = false;

            try {
                p1 = suEcho();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            if (p1){
                editor.putBoolean("initialRoot", false);
                editor.commit();
            }

            if (!isInjectDialogShown) {
                showInjectDialog();
                isInjectDialogShown = true;
            }

            grantPermissions.setText(getString(R.string.grantPermissions));
            grantPermissions.setEnabled(true);
        }
    }

}
