package balti.migrate.preferences

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.EditTextPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.provider.Settings
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin.Companion.MIGRATE_CACHE_DEFAULT
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_ALTERNATE_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_AUTOSELECT_EXTRAS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_CALCULATING_SIZE_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_CALLS_VERIFY
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DELETE_ERROR_BACKUP
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_FILELIST_IN_ZIP_VERIFICATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_FORCE_SEPARATE_EXTRAS_BACKUP
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_APP_CACHE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_MANUAL_BUILDPROP
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_MANUAL_MIGRATE_CACHE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_MANUAL_SYSTEM
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_NEW_ICON_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SEPARATE_EXTRAS_BACKUP
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SHOW_BACKUP_SUMMARY
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SMS_VERIFY
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SYSTEM_CHECK
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_TAR_GZ_INTEGRITY
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_TERMINAL_METHOD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_USE_SU_FOR_KEYBOARD
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_ZIP_VERIFICATION

class MainPreferenceActivity: PreferenceActivity() {

    private val showBackupSummary by lazy { findPreference("showBackupSummary") as CheckBoxPreference }
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

    private val suForKeyboard by lazy { findPreference("suForKeyboard") as CheckBoxPreference }
    private val useFileListInZipVerification by lazy { findPreference("useFileListInZipVerification") as CheckBoxPreference }

    private val manualCachePref by lazy { findPreference("manualCachePref") as EditTextPreference }
    private val manualSystemPref by lazy { findPreference("manualSystemPref") as EditTextPreference }
    private val manualBuildpropPref by lazy { findPreference("manualBuildpropPref") as EditTextPreference }

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

            fun setValue(editTextPreference: EditTextPreference, field: String, defaultValue: String){

                fun convertIfBlank(value: String?, defaultValue: String): String {
                    return value.let { if (it == null || it == "") defaultValue else it }
                }

                fun refreshSummary(v: String){
                    editTextPreference.summary = if (v == "") {
                        if (editTextPreference in arrayOf(manualBuildpropPref, manualSystemPref))
                            getString(R.string.detect_while_flashing_hint)
                        else v
                    }
                    else v
                }

                convertIfBlank(getString(field, defaultValue), defaultValue).let {
                    refreshSummary(it)
                    editTextPreference.text = it
                }
                editTextPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    val toStore = convertIfBlank(newValue.toString(), defaultValue)
                    editor.putString(field, toStore)
                    editor.apply()
                    refreshSummary(toStore)
                    editTextPreference.text = toStore
                    false
                }
            }

            setValue(showBackupSummary, PREF_SHOW_BACKUP_SUMMARY)
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

            setValue(suForKeyboard, PREF_USE_SU_FOR_KEYBOARD)
            setValue(useFileListInZipVerification, PREF_FILELIST_IN_ZIP_VERIFICATION)

            setValue(manualCachePref, PREF_MANUAL_MIGRATE_CACHE, MIGRATE_CACHE_DEFAULT)
            setValue(manualSystemPref, PREF_MANUAL_SYSTEM, "")
            setValue(manualBuildpropPref, PREF_MANUAL_BUILDPROP, "")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                useNewSizingMethod.isEnabled = true
                useNewSizingMethod.isChecked = getInt(PREF_CALCULATING_SIZE_METHOD, PREF_ALTERNATE_METHOD) == PREF_ALTERNATE_METHOD
                useNewSizingMethod.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    editor.putInt(PREF_CALCULATING_SIZE_METHOD, if (newValue as Boolean) PREF_ALTERNATE_METHOD else PREF_TERMINAL_METHOD)
                    editor.apply()
                    true
                }
            }
            else {
                useNewSizingMethod.isChecked = false
                useNewSizingMethod.isEnabled = false
                useNewSizingMethod.summary = getString(R.string.only_for_oreo_and_above)
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