package balti.migrate;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    SharedPreferences main;
    SharedPreferences.Editor editor;

    Button backup, restore;
    ImageButton drawerButton;

    DrawerLayout drawer;
    NavigationView navigationView;

    CommonTools commonTools;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        main = getSharedPreferences("main", MODE_PRIVATE);
        editor = main.edit();

        commonTools = new CommonTools(this);

        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigationDrawer);

        if (main.getBoolean("firstRun", true)) {

            editor.putInt("version", 1);
            editor.commit();

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                NotificationChannel channel = new NotificationChannel(BackupService.BACKUP_START_NOTIFICATION,
                        BackupService.BACKUP_START_NOTIFICATION, NotificationManager.IMPORTANCE_DEFAULT);
                channel.setSound(null, null);
                assert notificationManager != null;
                notificationManager.createNotificationChannel(channel);
            }
        }
        else showChangeLog(true);

        backup = findViewById(R.id.backupMain);
        backup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, BackupActivity.class));
            }
        });

        restore = findViewById(R.id.restoreMain);
        restore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HowToRestore.class));
            }
        });

        drawerButton = findViewById(R.id.drawerButton);
        drawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawer.openDrawer(Gravity.START);
            }
        });

        navigationView.setNavigationItemSelectedListener(this);

    }

    boolean[] isPermissionGranted() {
        boolean[] p = new boolean[]{false, false};

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            p[0] = true;

        if (main.getBoolean("initialRoot", true)) {
            p[1] = false;
        } else {
            try {
                Process suRequest = Runtime.getRuntime().exec("su -c pm grant " + getPackageName() + " android.permission.DUMP " +
                        "&& pm grant " + getPackageName() + " android.permission.PACKAGE_USAGE_STATS " +
                        "&& pm grant " + getPackageName() + " android.permission.WRITE_SECURE_SETTINGS"
                );
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
        boolean p[] = isPermissionGranted();
        if (!(p[0] && p[1])) {
            startActivity(new Intent(this, PermissionsScreen.class));
            finish();
        }
        else {

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && !main.getBoolean("android_version_warning", false)){
                new AlertDialog.Builder(this)
                        .setTitle(R.string.too_fast)
                        .setMessage(R.string.too_fast_desc)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(R.string.dont_show_again, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                editor.putBoolean("android_version_warning", true);
                                editor.commit();
                            }
                        })
                        .show();
            }
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

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(Gravity.START)) drawer.closeDrawer(Gravity.START);
        else if (!main.getBoolean("firstRun", true) && main.getBoolean("askForRating", true))
            askForRating(false);
        else super.onBackPressed();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id)
        {
            case R.id.about:
                AlertDialog.Builder about = new AlertDialog.Builder(this);
                View v = getLayoutInflater().inflate(R.layout.about_app, null);
                about.setView(v)
                        .setTitle(getString(R.string.about))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                break;

            case R.id.helpPage:
                startActivity(new Intent(this, HelpPage.class));
                break;

            case R.id.changelog:
                showChangeLog(false);
                break;

            case R.id.lastLog:
                reportLog();
                break;


            case R.id.appIntro:
                startActivity(new Intent(this, InitialGuide.class).putExtra("manual", true));
                break;

            case R.id.thanks:
                AlertDialog.Builder specialThanks = new AlertDialog.Builder(this);
                specialThanks.setMessage(getString(R.string.thanksToAll) + "\n\n" +
                        getString(R.string.tester) + "\n\n" +
                        "@akshayrohida\n" +
                        "@TusharG03\n" +
                        "@raj3303\n" +
                        "@arghyac35\n" +
                        "@Johnkator\n" +
                        "@samirkushwaha\n" +
                        "@su_bin\n" +
                        "Pranay\n" +
                        "@ishubhamsingh\n" +
                        "@Sachith_Hedge\n" +
                        "@Akianonymus\n" +
                        "@SubhrajyotiSen\n")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                break;

            case R.id.preferences:
                startActivity(new Intent(this, PreferenceScreen.class));
                break;

            case R.id.rate:
                askForRating(true);
                break;

            case R.id.contact:
                android.app.AlertDialog.Builder confirmEmail;
                confirmEmail = new android.app.AlertDialog.Builder(MainActivity.this);
                confirmEmail.setTitle(getString(R.string.sure_to_mail))
                        .setMessage(getString(R.string.sure_to_mail_exp))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                Intent email = new Intent(Intent.ACTION_SENDTO);
                                email.setData(Uri.parse("mailto:"));
                                email.putExtra(Intent.EXTRA_EMAIL, new String[]{"help.baltiapps@gmail.com"});
                                email.putExtra(Intent.EXTRA_SUBJECT, "Bug report for Migrate");
                                email.putExtra(Intent.EXTRA_TEXT, commonTools.getDeviceSpecifications());

                                try {
                                    startActivity(Intent.createChooser(email, getString(R.string.select_mail)));
                                }
                                catch (Exception e)
                                {
                                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .setNeutralButton(R.string.help, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startActivity(new Intent(MainActivity.this, HelpPage.class));
                            }
                        })
                        .show();
                break;

            case R.id.otherApps:
                View view = getLayoutInflater().inflate(R.layout.other_apps, null);
                android.app.AlertDialog.Builder a = new android.app.AlertDialog.Builder(MainActivity.this);
                a.setView(view);
                a.setTitle(R.string.other_apps);
                a.setPositiveButton(android.R.string.ok, null);
                a.show();
                otherAppsClickManager(view);
                break;
        }
        drawer.closeDrawer(Gravity.START);

        return true;
    }


    void showChangeLog(boolean onlyLatest)
    {
        int currVer = main.getInt("version", 1);
        android.support.v7.app.AlertDialog.Builder changelog  = new android.support.v7.app.AlertDialog.Builder(this);
        String message = "";
        String title = "";
        if (onlyLatest) {
            if (currVer < 1) {
                /*Put only the latest version here*/
                title = getString(R.string.version_1_0);
                message = getString(R.string.version_1_0_content);
                changelog.setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(R.string.close, null)
                        .show();

                editor.putInt("version", 1);
                editor.commit();
            }
        }
        else
        {
            title = getString(R.string.changelog);
            message = "";
            /*Add increasing versions here*/
            message = message + "\n" + getString(R.string.version_1_0) + "\n" + getString(R.string.version_1_0_content) + "\n";
            changelog.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.close, null)
                    .show();
        }

    }

    public void otherAppsClickManager(View layout)
    {
        //other apps links

        LinearLayout mdh = layout.findViewById(R.id.motodisplay_handwave) ;

        mdh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mdhPage = new Intent (Intent.ACTION_VIEW);
                mdhPage.setData(Uri.parse("market://details?id=sayantanrc.motodisplayhandwave"));
                startActivity(mdhPage);
            }
        });

        LinearLayout instamean = layout.findViewById(R.id.instamean) ;

        instamean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent instameanPage = new Intent (Intent.ACTION_VIEW);
                instameanPage.setData(Uri.parse("market://details?id=balti.instamean"));
                startActivity(instameanPage);
            }
        });

        LinearLayout bg_video = layout.findViewById(R.id.bg_video) ;

        bg_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent bgvideoPage = new Intent (Intent.ACTION_VIEW);
                bgvideoPage.setData(Uri.parse("market://details?id=balti.bgvideo"));
                startActivity(bgvideoPage);
            }
        });

        LinearLayout opc8085 = layout.findViewById(R.id.opcode_8085) ;

        opc8085.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent opc8085Page = new Intent (Intent.ACTION_VIEW);
                opc8085Page.setData(Uri.parse("market://details?id=balti.opcode8085"));
                startActivity(opc8085Page);
            }
        });

        LinearLayout prs = layout.findViewById(R.id.pickRingStop) ;

        prs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent prsIntent = new Intent (Intent.ACTION_VIEW);
                prsIntent.setData(Uri.parse("market://details?id=balti.pickringstop"));
                startActivity(prsIntent);
            }
        });
    }


    void askForRating(boolean manual) {

        AlertDialog.Builder rate = new AlertDialog.Builder(this);
        rate.setTitle(getString(R.string.rate_dialog_title))
                .setIcon(R.drawable.ic_rate)
                .setMessage(getString(R.string.rate_dialog_message))
                .setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Uri uri = Uri.parse("market://details?id=balti.migrate");
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        editor.putBoolean("askForRating", false);
                        editor.commit();
                    }
                });
        if (!manual) {
            rate.setNeutralButton(getString(R.string.never_show), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    editor.putBoolean("askForRating", false);
                    editor.commit();
                    finish();
                }
            })
                    .setNegativeButton(getString(R.string.later), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    });
        } else {
            rate.setNegativeButton("Cancel", null);
        }
        rate.show();
    }

    void reportLog(){
        View lView = View.inflate(this, R.layout.last_log_report, null);
        Button pLog = lView.findViewById(R.id.view_progress_log);
        Button eLog = lView.findViewById(R.id.view_error_log);
        Button report = lView.findViewById(R.id.report_logs);

        final AlertDialog ad = new AlertDialog.Builder(this, R.style.DarkAlert)
                .setTitle(R.string.lastLog)
                .setIcon(R.drawable.ic_log)
                .setView(lView)
                .setNegativeButton(R.string.close, null)
                .create();

        pLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File f = new File(getExternalCacheDir(), "progressLog");
                if (f.exists())
                    startActivity(
                            new Intent(MainActivity.this, SimpleLogDisplay.class)
                                    .putExtra("head", getString(R.string.progressLog))
                                    .putExtra("filePath", f.getAbsolutePath())
                    );
                else Toast.makeText(MainActivity.this, getString(R.string.progress_log_does_not_exist), Toast.LENGTH_SHORT).show();
            }
        });

        eLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File f = new File(getExternalCacheDir(), "errorLog");
                if (f.exists())
                    startActivity(
                            new Intent(MainActivity.this, SimpleLogDisplay.class)
                                    .putExtra("head", getString(R.string.errorLog))
                                    .putExtra("filePath", f.getAbsolutePath())
                    );
                else Toast.makeText(MainActivity.this, getString(R.string.error_log_does_not_exist), Toast.LENGTH_SHORT).show();
            }
        });

        report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commonTools.reportLogs(false);
                ad.dismiss();
            }
        });

        ad.show();
    }
}
