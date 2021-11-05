package balti.migrate.extraBackupsActivity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.AppInstance.Companion.appBackupDataPackets
import balti.migrate.AppInstance.Companion.selectedBackupDataPackets
import balti.migrate.R
import balti.migrate.backupActivity.BackupActivityKotlin
import balti.migrate.extraBackupsActivity.apps.AppSizeCalculationActivity
import balti.migrate.extraBackupsActivity.engines.adb.AdbFragment
import balti.migrate.extraBackupsActivity.engines.calls.CallsFragment
import balti.migrate.extraBackupsActivity.engines.contacts.ContactsFragment
import balti.migrate.extraBackupsActivity.engines.dpi.DpiFragment
import balti.migrate.extraBackupsActivity.engines.fontScale.FontScaleFragment
import balti.migrate.extraBackupsActivity.engines.installers.InstallersFragment
import balti.migrate.extraBackupsActivity.engines.keyboard.KeyboardFragment
import balti.migrate.extraBackupsActivity.engines.sms.SmsFragment
import balti.migrate.simpleActivities.ProgressShowActivity_new
import balti.migrate.utilities.BackupProgressNotificationSystem
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_CANONICAL_DESTINATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FILEX_DESTINATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_FLASHER_ONLY
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_IS_ALL_APP_SELECTED
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_AUTOSELECT_EXTRAS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_ADB
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_CALLS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_DPI
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_FONTSCALE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_INSTALLERS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_KEYBOARD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_BACKUP_SMS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SHOW_STOCK_WARNING
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefBoolean
import kotlinx.android.synthetic.main.extra_backups.*
import java.text.SimpleDateFormat
import java.util.*

class ExtraBackupsKotlin: AppCompatActivity(R.layout.extra_backups) {

    private val commonTools by lazy { CommonToolsKotlin(this) }

    private lateinit var canonicalDestination: String
    private var backupName = ""
    private var flasherOnly = false
    private var isAllAppsSelected = true

    private val contactsFragment: ContactsFragment by lazy { supportFragmentManager.findFragmentById(R.id.contacts_fragment) as ContactsFragment }
    private val callsFragment: CallsFragment by lazy { supportFragmentManager.findFragmentById(R.id.calls_fragment) as CallsFragment }
    private val smsFragment: SmsFragment by lazy { supportFragmentManager.findFragmentById(R.id.sms_fragment) as SmsFragment }
    private val dpiFragment: DpiFragment by lazy { supportFragmentManager.findFragmentById(R.id.dpi_fragment) as DpiFragment }
    private val fontScaleFragment: FontScaleFragment by lazy { supportFragmentManager.findFragmentById(R.id.font_scale_fragment) as FontScaleFragment }
    private val adbFragment: AdbFragment by lazy { supportFragmentManager.findFragmentById(R.id.adb_fragment) as AdbFragment }
    private val keyboardFragment: KeyboardFragment by lazy { supportFragmentManager.findFragmentById(R.id.keyboard_fragment) as KeyboardFragment }
    private val installersFragment: InstallersFragment by lazy { supportFragmentManager.findFragmentById(R.id.installer_fragment) as InstallersFragment }

    private val askForNameLauncher by lazy {
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                flasherOnly = it.data?.getBooleanExtra(EXTRA_FLASHER_ONLY, false) ?: false
                validateNameAndStartBackup(it.data?.getStringExtra(EXTRA_BACKUP_NAME) ?: "")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        canonicalDestination = getPrefString(PREF_DEFAULT_BACKUP_PATH, DEFAULT_INTERNAL_STORAGE_DIR)
        isAllAppsSelected = intent.getBooleanExtra(EXTRA_IS_ALL_APP_SELECTED, false)

        startBackupButton.setOnClickListener {
            if (selectedBackupDataPackets.isNotEmpty()
                || contactsFragment.isChecked() == true
                || callsFragment.isChecked() == true
                || smsFragment.isChecked() == true
                || dpiFragment.isChecked() == true
                || keyboardFragment.isChecked() == true
                || adbFragment.isChecked() == true
                || fontScaleFragment.isChecked() == true) {

                /**
                 * something is selected. Hence ask for backup name.
                 */
                askForName()
            }
            else {
                Toast.makeText(this, R.string.nothing_to_backup, Toast.LENGTH_SHORT).show()
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

        /**
         * A top bar showing the list of apps not selected for backup.
         * Useful if user has thought that he/she has selected all the apps but some went missing.
         */
        no_app_selected_label.apply {

            text = if (selectedBackupDataPackets.isEmpty()) getString(R.string.no_app_selected)
            else "${getString(R.string.apps_selected)} ${selectedBackupDataPackets.size}/${appBackupDataPackets.size}"

            /**
             * Show a list of apps not selected.
             */
            setOnClickListener {
                if (selectedBackupDataPackets.size != appBackupDataPackets.size){
                    appBackupDataPackets.filter {
                        selectedBackupDataPackets.none { sp -> sp.PACKAGE_INFO.packageName == it.PACKAGE_INFO.packageName }
                    }.let {
                        var list = getString(R.string.some_apps_are_not_allowed) + "\n\n" + getString(R.string.not_selected) + ":" +"\n\n"
                        it.forEach {
                            list += packageManager.getApplicationLabel(it.PACKAGE_INFO.applicationInfo).toString() + "\n"
                        }
                        AlertDialog.Builder(this@ExtraBackupsKotlin)
                            .setTitle(R.string.not_selected)
                            .setMessage(list)
                            .setPositiveButton(R.string.close, null)
                            .show()
                    }
                }
                else Toast.makeText(this@ExtraBackupsKotlin, R.string.all_selected, Toast.LENGTH_SHORT).show()
            }
        }

        askForNameLauncher  // just call to initialise
        autoSelectExtras()

        listenForUpdatesToStartProgressActivity()
    }

    /**
     * Function to immediately start [ProgressShowActivity_new] if any update is received.
     */
    private fun listenForUpdatesToStartProgressActivity(){
        lifecycleScope.launchWhenStarted {
            BackupProgressNotificationSystem.addListener(false, true){
                startActivity(Intent(this@ExtraBackupsKotlin, ProgressShowActivity_new::class.java))
                finish()
            }
        }
    }

    /**
     * Auto select the extras which were selected during last backup.
     * See [storeExtraSelections] to check how they are stored.
     */
    private fun autoSelectExtras(){
        if (getPrefBoolean(PREF_AUTOSELECT_EXTRAS, true)) {

            /** Check calls and SMS first */
            val doSelectSms = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED && getPrefBoolean(PREF_BACKUP_SMS, false)
            val doSelectCalls = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED && getPrefBoolean(PREF_BACKUP_CALLS, false)
            val doSelectSmsAndCalls = doSelectSms && doSelectCalls

            when {

                /**
                 * If calls and SMS were both selected, read SMS first, then calls.
                 * Doing both simultaneously is avoided for performance reasons.
                 */
                doSelectSmsAndCalls -> {
                    smsFragment.apply {

                        /**
                         * After reading SMS is complete, start reading calls.
                         * And make onReaderComplete empty again.
                         */
                        onReaderComplete {
                            callsFragment.checkCheckbox(true)
                            onReaderComplete {  }
                        }
                        checkCheckbox(true)
                    }
                }

                /** Case when SMS was selected last time during backup but not calls.*/
                doSelectSms -> smsFragment.checkCheckbox(true)

                /** Case when calls was selected last time during backup but not SMS.*/
                doSelectCalls -> callsFragment.checkCheckbox(true)

            }

            /** Checking other extras like ADB, Font scale etc.*/
            installersFragment.checkCheckbox(getPrefBoolean(PREF_BACKUP_INSTALLERS, false))
            keyboardFragment.checkCheckbox(getPrefBoolean(PREF_BACKUP_KEYBOARD, false))
            if (!getPrefBoolean(PREF_SHOW_STOCK_WARNING, true)) {
                adbFragment.checkCheckbox(getPrefBoolean(PREF_BACKUP_ADB, false))
                dpiFragment.checkCheckbox(getPrefBoolean(PREF_BACKUP_DPI, false))
                fontScaleFragment.checkCheckbox(getPrefBoolean(PREF_BACKUP_FONTSCALE, false))
            }
        }
    }

    /**
     * Start [AskForName] activity. Send a prefilled name to be shown in the activity.
     * Once the result (backup name) is sent back here, send it to [validateNameAndStartBackup].
     *
     * @param previousName If [validateNameAndStartBackup] finds that a file with the same backup name already exists
     * then if the user chooses to rename the new backup, then the old name will be sent in this parameter.
     * This is useful if (say) the user just wants to add a small change in the backup name
     * without having to type the whole name again.
     */
    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun askForName(previousName: String? = null) {

        val backupName = if (previousName == null) {
            val sdf = SimpleDateFormat("yyyy.MM.dd_HH.mm.ss")
            if (isAllAppsSelected && smsFragment.isChecked() == true && callsFragment.isChecked() == true)              //extras_markers
                "${getString(R.string.fullBackupLabel)}_${sdf.format(Calendar.getInstance().time)}"
            else "${getString(R.string.backupLabel)}_${sdf.format(Calendar.getInstance().time)}"
        }
        else previousName

        askForNameLauncher.launch(Intent(this, AskForName::class.java).apply {
            putExtra(EXTRA_BACKUP_NAME, backupName)
        })
    }

    /**
     * Remove unsupported characters.
     * Check if a file with the same backup name exists.
     *
     * If yes, give the user a choice to overwrite it or enter a new backup name.
     * If no, call [startBackup].
     */
    private fun validateNameAndStartBackup(name: String) {

        name.trim()
            .replace("[\\\\/:*?\"'<>|]".toRegex(), " ")
            .replace(' ', '_').run {

                if (this != "") {

                    val dir = if (FileXInit.isTraditional) FileX.new("$canonicalDestination/$this") else FileX.new(this)
                    val zip = if (FileXInit.isTraditional) FileX.new("$canonicalDestination/$this.zip") else FileX.new("$this.zip")

                    if (dir.exists() || zip.exists()) {
                        AlertDialog.Builder(this@ExtraBackupsKotlin)
                            .setTitle(R.string.overwrite)
                            .setMessage(R.string.overwriteMessage)
                            .setPositiveButton(R.string.yes) { _, _ ->
                                if (dir.exists() && CommonToolsKotlin.isDeletable(dir)) dir.deleteRecursively()
                                if (zip.exists()) zip.delete()
                                startBackup(this)
                            }
                            .setNegativeButton(getString(R.string.rename)) {_, _ ->
                                askForName(backupName)
                            }
                            .show()
                    } else {
                        startBackup(this)
                    }
                } else {
                    Toast.makeText(this@ExtraBackupsKotlin, R.string.backupNameCannotBeEmpty, Toast.LENGTH_SHORT).show()
                    askForName()
                }
            }
    }

    /**
     * Call [AppSizeCalculationActivity] which in turn starts [BackupServiceKotlin].
     */
    private fun startBackup(backupName: String) {
        this.backupName = backupName

        storeExtraSelections()

        startActivity(
            Intent(this@ExtraBackupsKotlin, AppSizeCalculationActivity::class.java).apply {
                putExtra(EXTRA_CANONICAL_DESTINATION, canonicalDestination)
                putExtra(EXTRA_FILEX_DESTINATION, if (FileXInit.isTraditional) canonicalDestination else "")
                putExtra(EXTRA_BACKUP_NAME, backupName)
                putExtra(EXTRA_FLASHER_ONLY, flasherOnly)
            }
        )
    }

    /**
     * Store extra backups options.
     * See [autoSelectExtras] on how they are read.
     */
    private fun storeExtraSelections(){
        if (getPrefBoolean(PREF_AUTOSELECT_EXTRAS, true)) {
            putPrefBoolean(PREF_BACKUP_SMS, smsFragment.isChecked() == true)
            putPrefBoolean(PREF_BACKUP_CALLS, callsFragment.isChecked() == true)
            putPrefBoolean(PREF_BACKUP_INSTALLERS, installersFragment.isChecked() == true)
            putPrefBoolean(PREF_BACKUP_ADB, adbFragment.isChecked() == true)
            putPrefBoolean(PREF_BACKUP_DPI, dpiFragment.isChecked() == true)
            putPrefBoolean(PREF_BACKUP_FONTSCALE, fontScaleFragment.isChecked() == true)
            putPrefBoolean(PREF_BACKUP_KEYBOARD, keyboardFragment.isChecked() == true)
        }
    }
}