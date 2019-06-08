package balti.migrate.activities

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import balti.migrate.*
import balti.migrate.CommonTools.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.inAppRestore.ZipPicker
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.last_log_report.view.*
import kotlinx.android.synthetic.main.please_wait.view.*
import java.io.File

class MainActivityKotlin : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val main : SharedPreferences by lazy { getSharedPreferences("main", Context.MODE_PRIVATE) }
    private val editor : SharedPreferences.Editor by lazy { main.edit() }
    private val commonTools by lazy { CommonTools(this) }                                               /*kotlin*/

    private val REQUEST_CODE_BACKUP = 43
    private val REQUEST_CODE_RESTORE = 5443

    private var rootErrorMessage = ""
    private var loadingDialog: AlertDialog? = null

    private val storageHandler by lazy { Handler() }
    private val storageRunnable by lazy { object : Runnable {
        override fun run() {
            refreshStorageSizes()
            storageHandler.postDelayed(this, 1000)
        }
    }}

    companion object {
        val THIS_VERSION = 13
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (main.getBoolean("firstRun", true)){
            externalCacheDir

            editor.putInt("version", THIS_VERSION)
            editor.commit()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(BackupService.BACKUP_START_NOTIFICATION,              /*kotlin*/
                        BackupService.BACKUP_START_NOTIFICATION, NotificationManager.IMPORTANCE_DEFAULT)/*kotlin*/
                channel.setSound(null, null)
                notificationManager.createNotificationChannel(channel)
            }

            startActivity(Intent(this, InitialGuide::class.java))                           /*kotlin*/
            finish()
        }
        else showChangeLog(true)

        backupMain.setOnClickListener {
            val v = View.inflate(this, R.layout.please_wait, null)
            v.waiting_details.visibility = View.GONE
            v.waiting_cancel.visibility = View.GONE

            v.waiting_progress.setText(R.string.checking_permissions)

            try {
                loadingDialog?.dismiss()
            } catch (ignored: Exception){}

            loadingDialog = AlertDialog.Builder(this)
                    .setView(v)
                    .setCancelable(false)
                    .create()

            loadingDialog?.show()

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_BACKUP)
        }

        restoreMain.setOnClickListener {
            startActivity(Intent(this, HowToRestore::class.java))                           /*kotlin*/
        }

        drawerButton.setOnClickListener {
            drawer_layout.openDrawer(Gravity.START)
        }

        learn_sd_card_support.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        learn_sd_card_support.setOnClickListener {
            commonTools.showSdCardSupportDialog()
        }

        navigationDrawer.setNavigationItemSelectedListener(this)

        val cpuAbi = Build.SUPPORTED_ABIS[0]

        if (cpuAbi == "armeabi-v7a" || cpuAbi == "arm64-v8a" || cpuAbi == "x86" || cpuAbi == "x86_64"){
            if (Build.VERSION.SDK_INT > 28 && !main.getBoolean("android_version_warning", false)){
                AlertDialog.Builder(this)
                        .setTitle(R.string.too_fast)
                        .setMessage(R.string.too_fast_desc)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(R.string.dont_show_again) { _, _ ->
                                editor.putBoolean("android_version_warning", true)
                                editor.commit()
                        }
                        .show()
            }
        }
        else {
            backupMain.visibility = View.GONE
            AlertDialog.Builder(this)
                    .setTitle(R.string.unsupported_device)
                    .setMessage(getString(R.string.cpu_arch_is) + "\n" + cpuAbi + "\n\n" + getString(R.string.currently_supported_cpu))
                    .setPositiveButton(R.string.close) { _, _ ->
                        finish()
                    }
                    .setNegativeButton(R.string.contact) { _, _ ->
                        val email = Intent(Intent.ACTION_SENDTO)
                        email.data = Uri.parse("mailto:")
                        email.putExtra(Intent.EXTRA_EMAIL, arrayOf("help.baltiapps@gmail.com"))
                        email.putExtra(Intent.EXTRA_SUBJECT, "Unsupported device")
                        email.putExtra(Intent.EXTRA_TEXT, commonTools.deviceSpecifications)
                        try {
                            startActivity(Intent.createChooser(email, getString(R.string.select_mail)))
                        } catch (e: Exception) {
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setCancelable(false)
                    .show()
        }

        /*val adRequest = AdRequest.Builder().build();
        main_activity_adView.loadAd(adRequest)*/

        refreshStorageSizes()
        storageHandler.post(storageRunnable)
    }

    private fun showChangeLog(onlyLatest: Boolean) {
        val currVer = main.getInt("version", 1)
        val changelog = AlertDialog.Builder(this)

        if (onlyLatest) {
            if (currVer < THIS_VERSION) {
                /*Put only the latest version here*/
                changelog.setTitle(R.string.version_3_0)
                        .setMessage(R.string.version_3_0_content)
                        .setPositiveButton(R.string.close, null)
                        .show()

                editor.putInt("version", THIS_VERSION)
                editor.commit()
            }
        }
        else {
            val padding = 20

            val scrollView = ScrollView(this)
            scrollView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            val allVersions = TextView(this)
            allVersions.setPadding(padding, padding, padding, padding)
            allVersions.text = ""
            allVersions.textSize = 15f

            scrollView.addView(allVersions)

            /*Add increasing versions here*/

            allVersions.append("\n" + getString(R.string.version_3_0) + "\n" + getString(R.string.version_3_0_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_2_1) + "\n" + getString(R.string.version_2_1_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_2_0_1) + "\n" + getString(R.string.version_2_0_1_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_2_0) + "\n" + getString(R.string.version_2_0_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_1_2) + "\n" + getString(R.string.version_1_2_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_1_1_1) + "\n" + getString(R.string.version_1_1_1_only_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_1_1) + "\n" + getString(R.string.version_1_1_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_1_0_5) + "\n" + getString(R.string.version_1_0_5_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_1_0_4) + "\n" + getString(R.string.version_1_0_4_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_1_0_3) + "\n" + getString(R.string.version_1_0_3_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_1_0_2) + "\n" + getString(R.string.version_1_0_2_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_1_0_1) + "\n" + getString(R.string.version_1_0_1_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_1_0) + "\n" + getString(R.string.version_1_0_content) + "\n")

            changelog.setTitle(R.string.changelog)
                    .setView(scrollView)
                    .setPositiveButton(R.string.close, null)
                    .show()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            R.id.about -> {
                val about = AlertDialog.Builder(this)
                val v = layoutInflater.inflate(R.layout.about_app, null)
                about.setView(v)
                        .setTitle(getString(R.string.about))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
            }

            R.id.helpPage -> startActivity(Intent(this, HelpPage::class.java))              /*kotlin*/

            R.id.changelog -> showChangeLog(false)

            R.id.lastLog -> showLog()

            R.id.older_builds -> commonTools.openWeblink("https://www.androidfilehost.com/?w=files&flid=285270")

            R.id.appIntro ->
                startActivity(Intent(this, InitialGuide::class.java)                        /*kotlin*/
                        .putExtra("manual", true))

            R.id.thanks ->
                AlertDialog.Builder(this)
                        .setView(View.inflate(this, R.layout.thanks_layout, null))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()

            R.id.preferences -> startActivity(Intent(this, PreferenceScreen::class.java))   /*kotlin*/

            R.id.rate -> askForRating(true)

            R.id.contact ->
                AlertDialog.Builder(this)
                        .setTitle(R.string.sure_to_mail)
                        .setMessage(R.string.sure_to_mail_exp)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val email = Intent(Intent.ACTION_SENDTO)
                                    .setData(Uri.parse("mailto:"))
                                    .putExtra(Intent.EXTRA_EMAIL, arrayOf("help.baltiapps@gmail.com"))
                                    .putExtra(Intent.EXTRA_SUBJECT, "Bug report for Migrate")
                                    .putExtra(Intent.EXTRA_TEXT, commonTools.deviceSpecifications)

                            try {
                                startActivity(Intent.createChooser(email, getString(R.string.select_mail)))
                            } catch (e: Exception){
                                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .setNeutralButton(R.string.help) {_, _ ->
                            startActivity(Intent(this, HelpPage::class.java))               /*kotlin*/
                        }
                        .show()

            R.id.contact_telegram -> commonTools.openWeblink("https://t.me/migrateApp")

            R.id.xda_thread ->
                commonTools.openWeblink("https://forum.xda-developers.com/android/apps-games/app-migrate-custom-rom-migration-tool-t3862763")

            R.id.otherApps -> otherAppsClickManager()
        }
        drawer_layout.closeDrawer(Gravity.START)

        return true
    }

    private fun showLog() {
        val lView = View.inflate(this, R.layout.last_log_report, null)

        val ad = AlertDialog.Builder(this, R.style.DarkAlert)
                .setTitle(R.string.lastLog)
                .setIcon(R.drawable.ic_log)
                .setView(lView)
                .setNegativeButton(R.string.close, null)
                .create()

        lView.view_progress_log.setOnClickListener {
            val f = File(externalCacheDir, "progressLog.txt")
            if (f.exists())
                startActivity(
                        Intent(this, SimpleLogDisplay::class.java)                          /*kotlin*/
                                .putExtra("head", getString(R.string.progressLog))
                                .putExtra("filePath", f.absolutePath)
                )
            else Toast.makeText(this, R.string.progress_log_does_not_exist, Toast.LENGTH_SHORT).show()
        }

        lView.view_error_log.setOnClickListener {
            val f = File(externalCacheDir, "errorLog.txt")
            if (f.exists())
                startActivity(
                        Intent(this, SimpleLogDisplay::class.java)                          /*kotlin*/
                                .putExtra("head", getString(R.string.errorLog))
                                .putExtra("filePath", f.absolutePath)
                )
            else Toast.makeText(this, R.string.error_log_does_not_exist, Toast.LENGTH_SHORT).show()
        }

        lView.report_logs.setOnClickListener {
            commonTools.reportLogs(false)
            ad.dismiss()
        }

        ad.show()
    }

    private fun askForRating(manual: Boolean) {
        val rateDialog = AlertDialog.Builder(this)
                .setTitle(R.string.rate_dialog_title)
                .setMessage(R.string.rate_dialog_message)
                .setPositiveButton(R.string.sure) { _, _ ->
                    commonTools.openWeblink("market://details?id=balti.migrate")
                    editor.putBoolean("askForRating", false)
                    editor.commit()
                }
        if (!manual) {
            rateDialog.setNeutralButton(R.string.never_show) { _, _ ->
                editor.putBoolean("askForRating", false)
                editor.commit()
                finish()
            }
                    .setNegativeButton(R.string.later) { _, _ ->
                        finish()
                    }
        }
        else rateDialog.setNegativeButton("Cancel", null)

        rateDialog.show()
    }

    private fun otherAppsClickManager() {

        val view = layoutInflater.inflate(R.layout.other_apps, null)
        view.setOnClickListener{
            commonTools.openWeblink(
                    when (it.id) {
                        R.id.motodisplay_handwave -> "market://details?id=sayantanrc.motodisplayhandwave"
                        R.id.opcode_8085 -> "market://details?id=balti.opcode8085"
                        R.id.pickRingStop -> "market://details?id=balti.pickringstop"
                        else -> ""
                    }
            )
        }

        AlertDialog.Builder(this)
                .setView(view)
                .setTitle(R.string.other_apps)
                .setPositiveButton(android.R.string.ok, null)
                .show()

    }

    private fun refreshStorageSizes() {

        var availableKb = 0L
        var fullKb = 0L
        var consumedKb = 0L

        fun calculateStorage(path: String) {
            val statFs = StatFs(path)

            availableKb = (statFs.blockSizeLong * statFs.availableBlocksLong) / 1024
            fullKb = (statFs.blockSizeLong * statFs.blockCountLong) / 1024
            consumedKb = fullKb - availableKb
        }

        calculateStorage(Environment.getExternalStorageDirectory().absolutePath)

        internal_storage_bar.progress = ((consumedKb * 100) / fullKb).toInt()
        internal_storage_text.text = commonTools.getHumanReadableStorageSpace(consumedKb) + "/" +
                        commonTools.getHumanReadableStorageSpace(fullKb)

        var sdCardRoot : File? = null
        val defaultPath = main.getString("defaultBackupPath", DEFAULT_INTERNAL_STORAGE_DIR);

        if (defaultPath != DEFAULT_INTERNAL_STORAGE_DIR && File(defaultPath).canWrite()){
            sdCardRoot = File(defaultPath).parentFile
        }
        else {
            val sdCardPaths = commonTools.sdCardPaths
            if (sdCardPaths.size == 1 && File(sdCardPaths[0]).canWrite()){
                sdCardRoot = File(sdCardPaths[0])
            }
        }

        if (sdCardRoot != null){
            sd_card_storage_use_view.visibility = View.VISIBLE

            calculateStorage(sdCardRoot.absolutePath)

            sd_card_name.text = sdCardRoot.name
            sd_card_storage_bar.progress = ((consumedKb * 100) / fullKb).toInt()
            sd_card_storage_text.text = commonTools.getHumanReadableStorageSpace(consumedKb) + "/" +
                    commonTools.getHumanReadableStorageSpace(fullKb)
        }
        else {
            sd_card_storage_use_view.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()

        if (packageName != "balti.migrate") {
            val ad = AlertDialog.Builder(this)
            ad.setTitle(R.string.copied_app)
            ad.setMessage(R.string.copied_app_exp)
            ad.setCancelable(false)
            ad.setNegativeButton(R.string.close) { _, _ ->
                finish()
            }
            ad.setPositiveButton(R.string.install) { _, _ ->
                commonTools.openWeblink("market://details?id=balti.migrate")
            }
            ad.show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_BACKUP){
            if (grantResults.size == 2 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                if (isRootPermissionGranted()) {

                    if (!main.getBoolean("alternate_access_asked", false)) {

                        editor.putBoolean("alternate_access_asked", true)
                        editor.commit()

                        val accessPermissionDialog = AlertDialog.Builder(this)
                                .setNegativeButton(R.string.old_method_will_be_used) { _, _ ->
                                    editor.putInt("calculating_size_method", 1)
                                    editor.commit()
                                    startActivity(Intent(this, BackupActivity::class.java)) /*kotlin*/
                                }
                                .setNeutralButton(android.R.string.cancel) { _, _ ->
                                    editor.putBoolean("alternate_access_asked", false)
                                    editor.commit()
                                }
                                .setCancelable(false)

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            accessPermissionDialog.setTitle(R.string.use_reflection)
                                    .setMessage(R.string.use_reflection_exp)
                                    .setPositiveButton(R.string.yes) { _, _ ->
                                        editor.putInt("calculating_size_method", 2)
                                        editor.commit()
                                        startActivity(Intent(this, BackupActivity::class.java)) /*kotlin*/
                                    }
                        } else {
                            if (!isUsageAccessGranted()) {
                                accessPermissionDialog.setTitle(R.string.use_usage_access_permission)
                                        .setMessage(R.string.usage_access_permission_needed_desc)
                                        .setPositiveButton(R.string.proceed) { _, _ ->
                                            editor.putBoolean("alternate_access_asked", false)
                                            editor.commit()
                                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                            startActivity(intent)
                                            Toast.makeText(this, R.string.usage_permission_toast, Toast.LENGTH_SHORT).show()
                                        }
                            } else {
                                editor.putBoolean("alternate_access_asked", true)
                                editor.putInt("calculating_size_method", 2)
                                editor.commit()
                                startActivity(Intent(this, BackupActivity::class.java))     /*kotlin*/
                            }
                        }

                        accessPermissionDialog.show()

                    } else {
                        startActivity(Intent(this, BackupActivity::class.java))             /*kotlin*/
                    }
                } else {
                    AlertDialog.Builder(this)
                            .setTitle(R.string.root_permission_denied)
                            .setMessage(getString(R.string.root_permission_denied_desc) + "\n\n" + rootErrorMessage)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                }
            } else {
                AlertDialog.Builder(this)
                        .setMessage(R.string.storage_access_required)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
            }

            try {
                loadingDialog?.dismiss()
            } catch (ignored: Exception) {
            }
        }
        else if (requestCode == REQUEST_CODE_RESTORE) {
            if (grantResults.size == 2 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startActivity(Intent(this, ZipPicker::class.java))                          /*kotlin*/
            } else {
                AlertDialog.Builder(this)
                        .setMessage(R.string.storage_access_required_restore)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
            }
        }
    }

    private fun isRootPermissionGranted(): Boolean {

        return try {
            val r = commonTools.suEcho()
            rootErrorMessage = r[1] as String
            r[0] as Boolean
        } catch (e: Exception) {
            e.printStackTrace()
            rootErrorMessage = e.message.toString()
            false
        }
    }

    private fun isUsageAccessGranted(): Boolean {
        return try {
            val packageManager = packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid, applicationInfo.packageName)

            mode == AppOpsManager.MODE_ALLOWED

        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(Gravity.START))
            drawer_layout.closeDrawer(Gravity.START)
        else if (!main.getBoolean("firstRun", true) && main.getBoolean("askForRating", true))
            askForRating(false)
        else
            super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            loadingDialog?.dismiss()
        } catch (_: Exception) {
        }

        try {
            storageHandler.removeCallbacks(storageRunnable)
        } catch (_: Exception) {
        }

    }
}