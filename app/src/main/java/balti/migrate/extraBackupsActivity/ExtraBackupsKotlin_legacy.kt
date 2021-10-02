package balti.migrate.extraBackupsActivity

//import balti.migrate.AppInstance.Companion.callsList
//import balti.migrate.extraBackupsActivity.engines.calls.LoadCallsForSelectionKotlin
//import balti.migrate.extraBackupsActivity.engines.calls.ReadCallsKotlin_legacy
//import balti.migrate.extraBackupsActivity.engines.calls.containers.CallsDataPacketsKotlin
//import balti.migrate.utilities.CommonToolsKotlin.Companion.CALLS_PERMISSION
//import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_LOAD_CALLS
//import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_CALLS
//import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_SMS_THEN_CALLS
//import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_CALLS
//import balti.migrate.utilities.CommonToolsKotlin.Companion.SMS_AND_CALLS_PERMISSION
//import balti.migrate.AppInstance.Companion.dpiText
//import balti.migrate.extraBackupsActivity.engines.dpi.ReadDpiKotlin_legacy
//import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_DPI
//import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_DPI
//import balti.migrate.AppInstance.Companion.fontScale
//import balti.migrate.extraBackupsActivity.engines.fontScale.ReadFontScaleKotlin_legacy
//import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_FONTSCALE
//import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_FONTSCALE
//import balti.migrate.AppInstance.Companion.adbState
//import balti.migrate.extraBackupsActivity.engines.adb.ReadAdbKotlin_legacy
//import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_ADB
//import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_ADB
//import balti.migrate.AppInstance.Companion.keyboardText
//import balti.migrate.extraBackupsActivity.engines.keyboard.LoadKeyboardForSelection_legacy
//import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_LOAD_KEYBOARDS
//import balti.migrate.AppInstance.Companion.doBackupInstallers
//import balti.migrate.AppInstance.Companion.wifiData
//import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
//import balti.migrate.extraBackupsActivity.engines.installer.LoadInstallersForSelection_legacy
//import balti.migrate.extraBackupsActivity.wifi.ReadWifiKotlin
//import balti.migrate.extraBackupsActivity.wifi.containers.WifiDataPacket
//import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_LOAD_INSTALLERS
//import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_READ_WIFI
//import balti.migrate.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_FDROID
//import balti.migrate.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_PLAY_STORE
//import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_INSTALLERS
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.AppInstance.Companion.appBackupDataPackets
import balti.migrate.AppInstance.Companion.appPackets
import balti.migrate.AppInstance.Companion.selectedBackupDataPackets
import balti.migrate.R
import balti.migrate.backupActivity.BackupActivityKotlin
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.extraBackupsActivity.apps.MakeAppPackets
import balti.migrate.extraBackupsActivity.apps.containers.AppPacket
import balti.migrate.extraBackupsActivity.engines.adb.AdbFragment
import balti.migrate.extraBackupsActivity.engines.calls.CallsFragment
import balti.migrate.extraBackupsActivity.engines.contacts.ContactsFragment
import balti.migrate.extraBackupsActivity.engines.dpi.DpiFragment
import balti.migrate.extraBackupsActivity.engines.fontScale.FontScaleFragment
import balti.migrate.extraBackupsActivity.engines.keyboard.KeyboardFragment
import balti.migrate.extraBackupsActivity.engines.sms.SmsFragment
import balti.migrate.extraBackupsActivity.utils.OnJobCompletion
import balti.migrate.simpleActivities.ProgressShowActivity
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_REQUEST_BACKUP_DATA
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_DESTINATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FLASHER_ONLY
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_IS_ALL_APP_SELECTED
import balti.migrate.utilities.CommonToolsKotlin.Companion.IS_OTHER_APP_DATA_VISIBLE
import balti.migrate.utilities.CommonToolsKotlin.Companion.JOBCODE_MAKE_APP_PACKETS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_AUTOSELECT_EXTRAS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_CALLS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_SMS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SHOW_STOCK_WARNING
import balti.migrate.utilities.constants.MDP_Constants
import balti.module.baltitoolbox.functions.GetResources.getColorFromRes
import balti.module.baltitoolbox.functions.Misc
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefBoolean
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import kotlinx.android.synthetic.main.ask_for_backup_name.view.*
import kotlinx.android.synthetic.main.extra_backups.*
import kotlinx.android.synthetic.main.flasher_only_warning.view.*
import kotlinx.android.synthetic.main.please_wait.*
import java.text.SimpleDateFormat
import java.util.*

class ExtraBackupsKotlin_legacy : AppCompatActivity(), OnJobCompletion, CompoundButton.OnCheckedChangeListener {

    // to add extras search for "extras_markers" and change those lines/functions

    private val commonTools by lazy { CommonToolsKotlin(this) }
    //private val appListCopied by lazy { ArrayList<BackupDataPacketKotlin>(0) }

    private lateinit var destination: String
    private var backupName = ""
    private var isAllAppsSelected = true

                    //extras_markers
    //private var readSms: ReadSmsKotlin_legacy? = null
    //private var readCalls: ReadCallsKotlin_legacy? = null
    //private var readDpi: ReadDpiKotlin_legacy? = null
    //private var readAdb: ReadAdbKotlin_legacy? = null
    //private var readWifi: ReadWifiKotlin? = null
    //private var readFontScale: ReadFontScaleKotlin_legacy? = null

    private var makeAppPackets: MakeAppPackets? = null

    //private var loadKeyboard: LoadKeyboardForSelection_legacy? = null
    //private var loadInstallers: LoadInstallersForSelection_legacy? = null

    private var flasherOnlyBackup = false

    private val ASK_FOR_NAME_JOBCODE = 1666
    private val SETUP_MDP_JOBCODE = 1667

    private var mdpSetFromActivity = false

    private val contactsFragment: ContactsFragment by lazy { supportFragmentManager.findFragmentById(R.id.contacts_fragment) as ContactsFragment }
    private val callsFragment: CallsFragment by lazy { supportFragmentManager.findFragmentById(R.id.calls_fragment) as CallsFragment }
    private val smsFragment: SmsFragment by lazy { supportFragmentManager.findFragmentById(R.id.sms_fragment) as SmsFragment }
    private val dpiFragment: DpiFragment by lazy { supportFragmentManager.findFragmentById(R.id.dpi_fragment) as DpiFragment }
    private val fontScaleFragment: FontScaleFragment by lazy { supportFragmentManager.findFragmentById(R.id.font_scale_fragment) as FontScaleFragment }
    private val adbFragment: AdbFragment by lazy { supportFragmentManager.findFragmentById(R.id.adb_fragment) as AdbFragment }
    private val keyboardFragment: KeyboardFragment by lazy { supportFragmentManager.findFragmentById(R.id.keyboard_fragment) as KeyboardFragment }

    private val dialogView by lazy { View.inflate(this, R.layout.please_wait, null) }
    private val waitingDialog by lazy {
        AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
                .apply {
                    tryIt {
                        this.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
    }

    private val progressReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                startActivity(Intent(this@ExtraBackupsKotlin_legacy, ProgressShowActivity::class.java)   /*kotlin*/
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

    private fun launchMdpSetup(){
        startActivityForResult(Intent(this, MDP_Setup::class.java), SETUP_MDP_JOBCODE)
    }

    private fun checkMdpLayout(show: Boolean? = null){

        fun show(){
            mdp_setup_layout.visibility = View.VISIBLE
            mdp_why.apply {
                paintFlags = Paint.UNDERLINE_TEXT_FLAG
                setOnClickListener {
                    TODO("add link to help")
                }
            }
            mdp_setup.setOnClickListener {
                launchMdpSetup()
            }
        }

        fun hide() { mdp_setup_layout.visibility = View.GONE }

        if (show == true) show()
        else if (show == false) hide()
        else {
            if (selectedBackupDataPackets.filter { it.DATA }.isEmpty() || IS_OTHER_APP_DATA_VISIBLE) { hide(); return }
            if (Misc.isPackageInstalled(MDP_Constants.MDP_PACKAGE_NAME)) hide()
            else show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.extra_backups)

        destination = getPrefString(PREF_DEFAULT_BACKUP_PATH, DEFAULT_INTERNAL_STORAGE_DIR)
        isAllAppsSelected = intent.getBooleanExtra(EXTRA_IS_ALL_APP_SELECTED, false)

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_BACKUP_PROGRESS))
        commonTools.LBM?.sendBroadcast(Intent(ACTION_REQUEST_BACKUP_DATA))

        checkMdpLayout()

        // Disable wifi backup for now because it is unstable.
        //wifi_main_item.visibility = View.GONE
        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
        //    wifi_main_item.visibility = View.GONE

        startBackupButton.setOnClickListener {

            val allReadTasks = arrayOf(
                    //readSms,
                    //readCalls,
                    //readDpi,
                    //readAdb,
                    //readWifi,
                    //readFontScale
            )

            var isAnyRunning = false
            for (t in allReadTasks.indices){
                allReadTasks[t]?.let {
                    if (it.status == AsyncCoroutineTask.RUNNING) isAnyRunning = true
                }
                if (isAnyRunning) break
            }

            if (!isAnyRunning) {
                if (selectedBackupDataPackets.isNotEmpty()
                    || contactsFragment.isChecked() == true || callsFragment.isChecked() == true
                    || smsFragment.isChecked() == true || dpiFragment.isChecked() == true
                    || keyboardFragment.isChecked() == true
                    || adbFragment.isChecked() == true //|| do_backup_wifi.isChecked
                    || fontScaleFragment.isChecked() == true) {                  //extras_markers

                    if (IS_OTHER_APP_DATA_VISIBLE || selectedBackupDataPackets.filter { it.DATA }.isEmpty())
                        askForName()
                    else if (Misc.isPackageInstalled(MDP_Constants.MDP_PACKAGE_NAME))
                        askForName()
                    else {
                        val ad = AlertDialog.Builder(this).apply {
                            setTitle(R.string.mdp_not_set_up)
                            setMessage(R.string.mdp_not_set_up_desc)
                            setNegativeButton(R.string.proceed_without_mdp) {_, _ -> askForName()}
                            setPositiveButton(R.string.set_up_mdp) {_, _ -> launchMdpSetup()}
                            setNeutralButton(android.R.string.cancel, null)
                        }.create()
                        tryIt {
                            ad.setOnShowListener {
                                ad.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                                    tryIt { setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_no_mdp, 0, 0, 0) }
                                    tryIt { compoundDrawablePadding = 15 }
                                    tryIt { setTextColor(getColorFromRes(R.color.error_color)) }
                                }
                            }
                        }
                        ad.show()
                    }
                }
                else {
                    Toast.makeText(this, R.string.nothing_to_backup, Toast.LENGTH_SHORT).show()
                }
            } else {
                AlertDialog.Builder(this)
                        .setTitle(R.string.wait_while_reading_data)
                        .setMessage(R.string.wait_while_reading_data_desc)
                        .setPositiveButton(R.string.close, null)
                        .show()
            }
        }

                 //extras_markers
        //do_backup_sms.setOnCheckedChangeListener(this)
        //do_backup_calls.setOnCheckedChangeListener(this)
        //do_backup_dpi.setOnCheckedChangeListener(this)
        //do_backup_keyboard.setOnCheckedChangeListener(this)
        //do_backup_installers.setOnCheckedChangeListener(this)
        //do_backup_adb.setOnCheckedChangeListener(this)
        //do_backup_wifi.setOnCheckedChangeListener(this)
        //do_backup_fontScale.setOnCheckedChangeListener(this)

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
                /*isSmsAndCallsGranted ->
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS,
                            Manifest.permission.READ_CALL_LOG), SMS_AND_CALLS_PERMISSION)
                isCallsGranted -> do_backup_calls.isChecked = true
                isSmsGranted -> do_backup_sms.isChecked = true*/
            }
            //do_backup_installers.isChecked = getPrefBoolean(PREF_BACKUP_INSTALLERS, true)
            if (!getPrefBoolean(PREF_SHOW_STOCK_WARNING, true)){
                //do_backup_adb.isChecked = getPrefBoolean(PREF_BACKUP_ADB, false)
                //do_backup_dpi.isChecked = getPrefBoolean(PREF_BACKUP_DPI, false)
                //do_backup_fontScale.isChecked = getPrefBoolean(PREF_BACKUP_FONTSCALE, false)
            }
        }

        if (selectedBackupDataPackets.isEmpty())
            no_app_selected_label.text = getString(R.string.no_app_selected)
        else no_app_selected_label.text = getString(R.string.apps_selected) + " ${selectedBackupDataPackets.size}/${appBackupDataPackets.size}"

        no_app_selected_label.setOnClickListener {
            if (selectedBackupDataPackets.size != appBackupDataPackets.size){
                appBackupDataPackets.filter {
                    selectedBackupDataPackets.none { sp -> sp.PACKAGE_INFO.packageName == it.PACKAGE_INFO.packageName }
                }.let {
                    var list = getString(R.string.some_apps_are_not_allowed) + "\n\n" + getString(R.string.not_selected) + ":" +"\n\n"
                    it.forEach {
                        list += packageManager.getApplicationLabel(it.PACKAGE_INFO.applicationInfo).toString() + "\n"
                    }
                    AlertDialog.Builder(this)
                            .setTitle(R.string.not_selected)
                            .setMessage(list)
                            .setPositiveButton(R.string.close, null)
                            .show()
                }
            }
            else Toast.makeText(this, R.string.all_selected, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mdpSetFromActivity) mdpSetFromActivity = false
        else checkMdpLayout()
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

        /*if (buttonView == do_backup_contacts){

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

        } else*/ /*if (buttonView == do_backup_sms) {

            if (isChecked) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_SMS),
                        SMS_PERMISSION)

            } else deselectExtra(smsList, sms_main_item, sms_selected_status, readSms, sms_read_progress)

            putPrefBoolean(PREF_BACKUP_SMS, isChecked)

        }*/ /*else if (buttonView == do_backup_calls) {

            if (isChecked) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.READ_CALL_LOG),
                        CALLS_PERMISSION)
            } else deselectExtra(callsList, calls_main_item, calls_selected_status, readCalls, calls_read_progress)

            putPrefBoolean(PREF_BACKUP_CALLS, isChecked)

        }*/ /*if (buttonView == do_backup_dpi) {
            if (isChecked) {

                showStockWarning({
                    doWaitingJob {
                        readDpi = ReadDpiKotlin_legacy(JOBCODE_READ_DPI, this, dpi_main_item, dpi_selected_status, dpi_read_progress, do_backup_dpi)
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

        }*/  /*if (buttonView == do_backup_adb) {
            if (isChecked) {

                showStockWarning({
                    doWaitingJob{
                        readAdb = ReadAdbKotlin_legacy(JOBCODE_READ_ADB, this, adb_main_item, adb_selected_status, adb_read_progress, do_backup_adb)
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

        } else*/ /*if (buttonView == do_backup_keyboard) {

            if (isChecked) {
                doWaitingJob {
                    loadKeyboard = LoadKeyboardForSelection_legacy(JOBCODE_LOAD_KEYBOARDS, this, keyboard_main_item, keyboard_selected_status, do_backup_keyboard)
                    loadKeyboard?.execute()
                }
            }
            else {
                keyboardText = null
                deselectExtra(null, keyboard_main_item, keyboard_selected_status, loadKeyboard)
            }

        } else*/ /*if (buttonView == do_backup_installers){
            if (isChecked){
                updateInstallers(selectedBackupDataPackets, true)
            }
            else deselectExtra(null, installers_main_item, installer_selected_status, loadInstallers)

            doBackupInstallers = isChecked
            putPrefBoolean(PREF_BACKUP_INSTALLERS, isChecked)

        } else*/ /*if (buttonView == do_backup_wifi){
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
            }*/
        } /*else if (buttonView == do_backup_fontScale) {
            if (isChecked){

                showStockWarning({
                    doWaitingJob {
                        readFontScale = ReadFontScaleKotlin_legacy(JOBCODE_READ_FONTSCALE, this, fontScale_main_item, fontScale_selected_status, fontScale_read_progress, do_backup_fontScale)
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
        }*/

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {     //extras_markers
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        /*if (requestCode == SMS_AND_CALLS_PERMISSION){

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
        }*/
        /*else if (requestCode == CONTACT_PERMISSION){

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

        }*/
        /*if (requestCode == SMS_PERMISSION){

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                tryIt({
                    doWaitingJob {
                        readSms = ReadSmsKotlin_legacy(JOBCODE_READ_SMS, this, sms_main_item, sms_selected_status, sms_read_progress, do_backup_sms)
                        readSms?.execute()
                    }
                }, true)

            }
            else {
                Toast.makeText(this, R.string.sms_access_needed, Toast.LENGTH_SHORT).show()
                do_backup_sms.isChecked = false
            }

        }*/
        /*else if (requestCode == CALLS_PERMISSION){

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                tryIt({
                    doWaitingJob {
                        readCalls = ReadCallsKotlin_legacy(JOBCODE_READ_CALLS, this, calls_main_item, calls_selected_status, calls_read_progress, do_backup_calls)
                        readCalls?.execute()
                    }
                }, true)

            }
            else {
                Toast.makeText(this, R.string.calls_access_needed, Toast.LENGTH_SHORT).show()
                do_backup_calls.isChecked = false
            }

        }*/
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun askForName(previousName: String? = null) {

        val backupName = if (previousName == null) {
            val sdf = SimpleDateFormat("yyyy.MM.dd_HH.mm.ss")
            if (isAllAppsSelected && smsFragment.isChecked() == true && callsFragment.isChecked() == true)              //extras_markers
                "${getString(R.string.fullBackupLabel)}_${sdf.format(Calendar.getInstance().time)}"
            else "${getString(R.string.backupLabel)}_${sdf.format(Calendar.getInstance().time)}"
        }
        else previousName

        startActivityForResult(Intent(this, AskForName::class.java).apply {
            putExtra(EXTRA_BACKUP_NAME, backupName)
        }, ASK_FOR_NAME_JOBCODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ASK_FOR_NAME_JOBCODE){
            if (resultCode == Activity.RESULT_OK) {

                backupName = data?.getStringExtra(EXTRA_BACKUP_NAME) ?: ""

                fun validateName() {
                    backupName.trim()
                        .replace("[\\\\/:*?\"'<>|]".toRegex(), " ")
                        .replace(' ', '_').run {

                            if (this != "") {

                                val dir = if (FileXInit.isTraditional) FileX.new("$destination/$this") else FileX.new(this)
                                val zip = if (FileXInit.isTraditional) FileX.new("$destination/$this.zip") else FileX.new("$this.zip")

                                fun startBackup() {
                                    backupName = this
                                    waitingDialog.show()

                                    makeAppPackets = MakeAppPackets(JOBCODE_MAKE_APP_PACKETS, this@ExtraBackupsKotlin_legacy, destination, dialogView, flasherOnlyBackup)
                                    makeAppPackets?.execute()
                                }

                                if (dir.exists() || zip.exists()) {
                                    AlertDialog.Builder(this@ExtraBackupsKotlin_legacy)
                                            .setTitle(R.string.overwrite)
                                            .setMessage(R.string.overwriteMessage)
                                            .setPositiveButton(R.string.yes) { _, _ ->
                                                if (dir.exists() && CommonToolsKotlin.isDeletable(dir)) dir.deleteRecursively()
                                                if (zip.exists()) zip.delete()
                                                startBackup()
                                            }
                                            .setNegativeButton(getString(R.string.rename)) {_, _ ->
                                                askForName(backupName)
                                            }
                                            .show()
                                } else {
                                    startBackup()
                                }
                            } else {
                                Toast.makeText(this@ExtraBackupsKotlin_legacy, R.string.backupNameCannotBeEmpty, Toast.LENGTH_SHORT).show()
                                askForName()
                            }
                        }
                }
                validateName()
            }
        }
        else if (requestCode == SETUP_MDP_JOBCODE){
            mdpSetFromActivity = true
            if (resultCode == Activity.RESULT_OK) checkMdpLayout(false)
            else {
                checkMdpLayout(true)
                val error: String? = data?.getStringExtra(MDP_Constants.EXTRA_ERRORS)
                if (error != null) {
                    AlertDialog.Builder(this).apply {
                        setTitle(R.string.error_occurred)
                        setMessage(error)
                        setPositiveButton(R.string.close, null)
                    }.show()
                }
            }
        }
    }

    override fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any?) {           //extras_markers

        /*fun sms(){
            if (jobSuccess) {
                tryIt({
                    updateSms(jobResult as ArrayList<SmsDataPacketKotlin>)
                }, true)
            } else {
                do_backup_sms.isChecked = false
                jobResult.toString().let {
                    showErrorDialog(it, getString(R.string.error_reading_sms))
                }
            }}*/

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

        /*if (jobCode != JOBCODE_READ_SMS_THEN_CALLS)
            toggleBackupButton(1)*/

        when (jobCode){

            /*JOBCODE_READ_CONTACTS ->
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
                }*/

            //JOBCODE_READ_SMS -> sms()

            /*JOBCODE_READ_SMS_THEN_CALLS -> {
                sms()
                do_backup_calls.isChecked = true
            }*/

            /*JOBCODE_LOAD_SMS ->
                if (jobSuccess){
                    tryIt({
                        updateSms(jobResult as ArrayList<SmsDataPacketKotlin>)
                    }, true)
                }*/

            /*JOBCODE_READ_CALLS ->
                if (jobSuccess) {
                    tryIt({
                        updateCalls(jobResult as ArrayList<CallsDataPacketsKotlin>)
                    }, true)
                } else {
                    do_backup_calls.isChecked = false
                    jobResult.toString().let {
                        showErrorDialog(it, getString(R.string.error_reading_calls))
                    }
                }*/

            /*JOBCODE_LOAD_CALLS ->
                if (jobSuccess){
                    tryIt({
                        updateCalls(jobResult as ArrayList<CallsDataPacketsKotlin>)
                    }, true)
                }*/

            /*JOBCODE_READ_DPI ->
                tryIt ({
                    jobResult.toString().let {
                        if (jobSuccess) dpiText = it
                        else {
                            do_backup_dpi.isChecked = false
                            showErrorDialog(it, getString(R.string.error_reading_dpi))
                        }
                    }
                }, true)*/


            /*JOBCODE_READ_ADB ->
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
                }, true)*/

            /*JOBCODE_LOAD_KEYBOARDS -> {
                tryIt({
                    jobResult.toString().let {
                        keyboardText = if (jobSuccess) it
                        else null
                    }
                }, true)
            }*/

            /*JOBCODE_LOAD_INSTALLERS ->
                if (jobSuccess) {
                    tryIt ({
                        updateInstallers(jobResult as ArrayList<BackupDataPacketKotlin>)
                    }, true)
                }*/

            /*JOBCODE_READ_WIFI ->
                tryIt ({
                    if (jobSuccess) {
                        wifiData = jobResult as WifiDataPacket
                        wifi_selected_status.text = wifiData?.fileName
                    }
                    else {
                        do_backup_wifi.isChecked = false
                        showErrorDialog(jobResult.toString(), getString(R.string.error_reading_wifi))
                    }
                }, true)*/

            /*JOBCODE_READ_FONTSCALE ->
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
                }, true)*/

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

    /*private fun updateContacts(newList: ArrayList<ContactsDataPacketKotlin>){

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
    }*/

    /*private fun updateSms(newList: ArrayList<SmsDataPacketKotlin>){

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
                LoadSmsForSelectionKotlin_legacy(JOBCODE_LOAD_SMS, this, smsList).execute()
            }
        }
        else {
            AlertDialog.Builder(this)
                    .setMessage(R.string.empty_sms)
                    .setPositiveButton(R.string.close) {_, _ -> do_backup_sms.isChecked = false }
                    .setCancelable(false)
                    .show()
        }
    }*/

    /*private fun updateCalls(newList: ArrayList<CallsDataPacketsKotlin>){

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
    }*/

    /*private fun updateInstallers(newAppList: ArrayList<BackupDataPacketKotlin>, selfCall: Boolean = false){

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
            }

            installer_selected_status.visibility = View.VISIBLE
            installer_selected_status.text = "$n ${getString(R.string.of)} ${selectedBackupDataPackets.size}"
            installers_main_item.isClickable = true
            installers_main_item.setOnClickListener {
                doWaitingJob {
                    loadInstallers = LoadInstallersForSelection_legacy(JOBCODE_LOAD_INSTALLERS, this, selectedBackupDataPackets)
                    loadInstallers?.execute()
                }
            }
        }

    }*/

    override fun onDestroy() {                          //extras_markers
        super.onDestroy()

        tryIt { waitingDialog.dismiss() }

        tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }

        tryIt { makeAppPackets?.cancel(true) }
        //tryIt { readContacts?.cancel(true) }
        //tryIt { readSms?.cancel(true) }
        //tryIt { readCalls?.cancel(true) }
        //tryIt { readDpi?.cancel(true) }
        //tryIt { readAdb?.cancel(true) }
        //tryIt { readWifi?.cancel(true) }
        //tryIt { readFontScale?.cancel(true) }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, BackupActivityKotlin::class.java))
    }

}