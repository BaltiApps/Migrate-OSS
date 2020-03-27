package balti.migrate.simpleActivities

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import balti.migrate.R
import balti.migrate.backupActivity.BackupActivityKotlin
import balti.migrate.preferences.MainPreferenceActivity
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_CANCELLING
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_END
import balti.migrate.utilities.CommonToolKotlin.Companion.CHANNEL_BACKUP_RUNNING
import balti.migrate.utilities.CommonToolKotlin.Companion.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_ERRORLOG
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_MAIN_PREF
import balti.migrate.utilities.CommonToolKotlin.Companion.FILE_PROGRESSLOG
import balti.migrate.utilities.CommonToolKotlin.Companion.LAST_SUPPORTED_ANDROID_API
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_ALTERNATE_ACCESS_ASKED
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_ALTERNATE_METHOD
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_ANDROID_VERSION_WARNING
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_ASK_FOR_RATING
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_CALCULATING_SIZE_METHOD
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_FIRST_RUN
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_TERMINAL_METHOD
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_VERSION_CURRENT
import balti.migrate.utilities.CommonToolKotlin.Companion.SIMPLE_LOG_VIEWER_FILEPATH
import balti.migrate.utilities.CommonToolKotlin.Companion.SIMPLE_LOG_VIEWER_HEAD
import balti.migrate.utilities.CommonToolKotlin.Companion.TG_DEV_LINK
import balti.migrate.utilities.CommonToolKotlin.Companion.TG_LINK
import balti.migrate.utilities.CommonToolKotlin.Companion.THIS_VERSION
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.last_log_report.view.*
import kotlinx.android.synthetic.main.please_wait.view.*
import java.io.File

class MainActivityKotlin : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val main : SharedPreferences by lazy { getSharedPreferences(FILE_MAIN_PREF, Context.MODE_PRIVATE) }
    private val editor : SharedPreferences.Editor by lazy { main.edit() }
    private val commonTools by lazy { CommonToolKotlin(this) }

    private val REQUEST_CODE_BACKUP = 43

    private var rootErrorMessage = ""
    private var loadingDialog: AlertDialog? = null

    private val storageHandler by lazy { Handler() }
    private val storageRunnable by lazy { object : Runnable {
        override fun run() {
            refreshStorageSizes()
            storageHandler.postDelayed(this, 1000)
        }
    }}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (main.getBoolean(PREF_FIRST_RUN, true)){
            externalCacheDir

            editor.putInt(PREF_VERSION_CURRENT, THIS_VERSION)
            editor.commit()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                commonTools.makeNotificationChannel(CHANNEL_BACKUP_RUNNING, CHANNEL_BACKUP_RUNNING, NotificationManager.IMPORTANCE_LOW)
                commonTools.makeNotificationChannel(CHANNEL_BACKUP_END, CHANNEL_BACKUP_END, NotificationManager.IMPORTANCE_HIGH)
                commonTools.makeNotificationChannel(CHANNEL_BACKUP_CANCELLING, CHANNEL_BACKUP_CANCELLING, NotificationManager.IMPORTANCE_MIN)
            }

            startActivity(Intent(this, InitialGuideKotlin::class.java))
            finish()
        }
        else showChangeLog(true)

        backupMain.setOnClickListener {
            val v = View.inflate(this, R.layout.please_wait, null)
            v.waiting_details.visibility = View.GONE
            v.waiting_cancel.visibility = View.GONE

            v.waiting_progress.setText(R.string.checking_permissions)

            commonTools.tryIt { loadingDialog?.dismiss() }

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
            startActivity(Intent(this, RestoreByTwrp::class.java))
        }

        openPreferences.setOnClickListener {
            startActivity(Intent(this, MainPreferenceActivity::class.java))
        }

        drawerButton.setOnClickListener {
            drawer_layout.openDrawer(GravityCompat.START)
        }

        learn_sd_card_support.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        learn_sd_card_support.setOnClickListener {
            commonTools.showSdCardSupportDialog()
        }

        navigationDrawer.setNavigationItemSelectedListener(this)

        val cpuAbi = Build.SUPPORTED_ABIS[0]

        if (cpuAbi == "armeabi-v7a" || cpuAbi == "arm64-v8a" || cpuAbi == "x86" || cpuAbi == "x86_64"){
            if (Build.VERSION.SDK_INT > LAST_SUPPORTED_ANDROID_API && !main.getBoolean(PREF_ANDROID_VERSION_WARNING, false)){
                AlertDialog.Builder(this)
                        .setTitle(R.string.too_fast)
                        .setMessage(R.string.too_fast_desc)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(R.string.dont_show_again) { _, _ ->
                                editor.putBoolean(PREF_ANDROID_VERSION_WARNING, true)
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
                    .setNegativeButton(R.string.close) { _, _ ->
                        finish()
                    }
                    .setPositiveButton(R.string.contact) { _, _ ->
                        commonTools.openWebLink(TG_LINK)
                    }
                    .setNeutralButton(R.string.use_email_instead) {_, _ ->
                        val email = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("help.baltiapps@gmail.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "Unsupported device.")
                            putExtra(Intent.EXTRA_TEXT, commonTools.deviceSpecifications)
                        }
                        try {
                            startActivity(Intent.createChooser(email, getString(R.string.select_telegram)))
                        } catch (e: Exception) {
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setCancelable(false)
                    .show()
        }

        refreshStorageSizes()
        storageHandler.post(storageRunnable)

    }

    private fun showChangeLog(onlyLatest: Boolean) {
        val currVer = main.getInt(PREF_VERSION_CURRENT, 1)
        val changelog = AlertDialog.Builder(this)

        if (onlyLatest) {
            if (currVer < THIS_VERSION) {
                /*Put only the latest version here*/
                changelog.setTitle(R.string.version_3_0_1)
                        .setMessage(R.string.version_3_0_1_content)
                        .setPositiveButton(R.string.close, null)
                        .show()

                editor.putInt(PREF_VERSION_CURRENT, THIS_VERSION)
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

            allVersions.append("\n" + getString(R.string.version_3_0_1) + "\n" + getString(R.string.version_3_0_1_content) + "\n")
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

            R.id.helpPage -> startActivity(Intent(this, HelpPageKotlin::class.java))

            R.id.privacyPolicy -> startActivity(Intent(this, PrivacyPolicy::class.java))

            R.id.changelog -> showChangeLog(false)

            R.id.lastLog -> showLog()

            R.id.older_builds -> commonTools.openWebLink("https://www.androidfilehost.com/?w=files&flid=285270")

            R.id.appIntro ->
                startActivity(Intent(this, InitialGuideKotlin::class.java))

            R.id.translate ->
                AlertDialog.Builder(this)
                        .setView(View.inflate(this, R.layout.translation_layout, null))
                        .setPositiveButton(R.string.close, null)
                        .show()

            R.id.contributors ->
                AlertDialog.Builder(this)
                        .setView(View.inflate(this, R.layout.contributors, null))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()

            R.id.rate -> askForRating(true)

            R.id.contact ->
                AlertDialog.Builder(this)
                        .setTitle(R.string.contact_via_telegram)
                        .setMessage(R.string.contact_via_telegram_desc)
                        .setPositiveButton(R.string.post_in_group) { _, _ ->
                            commonTools.openWebLink(TG_LINK)
                        }
                        .setNeutralButton(android.R.string.cancel, null)
                        .setNegativeButton(R.string.contact_dev) {_, _ ->
                            commonTools.openWebLink(TG_DEV_LINK)
                        }
                        .show()

            R.id.xda_thread ->
                commonTools.openWebLink("https://forum.xda-developers.com/android/apps-games/app-migrate-custom-rom-migration-tool-t3862763")

            R.id.otherApps -> otherAppsClickManager()
        }
        drawer_layout.closeDrawer(GravityCompat.START)

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
            val f = File(externalCacheDir, FILE_PROGRESSLOG)
            if (f.exists())
                startActivity(
                        Intent(this, SimpleLogViewer::class.java)
                                .putExtra(SIMPLE_LOG_VIEWER_HEAD, getString(R.string.progressLog))
                                .putExtra(SIMPLE_LOG_VIEWER_FILEPATH, f.absolutePath)
                )
            else Toast.makeText(this, R.string.progress_log_does_not_exist, Toast.LENGTH_SHORT).show()
        }

        lView.view_error_log.setOnClickListener {
            val f = File(externalCacheDir, FILE_ERRORLOG)
            if (f.exists())
                startActivity(
                        Intent(this, SimpleLogViewer::class.java)
                                .putExtra(SIMPLE_LOG_VIEWER_HEAD, getString(R.string.errorLog))
                                .putExtra(SIMPLE_LOG_VIEWER_FILEPATH, f.absolutePath)
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
                    commonTools.playStoreLink(packageName)
                    editor.putBoolean(PREF_ASK_FOR_RATING, false)
                    editor.commit()
                }
        if (!manual) {
            rateDialog.setNeutralButton(R.string.never_show) { _, _ ->
                editor.putBoolean(PREF_ASK_FOR_RATING, false)
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
            commonTools.openWebLink(
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

        try {

            var availableBytes = 0L
            var fullBytes = 0L
            var consumedBytes = 0L

            fun calculateStorage(path: String) {
                val statFs = StatFs(path)

                availableBytes = (statFs.blockSizeLong * statFs.availableBlocksLong)
                fullBytes = (statFs.blockSizeLong * statFs.blockCountLong)
                consumedBytes = fullBytes - availableBytes
            }

            calculateStorage(Environment.getExternalStorageDirectory().absolutePath)

            internal_storage_bar.progress = ((consumedBytes * 100) / fullBytes).toInt()
            internal_storage_text.text = commonTools.getHumanReadableStorageSpace(consumedBytes) + "/" +
                    commonTools.getHumanReadableStorageSpace(fullBytes)

            var sdCardRoot: File? = null
            val defaultPath = main.getString(PREF_DEFAULT_BACKUP_PATH, DEFAULT_INTERNAL_STORAGE_DIR)

            if (defaultPath != DEFAULT_INTERNAL_STORAGE_DIR && File(defaultPath).canWrite()) {
                sdCardRoot = File(defaultPath).parentFile
            } else {
                val sdCardPaths = commonTools.getSdCardPaths()
                if (sdCardPaths.size == 1 && File(sdCardPaths[0]).canWrite()) {
                    sdCardRoot = File(sdCardPaths[0])
                }
            }

            if (sdCardRoot != null) {
                sd_card_storage_use_view.visibility = View.VISIBLE

                calculateStorage(sdCardRoot.absolutePath)

                sd_card_name.text = sdCardRoot.name
                sd_card_storage_bar.progress = ((consumedBytes * 100) / fullBytes).toInt()
                sd_card_storage_text.text = commonTools.getHumanReadableStorageSpace(consumedBytes) + "/" +
                        commonTools.getHumanReadableStorageSpace(fullBytes)
            } else {
                sd_card_storage_use_view.visibility = View.GONE
            }
        }
        catch(_: Exception){
            internal_storage_use_view.visibility = View.GONE
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
            ad.setPositiveButton(R.string.install_original_migrate) { _, _ ->
                commonTools.playStoreLink("balti.migrate")
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

                    val isAboveOreo = Build.VERSION.SDK_INT > Build.VERSION_CODES.O
                    val isAlternatePermission = main.getInt(PREF_CALCULATING_SIZE_METHOD, PREF_ALTERNATE_METHOD) == PREF_ALTERNATE_METHOD

                    if (!main.getBoolean(PREF_ALTERNATE_ACCESS_ASKED, false) || (isAboveOreo && isAlternatePermission && !isUsageAccessGranted())) {

                        val accessPermissionDialog = AlertDialog.Builder(this)
                                .setNegativeButton(R.string.old_method_will_be_used) { _, _ ->
                                    editor.putBoolean(PREF_ALTERNATE_ACCESS_ASKED, true)
                                    editor.putInt(PREF_CALCULATING_SIZE_METHOD, PREF_TERMINAL_METHOD)
                                    editor.commit()
                                    startActivity(Intent(this, BackupActivityKotlin::class.java))
                                }
                                .setNeutralButton(android.R.string.cancel) { _, _ ->
                                    editor.putBoolean(PREF_ALTERNATE_ACCESS_ASKED, false)
                                    editor.commit()
                                }
                                .setCancelable(false)

                        if (!isAboveOreo) {
                            accessPermissionDialog.setTitle(R.string.use_reflection)
                                    .setMessage(R.string.use_reflection_exp)
                                    .setPositiveButton(R.string.yes) { _, _ ->
                                        editor.putBoolean(PREF_ALTERNATE_ACCESS_ASKED, true)
                                        editor.putInt(PREF_CALCULATING_SIZE_METHOD, PREF_ALTERNATE_METHOD)
                                        editor.commit()
                                        startActivity(Intent(this, BackupActivityKotlin::class.java))
                                    }

                            accessPermissionDialog.show()

                        }
                        else
                        {
                            if (!isUsageAccessGranted()) {
                                accessPermissionDialog.setTitle(R.string.use_usage_access_permission)
                                        .setMessage(R.string.usage_access_permission_needed_desc)
                                        .setPositiveButton(R.string.proceed) { _, _ ->
                                            editor.putBoolean(PREF_ALTERNATE_ACCESS_ASKED, false)
                                            editor.commit()
                                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                            startActivity(intent)
                                            Toast.makeText(this, R.string.usage_permission_toast, Toast.LENGTH_SHORT).show()
                                        }

                                accessPermissionDialog.show()

                            } else {
                                editor.putBoolean(PREF_ALTERNATE_ACCESS_ASKED, true)
                                editor.putInt(PREF_CALCULATING_SIZE_METHOD, PREF_ALTERNATE_METHOD)
                                editor.commit()
                                startActivity(Intent(this, BackupActivityKotlin::class.java))
                            }
                        }

                    } else {
                        startActivity(Intent(this, BackupActivityKotlin::class.java))
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

            commonTools.tryIt { loadingDialog?.dismiss() }
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
        if (drawer_layout.isDrawerOpen(GravityCompat.START))
            drawer_layout.closeDrawer(GravityCompat.START)
        else if (!main.getBoolean(PREF_FIRST_RUN, true) && main.getBoolean(PREF_ASK_FOR_RATING, true))
            askForRating(false)
        else
            super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        commonTools.tryIt { loadingDialog?.dismiss() }
        commonTools.tryIt { storageHandler.removeCallbacks(storageRunnable) }
    }
}