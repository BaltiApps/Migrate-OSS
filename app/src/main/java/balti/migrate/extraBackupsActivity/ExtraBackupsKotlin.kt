package balti.migrate.extraBackupsActivity

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import balti.migrate.BackupProgressLayout
import balti.migrate.R
import balti.migrate.backupActivity.BackupActivityKotlin
import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.extraBackupsActivity.adb.ReadAdbKotlin
import balti.migrate.extraBackupsActivity.apps.MakeAppPackets
import balti.migrate.extraBackupsActivity.apps.containers.AppBatch
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
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_SERVICE_STARTED
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_REQUEST_BACKUP_DATA
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_START_BATCH_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.CALLS_PERMISSION
import balti.migrate.utilities.CommonToolKotlin.Companion.CONTACT_PERMISSION
import balti.migrate.utilities.CommonToolKotlin.Companion.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_LOAD_CALLS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_LOAD_CONTACTS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_LOAD_INSTALLERS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_LOAD_KEYBOARDS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_LOAD_SMS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_MAKE_APP_PACKETS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_READ_ADB
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_READ_CALLS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_READ_CONTACTS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_READ_DPI
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_READ_FONTSCALE
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_READ_SMS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_READ_SMS_THEN_CALLS
import balti.migrate.utilities.CommonToolKotlin.Companion.JOBCODE_READ_WIFI
import balti.migrate.utilities.CommonToolKotlin.Companion.PACKAGE_NAME_FDROID
import balti.migrate.utilities.CommonToolKotlin.Companion.PACKAGE_NAME_PLAY_STORE
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_AUTOSELECT_EXTRAS
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_FILE_MAIN
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_SHOW_STOCK_WARNING
import balti.migrate.utilities.CommonToolKotlin.Companion.SMS_AND_CALLS_PERMISSION
import balti.migrate.utilities.CommonToolKotlin.Companion.SMS_PERMISSION
import kotlinx.android.synthetic.main.ask_for_backup_name.view.*
import kotlinx.android.synthetic.main.extra_backups.*
import kotlinx.android.synthetic.main.please_wait.view.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ExtraBackupsKotlin : AppCompatActivity(), OnJobCompletion, CompoundButton.OnCheckedChangeListener {

    // to add extras search for "extras_markers" and change those lines/functions

    companion object {
    }

    private val commonTools by lazy { CommonToolKotlin(this) }
    val appListCopied by lazy { ArrayList<BackupDataPacketKotlin>(0) }

    private lateinit var destination: String
    private var backupName = ""
    private lateinit var main: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
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

    private var appBatches = ArrayList<AppBatch>(0)
    private var contactList = ArrayList<ContactsDataPacketKotlin>(0)    //extras_markers
    private var smsList = ArrayList<SmsDataPacketKotlin>(0)
    private var callsList = ArrayList<CallsDataPacketsKotlin>(0)
    private var keyboardText = ""
    private var dpiText = ""
    private var adbState = 0
    private var wifiData: WifiDataPacket? = null
    private var fontScale = 0.0

    private val progressReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                startActivity(Intent(this@ExtraBackupsKotlin, BackupProgressLayout::class.java)   /*kotlin*/
                        .apply {
                            putExtras(this.extras)
                            action = ACTION_BACKUP_PROGRESS
                        }
                )
                commonTools.tryIt { commonTools.LBM?.unregisterReceiver(this) }
                finish()
            }
        }
    }

    private val serviceStartReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                BackupServiceKotlin.destination = destination
                BackupServiceKotlin.backupName = backupName

                BackupServiceKotlin.appBatches = appBatches

                BackupServiceKotlin.contactsList = if (do_backup_contacts.isChecked) contactList else null
                BackupServiceKotlin.callsList = if (do_backup_calls.isChecked) callsList else null
                BackupServiceKotlin.smsList = if (do_backup_sms.isChecked) smsList else null

                BackupServiceKotlin.dpiText = if (do_backup_dpi.isChecked) dpiText else null
                BackupServiceKotlin.keyboardText = if (do_backup_keyboard.isChecked) keyboardText else null
                BackupServiceKotlin.adbState = if (do_backup_adb.isChecked) adbState else null
                BackupServiceKotlin.fontScale = if (do_backup_fontScale.isChecked) fontScale else null
                BackupServiceKotlin.wifiData = if (do_backup_wifi.isChecked) wifiData else null

                BackupServiceKotlin.doBackupInstallers = do_backup_installers.isChecked

                commonTools.LBM?.sendBroadcast(Intent(ACTION_START_BATCH_BACKUP))
                commonTools.tryIt { commonTools.LBM?.unregisterReceiver(this) }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.extra_backups)
        main = getSharedPreferences(PREF_FILE_MAIN, Context.MODE_PRIVATE)
        editor = main.edit()

        destination = main.getString(PREF_DEFAULT_BACKUP_PATH, DEFAULT_INTERNAL_STORAGE_DIR)

        fun doEverything() {
            startBackupButton.setOnClickListener {
                if (appListCopied.size > 0 || do_backup_contacts.isChecked || do_backup_calls.isChecked
                        || do_backup_sms.isChecked || do_backup_dpi.isChecked || do_backup_keyboard.isChecked
                        || do_backup_installers.isChecked || do_backup_adb.isChecked || do_backup_wifi.isChecked || do_backup_fontScale.isChecked) {                  //extras_markers
                    askForName()
                }
            }

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

            commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_BACKUP_PROGRESS))
            commonTools.LBM?.registerReceiver(serviceStartReceiver, IntentFilter(ACTION_BACKUP_SERVICE_STARTED))

            commonTools.LBM?.sendBroadcast(Intent(ACTION_REQUEST_BACKUP_DATA))

            do_backup_contacts.setOnCheckedChangeListener(this)         //extras_markers
            do_backup_sms.setOnCheckedChangeListener(this)
            do_backup_calls.setOnCheckedChangeListener(this)
            do_backup_dpi.setOnCheckedChangeListener(this)
            do_backup_keyboard.setOnCheckedChangeListener(this)
            do_backup_installers.setOnCheckedChangeListener(this)
            do_backup_adb.setOnCheckedChangeListener(this)
            do_backup_wifi.setOnCheckedChangeListener(this)
            do_backup_fontScale.setOnCheckedChangeListener(this)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                wifi_main_item.visibility = View.GONE

            if (main.getBoolean(PREF_AUTOSELECT_EXTRAS, true)) {        //extras_markers
                val isSmsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                val isCallsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
                val isSmsAndCallsGranted = isSmsGranted && isCallsGranted

                when {
                    isSmsAndCallsGranted ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS,
                                Manifest.permission.READ_CALL_LOG), SMS_AND_CALLS_PERMISSION)
                    isCallsGranted -> do_backup_calls.isChecked = true
                    isSmsGranted -> do_backup_sms.isChecked = true
                }
                do_backup_installers.isChecked = true
            }
        }

        class Copy : AsyncTask<Any, Any, Any>() {

            private val dialogView by lazy { View.inflate(this@ExtraBackupsKotlin, R.layout.please_wait, null) }
            private val waitingDialog by lazy {
                AlertDialog.Builder(this@ExtraBackupsKotlin)
                        .setView(dialogView)
                        .setCancelable(false)
                        .create()
            }

            override fun onPreExecute() {
                super.onPreExecute()
                dialogView.waiting_cancel.setOnClickListener {
                    cancel(true)
                    startActivity(Intent(this@ExtraBackupsKotlin, BackupActivityKotlin::class.java))
                    finish()
                }
                waitingDialog.show()
            }

            override fun doInBackground(vararg params: Any?): Any? {
                commonTools.tryIt({
                    BackupActivityKotlin.appList.forEach {
                        if (it.APP || it.PERMISSION || it.DATA) {
                            appListCopied.add(it.copy())
                            if (isAllAppsSelected && !(it.APP && it.DATA && it.PERMISSION)) isAllAppsSelected = false
                        } else if (isAllAppsSelected) isAllAppsSelected = false
                    }


                }, true, isCancelable = false)

                return null
            }

            override fun onPostExecute(result: Any?) {
                super.onPostExecute(result)
                waitingDialog.dismiss()
                doEverything()
            }
        }

        Copy().execute()
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {        //extras_markers

        fun showStockWarning(fPositive:() -> Unit, fNegative:() -> Unit){
            if (main.getBoolean(PREF_SHOW_STOCK_WARNING, true)){
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
                            editor.putBoolean(PREF_SHOW_STOCK_WARNING, false)
                            editor.commit()
                            fPositive()
                        }
                        .setCancelable(false)
                        .show()
            }
            else {
                fPositive()
            }
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
            else {
                contacts_main_item.isClickable = false
                contacts_read_progress.visibility = View.GONE
                contacts_selected_status.visibility = View.GONE
                commonTools.tryIt { readContacts?.cancel(true) }
            }

        } else if (buttonView == do_backup_sms) {

            if (isChecked) {

                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_SMS),
                        SMS_PERMISSION)

            } else {
                sms_main_item.isClickable = false
                sms_read_progress.visibility = View.GONE
                sms_selected_status.visibility = View.GONE
                commonTools.tryIt { readSms?.cancel(true) }

            }

        } else if (buttonView == do_backup_calls) {

            if (isChecked) {

                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_CALL_LOG),
                        CALLS_PERMISSION)


            } else {
                calls_main_item.isClickable = false
                calls_read_progress.visibility = View.GONE
                calls_selected_status.visibility = View.GONE
                commonTools.tryIt { readCalls?.cancel(true) }

            }

        } else if (buttonView == do_backup_dpi) {
            if (isChecked) {

                showStockWarning({
                    readDpi = ReadDpiKotlin(JOBCODE_READ_DPI, this, dpi_main_item, dpi_selected_status, dpi_read_progress, do_backup_dpi)
                    readDpi?.let { it.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)}
                    toggleBackupButton(0)
                }, {
                    do_backup_dpi.isChecked = false
                })
                sal_dpi.visibility = View.GONE

            } else {
                dpi_main_item.isClickable = false
                dpi_selected_status.visibility = View.GONE
                dpi_read_progress.visibility = View.GONE
                sal_dpi.visibility = View.VISIBLE

                commonTools.tryIt { readDpi?.cancel(true) }

            }

        } else if (buttonView == do_backup_adb) {
            if (isChecked) {

                showStockWarning({
                    readAdb = ReadAdbKotlin(JOBCODE_READ_ADB, this, adb_main_item, adb_selected_status, adb_read_progress, do_backup_adb)
                    readAdb?.let { it.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)}
                    toggleBackupButton(0)
                }, {
                    do_backup_adb.isChecked = false
                })
                sal_adb.visibility = View.GONE

            } else {
                adb_main_item.isClickable = false
                adb_selected_status.visibility = View.GONE
                adb_read_progress.visibility = View.GONE
                sal_adb.visibility = View.VISIBLE

                commonTools.tryIt { readAdb?.cancel(true) }

            }

        } else if (buttonView == do_backup_keyboard) {

            if (isChecked) {
                loadKeyboard = LoadKeyboardForSelection(JOBCODE_LOAD_KEYBOARDS, this, keyboard_main_item, keyboard_selected_status, do_backup_keyboard, appListCopied)
                loadKeyboard?.let { it.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR) }
                toggleBackupButton(0)
            }
            else {
                keyboardText = ""
                keyboard_main_item.isClickable = false
                keyboard_selected_status.visibility = View.GONE

                commonTools.tryIt { loadKeyboard?.cancel(true) }
            }

        } else if (buttonView == do_backup_installers){
            if (isChecked){
                updateInstallers(appListCopied, true)
            }
            else {
                installers_main_item.isClickable = false
                installer_selected_status.visibility = View.GONE

                commonTools.tryIt { loadInstallers?.cancel(true) }
            }
        } else if (buttonView == do_backup_wifi){
            if (isChecked) {

                showStockWarning({
                    readWifi = ReadWifiKotlin(JOBCODE_READ_WIFI, this, wifi_main_item, wifi_selected_status, wifi_read_progress, do_backup_wifi)
                    readWifi?.let { it.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)}
                    toggleBackupButton(0)
                }, {
                    do_backup_wifi.isChecked = false
                })
                sal_wifi.visibility = View.GONE

            } else {
                wifiData = null
                wifi_main_item.isClickable = false
                wifi_selected_status.visibility = View.GONE
                wifi_read_progress.visibility = View.GONE
                sal_wifi.visibility = View.VISIBLE

                commonTools.tryIt { readWifi?.cancel(true) }

            }
        } else if (buttonView == do_backup_fontScale) {
            if (isChecked){

                showStockWarning({
                    readFontScale = ReadFontScaleKotlin(JOBCODE_READ_FONTSCALE, this, fontScale_main_item, fontScale_selected_status, fontScale_read_progress, do_backup_fontScale)
                    readFontScale?.let { it.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)}
                    toggleBackupButton(0)
                }, {
                    do_backup_fontScale.isChecked = false
                })
                sal_fontScale.visibility = View.GONE

            } else {
                fontScale = 0.0
                fontScale_main_item.isClickable = false
                fontScale_selected_status.visibility = View.GONE
                fontScale_read_progress.visibility = View.GONE
                sal_fontScale.visibility = View.VISIBLE

                commonTools.tryIt { readFontScale?.cancel(true) }

            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {     //extras_markers
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SMS_AND_CALLS_PERMISSION){

            if (grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                do_backup_sms.setOnCheckedChangeListener(null)
                do_backup_sms.isChecked = true

                commonTools.tryIt({
                    readSms = ReadSmsKotlin(JOBCODE_READ_SMS_THEN_CALLS, this, sms_main_item, sms_selected_status, sms_read_progress, do_backup_sms)
                    readSms?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    toggleBackupButton(0)
                }, true)

                do_backup_sms.setOnCheckedChangeListener(this)
            }
        }
        else if (requestCode == CONTACT_PERMISSION){

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                commonTools.tryIt({
                    readContacts = ReadContactsKotlin(JOBCODE_READ_CONTACTS, this, contacts_main_item, contacts_selected_status, contacts_read_progress, do_backup_contacts)
                    readContacts?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    toggleBackupButton(0)
                }, true)

            }
            else {
                Toast.makeText(this, R.string.contacts_access_needed, Toast.LENGTH_SHORT).show()
                do_backup_contacts.isChecked = false
            }

        }
        else if (requestCode == SMS_PERMISSION){

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                commonTools.tryIt({
                    readSms = ReadSmsKotlin(JOBCODE_READ_SMS, this, sms_main_item, sms_selected_status, sms_read_progress, do_backup_sms)
                    readSms?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    toggleBackupButton(0)
                }, true)

            }
            else {
                Toast.makeText(this, R.string.sms_access_needed, Toast.LENGTH_SHORT).show()
                do_backup_sms.isChecked = false
            }

        }
        else if (requestCode == CALLS_PERMISSION){

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                commonTools.tryIt({
                    readCalls = ReadCallsKotlin(JOBCODE_READ_CALLS, this, calls_main_item, calls_selected_status, calls_read_progress, do_backup_calls)
                    readCalls?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    toggleBackupButton(0)
                }, true)

            }
            else {
                Toast.makeText(this, R.string.calls_access_needed, Toast.LENGTH_SHORT).show()
                do_backup_calls.isChecked = false
            }

        }
    }

    private fun askForName() {

        val mainView = View.inflate(this, R.layout.ask_for_backup_name, null)

        mainView.backup_name_edit_text.setSingleLine(true)
        mainView.sd_card_name.text = destination

        main.getString(PREF_DEFAULT_BACKUP_PATH, DEFAULT_INTERNAL_STORAGE_DIR).run {
            if (this == DEFAULT_INTERNAL_STORAGE_DIR || !(File(this).canWrite())){
                mainView.storage_select_radio_group.check(mainView.internal_storage_radio_button.id)
                setDestination(DEFAULT_INTERNAL_STORAGE_DIR, mainView.sd_card_name)
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
                    setDestination(DEFAULT_INTERNAL_STORAGE_DIR, mainView.sd_card_name)

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

                        1 -> setDestination(sdCardProbableDirs[0] + "/Migrate", mainView.sd_card_name)

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
                                        setDestination(DEFAULT_INTERNAL_STORAGE_DIR, mainView.sd_card_name)
                                    }
                                    .setPositiveButton(android.R.string.cancel) { _, _ ->
                                        setDestination(sdCardProbableDirs[sdGroup.checkedRadioButtonId] + "/Migrate",
                                                mainView.sd_card_name)
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
                                makeAppPackets = MakeAppPackets(JOBCODE_MAKE_APP_PACKETS, this@ExtraBackupsKotlin, destination, appListCopied)
                                makeAppPackets?.execute()
                            }

                            if (dir.exists() || zip.exists()){
                                AlertDialog.Builder(this@ExtraBackupsKotlin)
                                        .setTitle(R.string.overwrite)
                                        .setMessage(R.string.overwriteMessage)
                                        .setPositiveButton(R.string.yes) { _, _ ->
                                            if (dir.exists()) commonTools.dirDelete(dir.absolutePath)
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
        editor.putString("defaultBackupPath", destination)
        editor.commit()
    }

    override fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any?) {           //extras_markers

        fun sms(){
            if (jobSuccess) {
                commonTools.tryIt({
                    updateSms(jobResult as ArrayList<SmsDataPacketKotlin>)
                }, true)
            } else {
                do_backup_sms.isChecked = false
                jobResult.toString().let {
                    commonTools.showErrorDialog(it, getString(R.string.error_reading_sms))
                }
            }}

        toggleBackupButton(1)

        when (jobCode){

            JOBCODE_READ_CONTACTS ->
                if (jobSuccess) {
                    commonTools.tryIt({
                        updateContacts(jobResult as ArrayList<ContactsDataPacketKotlin>)
                    }, true)
                } else {
                    do_backup_contacts.isChecked = false
                    jobResult.toString().let {
                        commonTools.showErrorDialog(it, getString(R.string.error_reading_contacts))
                    }
                }

            JOBCODE_LOAD_CONTACTS ->
                if (jobSuccess){
                    commonTools.tryIt({
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
                    commonTools.tryIt({
                        updateSms(jobResult as ArrayList<SmsDataPacketKotlin>)
                    }, true)
                }

            JOBCODE_READ_CALLS ->
                if (jobSuccess) {
                    commonTools.tryIt({
                        updateCalls(jobResult as ArrayList<CallsDataPacketsKotlin>)
                    }, true)
                } else {
                    do_backup_calls.isChecked = false
                    jobResult.toString().let {
                        commonTools.showErrorDialog(it, getString(R.string.error_reading_calls))
                    }
                }

            JOBCODE_LOAD_CALLS ->
                if (jobSuccess){
                    commonTools.tryIt({
                        updateCalls(jobResult as ArrayList<CallsDataPacketsKotlin>)
                    }, true)
                }

            JOBCODE_READ_DPI ->
                commonTools.tryIt ({
                    jobResult.toString().let {
                        if (jobSuccess) dpiText = it
                        else commonTools.showErrorDialog(it, getString(R.string.error_reading_dpi))
                    }
                }, true)


            JOBCODE_READ_ADB ->
                commonTools.tryIt({
                    if (jobSuccess) {
                        adbState = jobResult as Int
                        adb_selected_status.text = getString(when (adbState){
                            0 -> R.string.adb_disabled
                            1 -> R.string.adb_enabled
                            else -> R.string.adb_unknown
                        })
                    }
                    else commonTools.showErrorDialog(jobResult.toString(), getString(R.string.error_reading_adb))
                }, true)

            JOBCODE_LOAD_KEYBOARDS ->
                commonTools.tryIt ({
                    jobResult.toString().let {
                        if (jobSuccess) keyboardText = it
                    }
                }, true)

            JOBCODE_LOAD_INSTALLERS ->
                if (jobSuccess) {
                    commonTools.tryIt ({
                        updateInstallers(jobResult as ArrayList<BackupDataPacketKotlin>)
                    }, true)
                }

            JOBCODE_READ_WIFI ->
                commonTools.tryIt ({
                    if (jobSuccess) {
                        wifiData = jobResult as WifiDataPacket
                        wifi_selected_status.text = wifiData?.fileName
                    }
                    else commonTools.showErrorDialog(jobResult.toString(), getString(R.string.error_reading_wifi))
                }, true)

            JOBCODE_READ_FONTSCALE ->
                commonTools.tryIt ({
                    if (jobSuccess) {
                        fontScale = jobResult as Double
                        fontScale_selected_status.text = fontScale.toString()
                    }
                    else commonTools.showErrorDialog(jobResult.toString(), getString(R.string.error_reading_fontScale))
                }, true)

            JOBCODE_MAKE_APP_PACKETS ->
                commonTools.tryIt ({
                    jobResult.toString().let {
                        if (jobSuccess) {
                            appBatches = jobResult as ArrayList<AppBatch>
                            //TODO("start backup engine")
                        }
                    }
                }, true)
        }
    }

    private fun updateContacts(newContactsList: ArrayList<ContactsDataPacketKotlin>){
        contactList.clear()

        contacts_selected_status.text = getString(R.string.reading)
        contacts_read_progress.visibility = View.GONE

        var n = 0
        newContactsList.forEach {
            contactList.add(it)
            if (it.selected) n++
        }

        if (n > 0){
            contacts_selected_status.text = "$n of ${contactList.size}"
            contacts_main_item.setOnClickListener {
                LoadContactsForSelectionKotlin(JOBCODE_LOAD_CONTACTS, this, contactList).execute()
            }
        }
        else {
            do_backup_contacts.isChecked = false
        }
    }

    private fun updateSms(newSmsList: ArrayList<SmsDataPacketKotlin>){
        smsList.clear()

        sms_selected_status.text = getString(R.string.reading)
        sms_read_progress.visibility = View.GONE

        var n = 0
        newSmsList.forEach {
            smsList.add(it)
            if (it.selected) n++
        }

        if (n > 0){
            sms_selected_status.text = "$n of ${smsList.size}"
            sms_main_item.setOnClickListener {
                LoadSmsForSelectionKotlin(JOBCODE_LOAD_SMS, this, smsList).execute()
            }
        }
        else {
            do_backup_sms.isChecked = false
        }
    }

    private fun updateCalls(newCallsList: ArrayList<CallsDataPacketsKotlin>){
        callsList.clear()

        calls_selected_status.text = getString(R.string.reading)
        calls_read_progress.visibility = View.GONE

        var n = 0
        newCallsList.forEach {
            callsList.add(it)
            if (it.selected) n++
        }

        if (n > 0){
            calls_selected_status.text = "$n of ${callsList.size}"
            calls_main_item.setOnClickListener {
                LoadCallsForSelectionKotlin(JOBCODE_LOAD_CALLS, this, callsList).execute()
            }
        }
        else {
            do_backup_calls.isChecked = false
        }
    }

    private fun updateInstallers(newAppList: ArrayList<BackupDataPacketKotlin>, selfCall: Boolean = false){

        if (appListCopied.size == 0)
        {
            installer_selected_status.visibility = View.VISIBLE
            installer_selected_status.text = getString(R.string.no_app_selected)
            installers_main_item.isClickable = false
            return
        }
        else {
            if (!selfCall) appListCopied.clear()
            var n = 0
            newAppList.forEach {
                if (!selfCall) appListCopied.add(it)
                if (it.installerName == PACKAGE_NAME_PLAY_STORE || it.installerName == PACKAGE_NAME_FDROID) n++
            }
            installer_selected_status.visibility = View.VISIBLE
            installer_selected_status.text = "$n of ${appListCopied.size}"
            installers_main_item.isClickable = true
            installers_main_item.setOnClickListener {
                loadInstallers = LoadInstallersForSelection(JOBCODE_LOAD_INSTALLERS, this, appListCopied)
                loadInstallers?.execute()
                toggleBackupButton(0)
            }
        }

    }

    override fun onDestroy() {                          //extras_markers
        super.onDestroy()

        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
        commonTools.tryIt { commonTools.LBM?.unregisterReceiver(serviceStartReceiver) }

        commonTools.tryIt { makeAppPackets?.cancel(true) }
        commonTools.tryIt { readContacts?.cancel(true) }
        commonTools.tryIt { readSms?.cancel(true) }
        commonTools.tryIt { readCalls?.cancel(true) }
        commonTools.tryIt { readDpi?.cancel(true) }
        commonTools.tryIt { readAdb?.cancel(true) }
        commonTools.tryIt { readWifi?.cancel(true) }
        commonTools.tryIt { readFontScale?.cancel(true) }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, BackupActivityKotlin::class.java))
    }

}