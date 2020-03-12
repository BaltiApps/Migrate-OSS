package balti.migrate.preferences

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.provider.Settings
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_ALTERNATE_METHOD
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_AUTOSELECT_EXTRAS
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_CALCULATING_SIZE_METHOD
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_CALLS_VERIFY
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_DELETE_ERROR_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_FILELIST_IN_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_FORCE_SEPARATE_EXTRAS_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_IGNORE_APP_CACHE
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_NEW_ICON_METHOD
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_SEPARATE_EXTRAS_BACKUP
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_SMS_VERIFY
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_SYSTEM_CHECK
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_TAR_GZ_INTEGRITY
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_TERMINAL_METHOD
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_UPDATE_AUTO_CHECK
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_USE_SU_FOR_KEYBOARD
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_ZIP_VERIFICATION
import balti.updater.Updater

class MainPreferenceActivity: PreferenceActivity() {

    private val useNewSizingMethod by lazy { findPreference("useNewSizingMethod") as CheckBoxPreference }
    private val autoselectExtras by lazy { findPreference("autoselectExtras") as CheckBoxPreference }
    private val newIconMethod by lazy { findPreference("newIconMethod") as CheckBoxPreference }
    private val tarGzIntegrityCheck by lazy { findPreference("tarGzIntegrityCheck") as CheckBoxPreference }
    private val smsVerification by lazy { findPreference("smsVerification") as CheckBoxPreference }
    private val callsVerification by lazy { findPreference("callsVerification") as CheckBoxPreference }
    private val performSystemCheck by lazy { findPreference("performSystemCheck") as CheckBoxPreference }
    private val separateExtras by lazy { findPreference("separateExtras") as CheckBoxPreference }
    private val forceSeparateExtras by lazy { findPreference("forceSeparateExtras") as CheckBoxPreference }
    private val deleteErrorBackup by lazy { findPreference("deleteErrorBackup") as CheckBoxPreference }
    private val zipVerification by lazy { findPreference("zipVerification") as CheckBoxPreference }
    private val ignoreCache by lazy { findPreference("ignoreCache") as CheckBoxPreference }

    private val autoCheckUpdates by lazy { findPreference("autoCheckUpdates") as CheckBoxPreference }
    private val updateHosts by lazy { findPreference("updateHosts") as ListPreference }
    private val updateChannel by lazy { findPreference("updateChannel") as ListPreference }

    private val suForKeyboard by lazy { findPreference("suForKeyboard") as CheckBoxPreference }
    private val useFileListInZipVerification by lazy { findPreference("useFileListInZipVerification") as CheckBoxPreference }

    private val disableBatteryOptimisation by lazy { findPreference("disableBatteryOptimisation") as Preference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        AppInstance.sharedPrefs.run {

            val editor = edit()

            fun setValue(checkbox: CheckBoxPreference, field: String, defaultValue: Boolean = true){

                checkbox.isChecked = getBoolean(field, defaultValue)
                checkbox.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    editor.putBoolean(field, newValue as Boolean)
                    if (field == PREF_SEPARATE_EXTRAS_BACKUP && newValue == false) {
                        forceSeparateExtras.isChecked = false
                        editor.putBoolean(PREF_FORCE_SEPARATE_EXTRAS_BACKUP, false)
                    }
                    editor.apply()
                    true
                }
            }

            setValue(useNewSizingMethod, PREF_AUTOSELECT_EXTRAS)
            setValue(autoselectExtras, PREF_AUTOSELECT_EXTRAS)
            setValue(newIconMethod, PREF_NEW_ICON_METHOD)
            setValue(tarGzIntegrityCheck, PREF_TAR_GZ_INTEGRITY)
            setValue(smsVerification, PREF_SMS_VERIFY)
            setValue(callsVerification, PREF_CALLS_VERIFY)
            setValue(performSystemCheck, PREF_SYSTEM_CHECK)
            setValue(separateExtras, PREF_SEPARATE_EXTRAS_BACKUP)
            setValue(forceSeparateExtras, PREF_FORCE_SEPARATE_EXTRAS_BACKUP, false)
            setValue(deleteErrorBackup, PREF_DELETE_ERROR_BACKUP)
            setValue(zipVerification, PREF_ZIP_VERIFICATION)
            setValue(ignoreCache, PREF_IGNORE_APP_CACHE, false)

            setValue(autoCheckUpdates, PREF_UPDATE_AUTO_CHECK)

            setValue(suForKeyboard, PREF_USE_SU_FOR_KEYBOARD)
            setValue(useFileListInZipVerification, PREF_FILELIST_IN_ZIP_VERIFICATION)

            useNewSizingMethod.isChecked = getInt(PREF_CALCULATING_SIZE_METHOD, PREF_ALTERNATE_METHOD) == PREF_ALTERNATE_METHOD
            useNewSizingMethod.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                editor.putInt(PREF_CALCULATING_SIZE_METHOD, if (newValue as Boolean) PREF_ALTERNATE_METHOD else PREF_TERMINAL_METHOD)
                editor.apply()
                true
            }

            updateHosts.apply {
                Updater.getUpdateHosts().let {
                    entries = it
                    entryValues = it
                    setValueIndex(it.indexOf(Updater.getUpdateActiveHost()))
                }
                onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    Updater.setUpdateActiveHost(newValue.toString())
                    true
                }
            }

            updateChannel.apply {
                Updater.getChannels().let {
                    entries = it
                    entryValues = it
                    setValueIndex(it.indexOf(Updater.getActiveChannel()))
                }
                onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    Updater.setActiveChannel(newValue.toString())
                    true
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            disableBatteryOptimisation.apply {
                isEnabled = true
                setOnPreferenceClickListener {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    return@setOnPreferenceClickListener true
                }
            }
        }
        else disableBatteryOptimisation.isEnabled = false
    }
}