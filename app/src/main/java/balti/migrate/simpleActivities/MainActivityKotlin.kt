package balti.migrate.simpleActivities

import android.app.Activity
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Paint
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.R
import balti.migrate.backupActivity.BackupActivityKotlin
import balti.migrate.messages.MessagesView
import balti.migrate.preferences.MainPreferenceActivity
import balti.migrate.storageSelector.StorageSelectorActivity
import balti.migrate.storageSelector.StorageType
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_CANCELLING
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_END
import balti.migrate.utilities.CommonToolsKotlin.Companion.CHANNEL_BACKUP_RUNNING
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_MESSAGE_CONTENT
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_SHOW_FIRST_WARNING
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_ERRORLOG
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_MESSAGES
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_PROGRESSLOG
import balti.migrate.utilities.CommonToolsKotlin.Companion.LAST_SUPPORTED_ANDROID_API
import balti.migrate.utilities.CommonToolsKotlin.Companion.MESSAGE_ACTIVITY_CODE
import balti.migrate.utilities.CommonToolsKotlin.Companion.MESSAGE_BOARD_URL
import balti.migrate.utilities.CommonToolsKotlin.Companion.MESSAGE_FIELD_LAST_UPDATE_NO
import balti.migrate.utilities.CommonToolsKotlin.Companion.MIGRATE_OSS_GITHUB
import balti.migrate.utilities.CommonToolsKotlin.Companion.PACKAGE_MIGRATE_FLASHER
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_ALTERNATE_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_ANDROID_VERSION_WARNING
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_ASK_FOR_RATING
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_CALCULATING_SIZE_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_FIRST_RUN
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_FIRST_STORAGE_REQUEST
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_LAST_MESSAGE_LEVEL
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_LAST_MESSAGE_SNACK_LEVEL
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_STORAGE_TYPE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_TERMINAL_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_VERSION_CURRENT
import balti.migrate.utilities.CommonToolsKotlin.Companion.SIMPLE_LOG_VIEWER_FILEPATH
import balti.migrate.utilities.CommonToolsKotlin.Companion.SIMPLE_LOG_VIEWER_HEAD
import balti.migrate.utilities.CommonToolsKotlin.Companion.TG_DEV_LINK
import balti.migrate.utilities.CommonToolsKotlin.Companion.TG_LINK
import balti.migrate.utilities.CommonToolsKotlin.Companion.THIS_VERSION
import balti.module.baltitoolbox.functions.Misc.doBackgroundTask
import balti.module.baltitoolbox.functions.Misc.getHumanReadableStorageSpace
import balti.module.baltitoolbox.functions.Misc.makeNotificationChannel
import balti.module.baltitoolbox.functions.Misc.openWebLink
import balti.module.baltitoolbox.functions.Misc.playStoreLink
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefInt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefInt
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.last_log_report.view.*
import kotlinx.android.synthetic.main.please_wait.view.*
import kotlinx.android.synthetic.main.restore_by_flasher.view.*
import org.json.JSONObject
import java.net.URL

class MainActivityKotlin : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val commonTools by lazy { CommonToolsKotlin(this) }

    // old code
    //private val REQUEST_CODE_BACKUP = 43

    private var rootErrorMessage = ""
    private var loadingDialog: AlertDialog? = null

    private val storageHandler by lazy { Handler() }
    private val storageRunnable by lazy { object : Runnable {
        override fun run() {
            refreshStorageSizes()
            storageHandler.postDelayed(this, 5000)
            // increasing time because FileX has greater overhead
        }
    }}

    private val REQUEST_CODE_STORAGE_SELECTOR = 47

    private fun startStorageSpaceMonitor(){
        refreshStorageSizes()
        storageHandler.post(storageRunnable)
    }
    private fun stopStorageSpaceMonitor(){
        tryIt { storageHandler.removeCallbacks(storageRunnable) }
    }

    private fun startMessageView(content: String) {
        startActivityForResult(
                Intent(this, MessagesView::class.java)
                        .putExtra(EXTRA_MESSAGE_CONTENT, content)
                , MESSAGE_ACTIVITY_CODE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (getPrefBoolean(PREF_FIRST_RUN, true)){
            externalCacheDir

            putPrefInt(PREF_VERSION_CURRENT, THIS_VERSION, true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                makeNotificationChannel(CHANNEL_BACKUP_RUNNING, CHANNEL_BACKUP_RUNNING, NotificationManager.IMPORTANCE_LOW)
                makeNotificationChannel(CHANNEL_BACKUP_END, CHANNEL_BACKUP_END, NotificationManager.IMPORTANCE_HIGH)
                makeNotificationChannel(CHANNEL_BACKUP_CANCELLING, CHANNEL_BACKUP_CANCELLING, NotificationManager.IMPORTANCE_MIN)
            }

            startActivity(Intent(this, InitialGuideKotlin::class.java))
            finish()
        }
        else showChangeLog(true)

        backupMain.setOnClickListener {
            val v = View.inflate(this, R.layout.please_wait, null)
            v.waiting_progress_subtext.visibility = View.GONE
            v.waiting_cancel.visibility = View.GONE

            v.waiting_progress_text.setText(R.string.checking_permissions)

            tryIt { loadingDialog?.dismiss() }

            loadingDialog = AlertDialog.Builder(this)
                    .setView(v)
                    .setCancelable(false)
                    .create()

            stopStorageSpaceMonitor()

            loadingDialog?.show()

            if (getPrefBoolean(PREF_FIRST_STORAGE_REQUEST, true) || !commonTools.isStorageValid()){
                startActivityForResult(
                        Intent(this, StorageSelectorActivity::class.java),
                        REQUEST_CODE_STORAGE_SELECTOR
                )
            }
            else {
                // storage is ok. request root which also starts the backup activity.
                requestRoot()
            }
        }

        restoreMain.setOnClickListener {
            startActivity(Intent(this, RestoreByTwrp::class.java))
        }

        restoreFlasher.setOnClickListener {
            val v = View.inflate(this, R.layout.restore_by_flasher, null)
            v.get_flasher_button.setOnClickListener {
                playStoreLink(PACKAGE_MIGRATE_FLASHER)
            }
            AlertDialog.Builder(this)
                    .setView(v)
                    .setNegativeButton(R.string.close, null)
                    .show()
        }

        openPreferences.setOnClickListener {
            startActivity(Intent(this, MainPreferenceActivity::class.java))
        }

        drawerButton.setOnClickListener {
            drawer_layout.openDrawer(GravityCompat.START)
        }

        migrate_oss_link.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        migrate_oss_link.setOnClickListener {
            openWebLink(MIGRATE_OSS_GITHUB)
        }

        startStorageSpaceMonitor()

        navigationDrawer.setNavigationItemSelectedListener(this)

        val cpuAbi = Build.SUPPORTED_ABIS[0]

        if (cpuAbi == "armeabi-v7a" || cpuAbi == "arm64-v8a" || cpuAbi == "x86" || cpuAbi == "x86_64"){
            if (Build.VERSION.SDK_INT > LAST_SUPPORTED_ANDROID_API && !getPrefBoolean(PREF_ANDROID_VERSION_WARNING, false)){
                AlertDialog.Builder(this)
                        .setTitle(R.string.too_fast)
                        .setMessage(R.string.too_fast_desc)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(R.string.dont_show_again) { _, _ ->
                                putPrefBoolean(PREF_ANDROID_VERSION_WARNING, value = true, immediate = true)
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
                        openWebLink(TG_LINK)
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

        showFirstRunWarningIfApplicable()

        tryIt {
            val messageFile = FileX.new(filesDir.absolutePath, FILE_MESSAGES, true)
            messages.setImageResource(R.drawable.ic_messages)
            doBackgroundTask({
                tryIt {
                    messageFile.delete()
                    URL(MESSAGE_BOARD_URL).openStream().use { input ->
                        messageFile.outputStream()?.use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }, {
                tryIt {
                    if (messageFile.canRead()) {
                        val strBuf = StringBuffer("")
                        messageFile.readLines().forEach {
                            strBuf.append("$it\n")
                        }
                        val content = strBuf.toString()
                        val json = JSONObject(content)

                        messages.setOnClickListener {
                            startMessageView(content)
                        }

                        val updNo = json.getInt(MESSAGE_FIELD_LAST_UPDATE_NO)
                        if (updNo > getPrefInt(PREF_LAST_MESSAGE_LEVEL, 0)) {
                            messages.setImageResource(R.drawable.ic_messages_active)

                            if (updNo > getPrefInt(PREF_LAST_MESSAGE_SNACK_LEVEL, 0)) {
                                putPrefInt(PREF_LAST_MESSAGE_SNACK_LEVEL, updNo)
                                Snackbar.make(messages, getString(R.string.new_message), Snackbar.LENGTH_SHORT).apply {
                                    setAction(R.string.view) {
                                        startMessageView(content)
                                    }
                                }.show()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun showFirstRunWarningIfApplicable(){
        tryIt {
            if (intent.getBooleanExtra(EXTRA_SHOW_FIRST_WARNING, false)) {
                AlertDialog.Builder(this).apply {
                    setIcon(R.drawable.ic_warning)
                    setTitle(R.string.test_the_app)
                    setMessage(R.string.test_the_app_desc)
                    setPositiveButton(android.R.string.ok, null)
                }
                        .show()
            }
        }
    }

    private fun showChangeLog(onlyLatest: Boolean) {
        val lastVer = getPrefInt(PREF_VERSION_CURRENT, 1)
        val changelog = AlertDialog.Builder(this)

        if (onlyLatest) {
            if (lastVer < THIS_VERSION) {
                /*Put only the latest version here*/
                changelog.setTitle(R.string.version_5_0)
                        .setMessage(R.string.version_5_0_content)
                        .setPositiveButton(R.string.close, null)
                        .show()

                /*Version related changes*/
                if (lastVer < 23 && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {  // v3.0.3, immediate version after strike
                    putPrefInt(PREF_CALCULATING_SIZE_METHOD, PREF_TERMINAL_METHOD)
                }

                putPrefInt(PREF_VERSION_CURRENT, THIS_VERSION, true)
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

            allVersions.append("\n" + getString(R.string.version_5_0) + "\n" + getString(R.string.version_5_0_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_4_0) + "\n" + getString(R.string.version_4_0_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_3_1_1) + "\n" + getString(R.string.version_3_1_1_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_3_1) + "\n" + getString(R.string.version_3_1_content) + "\n")
            allVersions.append("\n" + getString(R.string.version_3_0_3) + "\n" + getString(R.string.version_3_0_3_content) + "\n")
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

            R.id.older_builds -> openWebLink("https://www.androidfilehost.com/?w=files&flid=285270")

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
                            openWebLink(TG_LINK)
                        }
                        .setNeutralButton(android.R.string.cancel, null)
                        .setNegativeButton(R.string.contact_dev) {_, _ ->
                            openWebLink(TG_DEV_LINK)
                        }
                        .show()

            R.id.xda_thread ->
                openWebLink("https://forum.xda-developers.com/android/apps-games/app-migrate-custom-rom-migration-tool-t3862763")

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
            val f = FileX.new(CACHE_DIR, FILE_PROGRESSLOG, true)
            if (f.exists())
                startActivity(
                        Intent(this, SimpleLogViewer::class.java)
                                .putExtra(SIMPLE_LOG_VIEWER_HEAD, getString(R.string.progressLog))
                                .putExtra(SIMPLE_LOG_VIEWER_FILEPATH, f.absolutePath)
                )
            else Toast.makeText(this, R.string.progress_log_does_not_exist, Toast.LENGTH_SHORT).show()
        }

        lView.view_error_log.setOnClickListener {
            val f = FileX.new(CACHE_DIR, FILE_ERRORLOG, true)
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
                    playStoreLink(packageName)
                    putPrefBoolean(PREF_ASK_FOR_RATING, false)
                }
        if (!manual) {
            rateDialog.setNeutralButton(R.string.never_show) { _, _ ->
                putPrefBoolean(PREF_ASK_FOR_RATING, false, immediate = true)
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
            openWebLink(
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

            val rootFile = if (getPrefString(PREF_STORAGE_TYPE, StorageType.CONVENTIONAL.value) in
                    arrayOf(StorageType.CONVENTIONAL.value, StorageType.ALL_FILES_STORAGE.value)){

                FileXInit.setTraditional(true)
                FileX.new(getPrefString(PREF_DEFAULT_BACKUP_PATH, PREF_DEFAULT_BACKUP_PATH))
            }
            else {
                FileXInit.setTraditional(false)
                FileX.new("/")
            }

            if (rootFile.canRead()){
                storage_bar_layout.visibility = View.VISIBLE

                val availableBytes = rootFile.usableSpace
                val fullBytes = rootFile.totalSpace
                val consumedBytes = fullBytes - availableBytes

                storage_bar.progress = ((consumedBytes * 100) / fullBytes).toInt()
                storage_text.text = getHumanReadableStorageSpace(consumedBytes) + "/" +
                        getHumanReadableStorageSpace(fullBytes)
            }
            else {
                storage_bar_layout.visibility = View.GONE
            }
        }
        catch(_: Exception){
            storage_bar_layout.visibility = View.GONE
        }
    }

    private fun dismissLoading(){
        tryIt { loadingDialog?.dismiss() }
    }

    override fun onResume() {
        super.onResume()

        fun resizeButtons(){

            if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) return

            val MAX_WIDTH = 525
            var SPACING: Int
            content_scrollView.viewTreeObserver.run {
                if (isAlive) addOnGlobalLayoutListener (
                        object : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                SPACING = buttonGap.width
                                val buttons = arrayOf(backupMain, restoreMain, restoreFlasher, openPreferences)
                                if (content_scrollView.width > MAX_WIDTH*2 + SPACING){
                                    buttons.forEach {
                                        it.layoutParams = LinearLayout.LayoutParams(MAX_WIDTH, WRAP_CONTENT)
                                    }
                                }
                                else {
                                    val maxWidth = (content_scrollView.width - SPACING)/2
                                    buttons.forEach {
                                        it.layoutParams = LinearLayout.LayoutParams(maxWidth, WRAP_CONTENT)
                                    }
                                }
                                tryIt { content_scrollView.viewTreeObserver.removeOnGlobalLayoutListener(this) }
                            }
                        })
            }
        }

        resizeButtons()

        if (packageName != "balti.migrate") {
            val ad = AlertDialog.Builder(this)
            ad.setTitle(R.string.copied_app)
            ad.setMessage(R.string.copied_app_exp)
            ad.setCancelable(false)
            ad.setNegativeButton(R.string.close) { _, _ ->
                finish()
            }
            ad.setPositiveButton(R.string.install_original_migrate) { _, _ ->
                playStoreLink("balti.migrate")
            }
            ad.show()
        }
    }

    private fun requestRoot(){
        doBackgroundTask({
            return@doBackgroundTask isRootPermissionGranted()
        }, {

            dismissLoading()
            if (it == true) {

                fun startBackupActivity() {
                    startActivity(Intent(this, BackupActivityKotlin::class.java))
                }

                val isOreoAndAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                val isAlternateMethod =
                    getPrefInt(PREF_CALCULATING_SIZE_METHOD, PREF_ALTERNATE_METHOD) == PREF_ALTERNATE_METHOD

                if (isOreoAndAbove && isAlternateMethod && !isUsageAccessGranted()) {

                    val accessPermissionDialog = AlertDialog.Builder(this).apply {
                        setTitle(R.string.use_usage_access_permission)
                        setMessage(getString(R.string.usage_access_permission_needed_desc) + "\n\n" + getString(R.string.usage_access_how))
                        setPositiveButton(R.string.proceed) { _, _ ->
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            startActivity(intent)
                            Toast.makeText(this@MainActivityKotlin, R.string.usage_permission_toast, Toast.LENGTH_SHORT).show()
                        }
                        setNegativeButton(R.string.old_method_will_be_used) { _, _ ->
                            putPrefInt(PREF_CALCULATING_SIZE_METHOD, PREF_TERMINAL_METHOD, true)
                            startBackupActivity()
                        }
                        setNeutralButton(android.R.string.cancel, null)
                        setCancelable(false)
                    }

                    accessPermissionDialog.show()

                } else {

                    if (!isOreoAndAbove){
                        if (isAlternateMethod) {
                            putPrefInt(PREF_CALCULATING_SIZE_METHOD, PREF_TERMINAL_METHOD)
                        }
                    }

                    startBackupActivity()
                }

            } else {
                AlertDialog.Builder(this)
                        .setTitle(R.string.root_permission_denied)
                        .setMessage(getString(R.string.root_permission_denied_desc) + "\n\n" + rootErrorMessage)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
            }
        })
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MESSAGE_ACTIVITY_CODE) messages.setImageResource(R.drawable.ic_messages)
        else if (requestCode == REQUEST_CODE_STORAGE_SELECTOR) {
            if (resultCode == Activity.RESULT_OK){
                putPrefBoolean(PREF_FIRST_STORAGE_REQUEST, false)
                requestRoot()
            }
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START))
            drawer_layout.closeDrawer(GravityCompat.START)
        else if (getPrefBoolean(PREF_FIRST_RUN, true) && getPrefBoolean(PREF_ASK_FOR_RATING, true))
            askForRating(false)
        else
            super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        tryIt { loadingDialog?.dismiss() }
        stopStorageSpaceMonitor()
    }
}