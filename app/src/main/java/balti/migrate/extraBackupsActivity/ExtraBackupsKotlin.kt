package balti.migrate.extraBackupsActivity

//import balti.migrate.AppInstance.Companion.appBackupDataPackets
import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import balti.migrate.AppInstance.Companion.adbState
import balti.migrate.AppInstance.Companion.appBackupDataPackets
import balti.migrate.AppInstance.Companion.appPackets
import balti.migrate.AppInstance.Companion.callsList
import balti.migrate.AppInstance.Companion.contactsList
import balti.migrate.AppInstance.Companion.doBackupInstallers
import balti.migrate.AppInstance.Companion.dpiText
import balti.migrate.AppInstance.Companion.fontScale
import balti.migrate.AppInstance.Companion.keyboardText
import balti.migrate.AppInstance.Companion.selectedBackupDataPackets
import balti.migrate.AppInstance.Companion.smsList
import balti.migrate.AppInstance.Companion.wifiData
import balti.migrate.R
import balti.migrate.backupActivity.BackupActivityKotlin
import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.extraBackupsActivity.adb.ReadAdbKotlin
import balti.migrate.extraBackupsActivity.apps.MakeAppPackets
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.extraBackupsActivity.calls.LoadCallsForSelectionKotlin
import balti.migrate.extraBackupsActivity.calls.ReadCallsKotlin
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.contacts.LoadContactsForSelectionKotlin
import balti.migrate.extraBackupsActivity.contacts.ReadContactsKotlin
import balti.migrate.extraBackupsActivity.contacts.containers.ContactsDataPacketKotlin
import balti.migrate.extraBackupsActivity.dpi.ReadDpiKotlin
import balti.migrate.extraBackupsActivity.fontScale.ReadFontScaleKotlin
import balti.migrate.extraBackupsActivity.installer.LoadInstallersForSelection
import balti.migrate.extraBackupsActivity.keyboard.LoadKeyboardForSelection
import balti.migrate.extraBackupsActivity.sms.LoadSmsForSelectionKotlin
import balti.migrate.extraBackupsActivity.sms.ReadSmsKotlin
import balti.migrate.extraBackupsActivity.sms.containers.SmsDataPacketKotlin
import balti.migrate.extraBackupsActivity.utils.OnJobCompletion
import balti.migrate.extraBackupsActivity.wifi.ReadWifiKotlin
import balti.migrate.extraBackupsActivity.wifi.containers.WifiDataPacket
import balti.migrate.simpleActivities.ProgressShowActivity
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_REQUEST_BACKUP_DATA
import balti.migrate.utilities.CommonToolsKotlin.Companion.CALLS_PERMISSION
import balti.migrate.utilities.CommonToolsKotlin.Companion.CONTACT_PERMISSION
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_DESTINATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FLASHER_ONLY
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_LOAD_CALLS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_LOAD_CONTACTS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_LOAD_INSTALLERS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_LOAD_KEYBOARDS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_LOAD_SMS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_MAKE_APP_PACKETS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_ADB
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_CALLS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_CONTACTS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_DPI
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_FONTSCALE
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_SMS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_SMS_THEN_CALLS
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_WIFI
import balti.migrate.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_FDROID
import balti.migrate.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_PLAY_STORE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_AUTOSELECT_EXTRAS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_ADB
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_CALLS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_DPI
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_FONTSCALE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_INSTALLERS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_SMS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_APP_CACHE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SHOW_FLASHER_ONLY_WARNING
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SHOW_STOCK_WARNING
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_USE_FLASHER_ONLY
import balti.migrate.utilities.CommonToolsKotlin.Companion.SMS_AND_CALLS_PERMISSION
import balti.migrate.utilities.CommonToolsKotlin.Companion.SMS_PERMISSION
import balti.module.baltitoolbox.functions.Misc.playStoreLink
import balti.module.baltitoolbox.functions.Misc.showErrorDialog
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefString
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import kotlinx.android.synthetic.main.ask_for_backup_name.view.*
import kotlinx.android.synthetic.main.extra_backups.*
import kotlinx.android.synthetic.main.flasher_only_warning.view.*
import kotlinx.android.synthetic.main.please_wait.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExtraBackupsKotlin : AppCompatActivity(), OnJobCompletion, CompoundButton.OnCheckedChangeListener {

    // to add extras search for "extras_markers" and change those lines/functions

    private val commonTools by lazy { CommonToolsKotlin(this) }
    //private val appListCopied by lazy { ArrayList<BackupDataPacketKotlin>(0) }

    private lateinit var destination: String
    private var backupName = ""
    private var isAllAppsSelected = true

    private var readContacts: ReadContactsKotlin? = null                    //extras_markers
    private var readSms: ReadSmsKotlin? = null
    private var readCalls: ReadCallsKotlin? = null
    private var readDpi: ReadDpiKotlin? = null
    private var readAdb: ReadAdbKotlin? = null
    private var readWifi: ReadWifiKotlin? = null
    private var readFontScale: ReadFontScaleKotlin? = null

    private var makeAppPackets: MakeAppPackets? = null

    private var loadKeyboard: LoadKeyboardForSelection? = null
    private var loadInstallers: LoadInstallersForSelection? = null

    private var flasherOnlyBackup = false

    private val dialogView by lazy { View.inflate(this, R.layout.please_wait, null) }
    private val waitingDialog by lazy {
        AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
    }

    private val progressReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                startActivity(Intent(this@ExtraBackupsKotlin, ProgressShowActivity::class.java)   /*kotlin*/
                        .apply {
                            intent?.let {
                                this.putExtras(it)
                                this.action = it.action
                            }
                        }
                )
                tryIt { commonTools.LBM?.unregisterReceiver(this) }
                finish()
            }
        }
    }

    private fun toggleBackupButton(v: Int = -1){
        if (v == 0){
            backupButtonWaiting.visibility = View.VISIBLE
            startBackupButton.visibility = View.GONE
        }
        else {
            backupButtonWaiting.visibility = View.GONE
            startBackupButton.visibility = View.VISIBLE
        }
    }

    private fun doWaitingJob(job: () -> Unit){
        toggleBackupButton(0)
        job()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.extra_backups)

        destination = getPrefString(PREF_DEFAULT_BACKUP_PATH, DEFAULT_INTERNAL_STORAGE_DIR)

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_BACKUP_PROGRESS))
        commonTools.LBM?.sendBroadcast(Intent(ACTION_REQUEST_BACKUP_DATA))

        // Disable wifi backup for now because it is unstable.
        wifi_main_item.visibility = View.GONE
        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
        //    wifi_main_item.visibility = View.GONE

        startBackupButton.setOnClickListener {

            val allReadTasks = arrayOf(
                    readContacts,
                    readSms,
                    readCalls,
                    readDpi,
                    readAdb,
                    readWifi,
                    readFontScale
            )

            var isAnyRunning = false
            for (t in allReadTasks.indices){
                allReadTasks[t]?.let {
                    if (it.status == AsyncCoroutineTask.RUNNING) isAnyRunning = true
                }
                if (isAnyRunning) break
            }

            if (!isAnyRunning) {
                if (selectedBackupDataPackets.isNotEmpty() || do_backup_contacts.isChecked || do_backup_calls.isChecked
                        || do_backup_sms.isChecked || do_backup_dpi.isChecked || do_backup_keyboard.isChecked
                        || do_backup_adb.isChecked || do_backup_wifi.isChecked || do_backup_fontScale.isChecked) {                  //extras_markers
                    askForName()
                }
            } else {
                AlertDialog.Builder(this)
                        .setTitle(R.string.wait_while_reading_data)
                        .setMessage(R.string.wait_while_reading_data_desc)
                        .setPositiveButton(R.string.close, null)
                        .show()
            }
        }

        do_backup_contacts.setOnCheckedChangeListener(this)         //extras_markers
        do_backup_sms.setOnCheckedChangeListener(this)
        do_backup_calls.setOnCheckedChangeListener(this)
        do_backup_dpi.setOnCheckedChangeListener(this)
        do_backup_keyboard.setOnCheckedChangeListener(this)
        do_backup_installers.setOnCheckedChangeListener(this)
        do_backup_adb.setOnCheckedChangeListener(this)
        do_backup_wifi.setOnCheckedChangeListener(this)
        do_backup_fontScale.setOnCheckedChangeListener(this)

        extraBackupsBackButton.setOnClickListener {
            startActivity(Intent(this, BackupActivityKotlin::class.java))
            finish()
        }

        extra_backups_help.setOnClickListener {
            AlertDialog.Builder(this)
                    .setMessage(R.string.extra_backups_help)
                    .setPositiveButton(R.string.close, null)
                    .show()
        }

        if (getPrefBoolean(PREF_AUTOSELECT_EXTRAS, true)) {        //extras_markers
            val isSmsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED && getPrefBoolean(PREF_BACKUP_SMS, false)
            val isCallsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED && getPrefBoolean(PREF_BACKUP_CALLS, false)
            val isSmsAndCallsGranted = isSmsGranted && isCallsGranted

            when {
                isSmsAndCallsGranted ->
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS,
                            Manifest.permission.READ_CALL_LOG), SMS_AND_CALLS_PERMISSION)
                isCallsGranted -> do_backup_calls.isChecked = true
                isSmsGranted -> do_backup_sms.isChecked = true
            }
            do_backup_installers.isChecked = getPrefBoolean(PREF_BACKUP_INSTALLERS, true)
            if (!getPrefBoolean(PREF_SHOW_STOCK_WARNING, true)){
                do_backup_adb.isChecked = getPrefBoolean(PREF_BACKUP_ADB, false)
                do_backup_dpi.isChecked = getPrefBoolean(PREF_BACKUP_DPI, false)
                do_backup_fontScale.isChecked = getPrefBoolean(PREF_BACKUP_FONTSCALE, false)
            }
        }

        if (selectedBackupDataPackets.isEmpty())
            no_app_selected_label.text = getString(R.string.no_app_selected)
        else no_app_selected_label.text = getString(R.string.apps_selected) + " ${selectedBackupDataPackets.size}/${appBackupDataPackets.size}"
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {        //extras_markers

        fun showStockWarning(fPositive:() -> Unit, fNegative:() -> Unit){
            if (getPrefBoolean(PREF_SHOW_STOCK_WARNING, true)){
                AlertDialog.Builder(this)
                        .setTitle(R.string.stock_android_title)
                        .setMessage(R.string.stock_android_desc)
                        .setPositiveButton(R.string.go_ahead) {_, _ ->
                            fPositive()
                        }
                        .setNegativeButton(android.R.string.cancel) {_,_ ->
                            fNegative()
                        }
                        .setNeutralButton(R.string.dont_show_stock_warning){_, _ ->
                            putPrefBoolean(PREF_SHOW_STOCK_WARNING, false, immediate = true)
                            fPositive()
                        }
                        .setCancelable(false)
                        .show()
            }
            else {
                fPositive()
            }
        }

        fun deselectExtra(dataContainer: ArrayList<*>?, viewMainItem: View, selectedStatusView: View,
                          readTask: AsyncCoroutineTask?, progressBar: ProgressBar? = null, sal: View? = null) {    //sal: Stock Android recommended label
            dataContainer?.clear()
            viewMainItem.isClickable = false
            progressBar?.visibility = View.GONE
            selectedStatusView.visibility = View.GONE
            sal?.visibility = View.VISIBLE
            tryIt { readTask?.cancel() }
        }

        if (!isChecked) toggleBackupButton(1)

        if (buttonView == do_backup_contacts){

            if (isChecked) {
                AlertDialog.Builder(this)
                        .setTitle(R.string.not_recommended)
                        .setMessage(getText(R.string.contacts_not_recommended))
                        .setPositiveButton(R.string.dont_backup) { _, _ ->
                            do_backup_contacts.isChecked = false
                        }
                        .setNegativeButton(R.string.backup_contacts_anyway) {_, _ ->
                                ActivityCompat.requestPermissions(this,
                                        arrayOf(Manifest.permission.READ_CONTACTS),
                                        CONTACT_PERMISSION)

                            }
                        .setCancelable(false)
                        .show()
            }
            else deselectExtra(contactsList, contacts_main_item, contacts_selected_status, readContacts, contacts_read_progress)

        } else if (buttonView == do_backup_sms) {

            if (isChecked) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_SMS),
                        SMS_PERMISSION)

            } else deselectExtra(smsList, sms_main_item, sms_selected_status, readSms, sms_read_progress)

            putPrefBoolean(PREF_BACKUP_SMS, isChecked)

        } else if (buttonView == do_backup_calls) {

            if (isChecked) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_CALL_LOG),
                        CALLS_PERMISSION)
            } else deselectExtra(callsList, calls_main_item, calls_selected_status, readCalls, calls_read_progress)

            putPrefBoolean(PREF_BACKUP_CALLS, isChecked)

        } else if (buttonView == do_backup_dpi) {
            if (isChecked) {

                showStockWarning({
                    doWaitingJob {
                        readDpi = ReadDpiKotlin(JOBCODE_READ_DPI, this, dpi_main_item, dpi_selected_status, dpi_read_progress, do_backup_dpi)
                        readDpi?.execute()
                    }
                }, {
                    do_backup_dpi.isChecked = false
                })
                sal_dpi.visibility = View.GONE

            } else {
                dpiText = null
                deselectExtra(null, dpi_main_item, dpi_selected_status, readDpi, dpi_read_progress, sal_dpi)
            }

            putPrefBoolean(PREF_BACKUP_DPI, isChecked)

        } else if (buttonView == do_backup_adb) {
            if (isChecked) {

                showStockWarning({
                    doWaitingJob{
                        readAdb = ReadAdbKotlin(JOBCODE_READ_ADB, this, adb_main_item, adb_selected_status, adb_read_progress, do_backup_adb)
                        readAdb?.execute()
                    }
                }, {
                    do_backup_adb.isChecked = false
                })
                sal_adb.visibility = View.GONE

            } else {
                adbState = null
                deselectExtra(null, adb_main_item, adb_selected_status, readAdb, adb_read_progress, sal_adb)
            }

            putPrefBoolean(PREF_BACKUP_ADB, isChecked)

        } else if (buttonView == do_backup_keyboard) {

            if (isChecked) {
                doWaitingJob {
                    loadKeyboard = LoadKeyboardForSelection(JOBCODE_LOAD_KEYBOARDS, this, keyboard_main_item, keyboard_selected_status, do_backup_keyboard)
                    loadKeyboard?.execute()
                }
            }
            else {
                keyboardText = null
                deselectExtra(null, keyboard_main_item, keyboard_selected_status, loadKeyboard)
            }

        } else if (buttonView == do_backup_installers){
            if (isChecked){
                updateInstallers(selectedBackupDataPackets, true)
            }
            else deselectExtra(null, installers_main_item, installer_selected_status, loadInstallers)

            doBackupInstallers = isChecked
            putPrefBoolean(PREF_BACKUP_INSTALLERS, isChecked)

        } else if (buttonView == do_backup_wifi){
            if (isChecked) {

                showStockWarning({
                    doWaitingJob {
                        readWifi = ReadWifiKotlin(JOBCODE_READ_WIFI, this, wifi_main_item, wifi_selected_status, wifi_read_progress, do_backup_wifi)
                        readWifi?.let { it.execute() }
                    }
                }, {
                    do_backup_wifi.isChecked = false
                })
                sal_wifi.visibility = View.GONE

            } else {
                wifiData = null
                deselectExtra(null, wifi_main_item, wifi_selected_status, readWifi, wifi_read_progress, sal_wifi)
            }
        } else if (buttonView == do_backup_fontScale) {
            if (isChecked){

                showStockWarning({
                    doWaitingJob {
                        readFontScale = ReadFontScaleKotlin(JOBCODE_READ_FONTSCALE, this, fontScale_main_item, fontScale_selected_status, fontScale_read_progress, do_backup_fontScale)
                        readFontScale?.let { it.execute() }
                    }
                }, {
                    do_backup_fontScale.isChecked = false
                })
                sal_fontScale.visibility = View.GONE

            } else {
                fontScale = null
                deselectExtra(null, fontScale_main_item, fontScale_selected_status, readFontScale, fontScale_read_progress, sal_fontScale)
            }

            putPrefBoolean(PREF_BACKUP_FONTSCALE, isChecked)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {     //extras_markers
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SMS_AND_CALLS_PERMISSION){

            if (grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                do_backup_sms.setOnCheckedChangeListener(null)
                do_backup_sms.isChecked = true

                tryIt({
                    doWaitingJob {
                        readSms = ReadSmsKotlin(JOBCODE_READ_SMS_THEN_CALLS, this, sms_main_item, sms_selected_status, sms_read_progress, do_backup_sms)
                        readSms?.execute()
                    }
                }, true)

                do_backup_sms.setOnCheckedChangeListener(this)
            }
        }
        else if (requestCode == CONTACT_PERMISSION){

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                tryIt({
                    doWaitingJob {
                        readContacts = ReadContactsKotlin(JOBCODE_READ_CONTACTS, this, contacts_main_item, contacts_selected_status, contacts_read_progress, do_backup_contacts)
                        readContacts?.execute()
                    }
                }, true)

            }
            else {
                Toast.makeText(this, R.string.contacts_access_needed, Toast.LENGTH_SHORT).show()
                do_backup_contacts.isChecked = false
            }

        }
        else if (requestCode == SMS_PERMISSION){

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                tryIt({
                    doWaitingJob {
                        readSms = ReadSmsKotlin(JOBCODE_READ_SMS, this, sms_main_item, sms_selected_status, sms_read_progress, do_backup_sms)
                        readSms?.execute()
                    }
                }, true)

            }
            else {
                Toast.makeText(this, R.string.sms_access_needed, Toast.LENGTH_SHORT).show()
                do_backup_sms.isChecked = false
            }

        }
        else if (requestCode == CALLS_PERMISSION){

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                tryIt({
                    doWaitingJob {
                        readCalls = ReadCallsKotlin(JOBCODE_READ_CALLS, this, calls_main_item, calls_selected_status, calls_read_progress, do_backup_calls)
                        readCalls?.execute()
                    }
                }, true)

            }
            else {
                Toast.makeText(this, R.string.calls_access_needed, Toast.LENGTH_SHORT).show()
                do_backup_calls.isChecked = false
            }

        }
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun askForName() {

        val mainView = View.inflate(this, R.layout.ask_for_backup_name, null)

        mainView.backup_name_edit_text.setSingleLine(true)
        mainView.destination_name.text = destination

        fun showFlasherOnlyWarning(fromPreference: Boolean){

            val flasherWarning = View.inflate(this, R.layout.flasher_only_warning, null)
            flasherWarning.get_flasher_button.setOnClickListener {
                playStoreLink("balti.migrate.flasher")
            }

            val ad = AlertDialog.Builder(this).apply {
                setView(flasherWarning)
                setIcon(R.drawable.ic_error)
            }

            if (fromPreference){
                flasherWarning.flasher_warning_checkbox.visibility = View.VISIBLE
                ad.apply {
                    setPositiveButton(R.string.proceed) {_, _ ->
                        putPrefBoolean(PREF_SHOW_FLASHER_ONLY_WARNING, !flasherWarning.flasher_warning_checkbox.isChecked)
                    }
                    setNegativeButton(R.string.disable_this) { _, _ ->
                        mainView.migrate_flasher_only.isChecked = false
                    }
                    setCancelable(false)
                }.show()
            }
            else {
                flasherWarning.flasher_warning_checkbox.visibility = View.GONE
                ad.setPositiveButton(R.string.close, null).show()
            }
        }

        mainView.migrate_flasher_only_warning_button.setOnClickListener {
            showFlasherOnlyWarning(false)
        }

        mainView.ignore_cache_checkbox.apply {
            isChecked = getPrefBoolean(PREF_IGNORE_APP_CACHE, false)
            setOnCheckedChangeListener { _, isChecked ->
                putPrefBoolean(PREF_IGNORE_APP_CACHE, isChecked, true)
            }
        }

        mainView.migrate_flasher_only.apply {
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && getPrefBoolean(PREF_SHOW_FLASHER_ONLY_WARNING, true))
                    showFlasherOnlyWarning(true)
                putPrefBoolean(PREF_USE_FLASHER_ONLY, isChecked)
                flasherOnlyBackup = isChecked
            }
            isChecked = !getPrefBoolean(PREF_SHOW_FLASHER_ONLY_WARNING, true)
                    && getPrefBoolean(PREF_USE_FLASHER_ONLY, false)
        }

        getPrefString(PREF_DEFAULT_BACKUP_PATH, DEFAULT_INTERNAL_STORAGE_DIR).run {
            if (this == DEFAULT_INTERNAL_STORAGE_DIR || !(File(this).canWrite())){
                mainView.storage_select_radio_group.check(mainView.internal_storage_radio_button.id)
                setDestination(DEFAULT_INTERNAL_STORAGE_DIR, mainView.destination_name)
            }
            else {
                mainView.storage_select_radio_group.check(mainView.sd_card_radio_button.id)
            }
        }

        mainView.storage_select_radio_group.setOnCheckedChangeListener { _, checkedId ->

            val exSdCardImage = ImageView(this).apply {
                this.setImageResource(R.drawable.ex_sd_card_enabler)
                this.setPadding(10, 10, 10, 10)
            }

            when(checkedId){

                R.id.internal_storage_radio_button ->
                    setDestination(DEFAULT_INTERNAL_STORAGE_DIR, mainView.destination_name)

                R.id.sd_card_radio_button -> {
                    val sdCardProbableDirs = commonTools.getSdCardPaths()
                    // the new function is supposed to automatically remove "" (empty files)

                    when (sdCardProbableDirs.size){
                        0 -> {
                            AlertDialog.Builder(this)
                                    .setTitle(R.string.no_sd_card_detected)
                                    .setMessage(R.string.no_sd_card_detected_exp)
                                    .setPositiveButton(R.string.close, null)
                                    .setNegativeButton(R.string.learn_about_sd_card_support) { _, _ ->
                                        commonTools.showSdCardSupportDialog()
                                    }
                                    .setView(exSdCardImage)
                                    .show()
                            mainView.internal_storage_radio_button.isChecked = true
                        }

                        1 -> setDestination(sdCardProbableDirs[0] + "/Migrate", mainView.destination_name)

                        else -> {

                            val sdGroup = RadioGroup(this).apply {
                                this.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                this.setPadding(20, 20, 20 ,20)
                            }

                            for (i in sdCardProbableDirs.indices){

                                val sdFile = File(sdCardProbableDirs[0])
                                val button = RadioButton(this).apply {
                                    this.text = sdFile.name
                                    this.id = i
                                }

                                sdGroup.addView(button)
                            }

                            sdGroup.check(0)

                            AlertDialog.Builder(this)
                                    .setTitle(R.string.please_select_sd_card)
                                    .setView(sdGroup)
                                    .setCancelable(false)
                                    .setNegativeButton(android.R.string.cancel) {_, _ ->
                                        mainView.internal_storage_radio_button.isChecked = true
                                        setDestination(DEFAULT_INTERNAL_STORAGE_DIR, mainView.destination_name)
                                    }
                                    .setPositiveButton(android.R.string.cancel) { _, _ ->
                                        setDestination(sdCardProbableDirs[sdGroup.checkedRadioButtonId] + "/Migrate",
                                                mainView.destination_name)
                                    }
                                    .show()

                        }
                    }
                }
            }

        }

        val sdf = SimpleDateFormat("yyyy.MM.dd_HH.mm.ss")
        if (isAllAppsSelected && do_backup_sms.isChecked && do_backup_calls.isChecked && do_backup_installers.isChecked)              //extras_markers
            mainView.backup_name_edit_text.setText("${getString(R.string.fullBackupLabel)}_${sdf.format(Calendar.getInstance().time)}")
        else mainView.backup_name_edit_text.setText("${getString(R.string.backupLabel)}_${sdf.format(Calendar.getInstance().time)}")

        val nameDialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.setBackupName))
                .setView(mainView)
                .setPositiveButton(getString(android.R.string.ok), null)
                .setNegativeButton(getString(android.R.string.cancel), null)
                .setCancelable(false)
                .create()

        fun validateName(){
            mainView.backup_name_edit_text.text.toString().trim()
                    .replace("[\\\\/:*?\"'<>|]".toRegex(), " ")
                    .replace(' ', '_').run {

                        if (this != "") {

                            val dir = File("$destination/$this")
                            val zip = File("$destination/$this.zip")

                            fun startBackup(){
                                backupName = this
                                waitingDialog.show()

                                makeAppPackets = MakeAppPackets(JOBCODE_MAKE_APP_PACKETS, this@ExtraBackupsKotlin, destination, dialogView, flasherOnlyBackup)
                                makeAppPackets?.execute()
                            }

                            if (dir.exists() || zip.exists()){
                                AlertDialog.Builder(this@ExtraBackupsKotlin)
                                        .setTitle(R.string.overwrite)
                                        .setMessage(R.string.overwriteMessage)
                                        .setPositiveButton(R.string.yes) { _, _ ->
                                            if (dir.exists() && CommonToolsKotlin.isDeletable(dir)) dir.deleteRecursively()
                                            if (zip.exists()) zip.delete()
                                            nameDialog.dismiss()
                                            startBackup()
                                        }
                                        .setNegativeButton(getString(R.string.rename), null)
                                        .show()
                            }
                            else {
                                nameDialog.dismiss()
                                startBackup()
                            }
                        }
                        else Toast.makeText(this@ExtraBackupsKotlin, getString(R.string.backupNameCannotBeEmpty), Toast.LENGTH_SHORT).show()
                    }
        }

        mainView.backup_name_edit_text.apply {
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE){
                    validateName()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }
        }

        nameDialog.setOnShowListener {
            (it as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                validateName()
            }
        }

        nameDialog.show()
    }

    private fun setDestination(path: String, label: TextView) {

        File(destination).let {
            if (!it.exists()) it.mkdirs()
        }

        destination = path
        label.text = destination
        putPrefString(PREF_DEFAULT_BACKUP_PATH, destination, true)
    }

    override fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any?) {           //extras_markers

        fun sms(){
            if (jobSuccess) {
                tryIt({
                    updateSms(jobResult as ArrayList<SmsDataPacketKotlin>)
                }, true)
            } else {
                do_backup_sms.isChecked = false
                jobResult.toString().let {
                    showErrorDialog(it, getString(R.string.error_reading_sms))
                }
            }}

        fun setDefaultValueForExtraNotPresent(execFunc: () -> Unit, doCheckbox: CheckBox, title: String, body: String){

            AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(body)
                    .setPositiveButton(R.string.yes_recommended) {_, _ -> execFunc() }
                    .setNegativeButton(android.R.string.cancel) {_, _ ->
                        doCheckbox.isChecked = false
                    }
                    .setCancelable(false)
                    .show()
        }

        if (jobCode != JOBCODE_READ_SMS_THEN_CALLS)
            toggleBackupButton(1)

        when (jobCode){

            JOBCODE_READ_CONTACTS ->
                if (jobSuccess) {
                    tryIt({
                        updateContacts(jobResult as ArrayList<ContactsDataPacketKotlin>)
                    }, true)
                } else {
                    do_backup_contacts.isChecked = false
                    jobResult.toString().let {
                        showErrorDialog(it, getString(R.string.error_reading_contacts))
                    }
                }

            JOBCODE_LOAD_CONTACTS ->
                if (jobSuccess){
                    tryIt({
                        updateContacts(jobResult as ArrayList<ContactsDataPacketKotlin>)
                    }, true)
                }

            JOBCODE_READ_SMS -> sms()

            JOBCODE_READ_SMS_THEN_CALLS -> {
                sms()
                do_backup_calls.isChecked = true
            }

            JOBCODE_LOAD_SMS ->
                if (jobSuccess){
                    tryIt({
                        updateSms(jobResult as ArrayList<SmsDataPacketKotlin>)
                    }, true)
                }

            JOBCODE_READ_CALLS ->
                if (jobSuccess) {
                    tryIt({
                        updateCalls(jobResult as ArrayList<CallsDataPacketsKotlin>)
                    }, true)
                } else {
                    do_backup_calls.isChecked = false
                    jobResult.toString().let {
                        showErrorDialog(it, getString(R.string.error_reading_calls))
                    }
                }

            JOBCODE_LOAD_CALLS ->
                if (jobSuccess){
                    tryIt({
                        updateCalls(jobResult as ArrayList<CallsDataPacketsKotlin>)
                    }, true)
                }

            JOBCODE_READ_DPI ->
                tryIt ({
                    jobResult.toString().let {
                        if (jobSuccess) dpiText = it
                        else {
                            do_backup_dpi.isChecked = false
                            showErrorDialog(it, getString(R.string.error_reading_dpi))
                        }
                    }
                }, true)


            JOBCODE_READ_ADB ->
                tryIt({
                    if (jobSuccess) {

                        {
                            adb_selected_status.text = getString(
                                    when (adbState) {
                                        0 -> R.string.adb_disabled
                                        1 -> R.string.adb_enabled
                                        else -> R.string.adb_unknown
                                    }
                            )
                        }.run {
                            if (jobResult == null){
                                setDefaultValueForExtraNotPresent({
                                    adbState = 0
                                    this()
                                }, do_backup_adb, getString(R.string.adb_label), getString(R.string.no_default_adb_scale))
                            }
                            else {
                                adbState = jobResult as Int
                                this()
                            }
                        }

                    }
                    else {
                        do_backup_adb.isChecked = false
                        showErrorDialog(jobResult.toString(), getString(R.string.error_reading_adb))
                    }
                }, true)

            JOBCODE_LOAD_KEYBOARDS -> {
                tryIt({
                    jobResult.toString().let {
                        keyboardText = if (jobSuccess) it
                        else null
                    }
                }, true)
            }

            JOBCODE_LOAD_INSTALLERS ->
                if (jobSuccess) {
                    tryIt ({
                        updateInstallers(jobResult as ArrayList<BackupDataPacketKotlin>)
                    }, true)
                }

            JOBCODE_READ_WIFI ->
                tryIt ({
                    if (jobSuccess) {
                        wifiData = jobResult as WifiDataPacket
                        wifi_selected_status.text = wifiData?.fileName
                    }
                    else {
                        do_backup_wifi.isChecked = false
                        showErrorDialog(jobResult.toString(), getString(R.string.error_reading_wifi))
                    }
                }, true)

            JOBCODE_READ_FONTSCALE ->
                tryIt ({
                    if (jobSuccess) {
                        if (jobResult == null) {
                            setDefaultValueForExtraNotPresent({
                                fontScale = 1.0
                                fontScale_selected_status.text = fontScale.toString()
                            }, do_backup_fontScale, getString(R.string.fontScale_label), getString(R.string.no_default_font_scale))
                        }
                        else {
                            fontScale = jobResult as Double
                            fontScale_selected_status.text = fontScale.toString()
                        }
                    }
                    else {
                        do_backup_fontScale.isChecked = false
                        showErrorDialog(jobResult.toString(), getString(R.string.error_reading_fontScale))
                    }
                }, true)

            JOBCODE_MAKE_APP_PACKETS ->
                tryIt ({
                    jobResult.toString().let {
                        if (jobSuccess) {
                            appPackets.clear()
                            appPackets.addAll(jobResult as ArrayList<AppPacket>)

                            waitingDialog.waiting_head.setText(R.string.just_a_minute)
                            waitingDialog.waiting_progress.setText(R.string.starting_engine)
                            waitingDialog.waiting_details.text = ""

                            Intent(this, BackupServiceKotlin::class.java).apply {
                                putExtra(EXTRA_DESTINATION, destination)
                                putExtra(EXTRA_BACKUP_NAME, backupName)
                                putExtra(EXTRA_FLASHER_ONLY, flasherOnlyBackup)
                            }.run {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    startForegroundService(this)
                                } else {
                                    startService(this)
                                }
                            }
                        }
                        else tryIt { waitingDialog.dismiss() }
                    }
                }, true)
        }
    }

    private fun updateContacts(newList: ArrayList<ContactsDataPacketKotlin>){

        contactsList.clear()

        contacts_selected_status.text = getString(R.string.reading)
        contacts_read_progress.visibility = View.GONE

        var n = 0
        newList.forEach {
            if (it.selected) n++
            contactsList.add(it)
        }

        if (contactsList.size > 0){
            contacts_selected_status.text = "$n ${getString(R.string.of)} ${contactsList.size}"
            contacts_main_item.setOnClickListener {
                LoadContactsForSelectionKotlin(JOBCODE_LOAD_CONTACTS, this, contactsList).execute()
            }
        }
        else {
            AlertDialog.Builder(this)
                    .setMessage(R.string.empty_contacts)
                    .setPositiveButton(R.string.close) {_, _ -> do_backup_contacts.isChecked = false }
                    .setCancelable(false)
                    .show()
        }
    }

    private fun updateSms(newList: ArrayList<SmsDataPacketKotlin>){

        smsList.clear()

        sms_selected_status.text = getString(R.string.reading)
        sms_read_progress.visibility = View.GONE

        var n = 0
        newList.forEach {
            if (it.selected) n++
            smsList.add(it)
        }

        if (smsList.size > 0){
            sms_selected_status.text = "$n ${getString(R.string.of)} ${smsList.size}"
            sms_main_item.setOnClickListener {
                LoadSmsForSelectionKotlin(JOBCODE_LOAD_SMS, this, smsList).execute()
            }
        }
        else {
            AlertDialog.Builder(this)
                    .setMessage(R.string.empty_sms)
                    .setPositiveButton(R.string.close) {_, _ -> do_backup_sms.isChecked = false }
                    .setCancelable(false)
                    .show()
        }
    }

    private fun updateCalls(newList: ArrayList<CallsDataPacketsKotlin>){

        callsList.clear()

        calls_selected_status.text = getString(R.string.reading)
        calls_read_progress.visibility = View.GONE

        var n = 0
        newList.forEach {
            if (it.selected) n++
            callsList.add(it)
        }

        if (callsList.size > 0){
            calls_selected_status.text = "$n ${getString(R.string.of)} ${callsList.size}"
            calls_main_item.setOnClickListener {
                LoadCallsForSelectionKotlin(JOBCODE_LOAD_CALLS, this, callsList).execute()
            }
        }
        else {
            AlertDialog.Builder(this)
                    .setMessage(R.string.empty_call_logs)
                    .setPositiveButton(R.string.close) {_, _ -> do_backup_calls.isChecked = false }
                    .setCancelable(false)
                    .show()
        }
    }

    private fun updateInstallers(newAppList: ArrayList<BackupDataPacketKotlin>, selfCall: Boolean = false){

        if (selectedBackupDataPackets.isEmpty())
        {
            installer_selected_status.visibility = View.VISIBLE
            installer_selected_status.text = getString(R.string.no_app_selected)
            installers_main_item.isClickable = false
            return
        }
        else {
            if (!selfCall) selectedBackupDataPackets.clear()
            var n = 0
            newAppList.forEach {
                if (!selfCall) selectedBackupDataPackets.add(it)
                if (it.installerName == PACKAGE_NAME_PLAY_STORE || it.installerName == PACKAGE_NAME_FDROID) n++
                if (selectedBackupDataPackets.none { s -> s.PACKAGE_INFO.packageName != it.PACKAGE_INFO.packageName }) Log.d(DEBUG_TAG, "missing: ${it.PACKAGE_INFO.packageName}")
            }

            installer_selected_status.visibility = View.VISIBLE
            installer_selected_status.text = "$n ${getString(R.string.of)} ${selectedBackupDataPackets.size}"
            installers_main_item.isClickable = true
            installers_main_item.setOnClickListener {
                doWaitingJob {
                    loadInstallers = LoadInstallersForSelection(JOBCODE_LOAD_INSTALLERS, this, selectedBackupDataPackets)
                    loadInstallers?.execute()
                }
            }
        }

    }

    override fun onDestroy() {                          //extras_markers
        super.onDestroy()

        tryIt { waitingDialog.dismiss() }

        tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }

        tryIt { makeAppPackets?.cancel(true) }
        tryIt { readContacts?.cancel(true) }
        tryIt { readSms?.cancel(true) }
        tryIt { readCalls?.cancel(true) }
        tryIt { readDpi?.cancel(true) }
        tryIt { readAdb?.cancel(true) }
        tryIt { readWifi?.cancel(true) }
        tryIt { readFontScale?.cancel(true) }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, BackupActivityKotlin::class.java))
    }

}