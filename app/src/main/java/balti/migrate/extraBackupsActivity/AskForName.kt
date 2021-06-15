package balti.migrate.extraBackupsActivity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.R
import balti.migrate.storageSelector.StorageDisplayUtils
import balti.migrate.storageSelector.StorageSelectorActivity
import balti.migrate.storageSelector.StorageType
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolsKotlin.Companion.PACKAGE_MIGRATE_FLASHER
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_APP_CACHE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SHOW_MANDATORY_FLASHER_WARNING
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_STORAGE_TYPE
import balti.module.baltitoolbox.functions.Misc.playStoreLink
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefBoolean
import kotlinx.android.synthetic.main.ask_for_backup_name.*
import kotlinx.android.synthetic.main.flasher_only_warning.view.*

class AskForName: AppCompatActivity() {

    private var destination: String = ""
    private var backupName = ""

    private val REQUEST_CODE_STORAGE_SELECTOR = 57

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ask_for_backup_name)

        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

        destination = getPrefString(PREF_DEFAULT_BACKUP_PATH, "")
        backupName = intent.getStringExtra(EXTRA_BACKUP_NAME) ?: ""

        backup_name_edit_text.isSingleLine = true

        migrate_flasher_only_warning_button.setOnClickListener {
            showFlasherOnlyWarning(false)
        }

        ignore_cache_checkbox.apply {
            isChecked = getPrefBoolean(PREF_IGNORE_APP_CACHE, false)
            setOnCheckedChangeListener { _, isChecked ->
                putPrefBoolean(PREF_IGNORE_APP_CACHE, isChecked, true)
            }
        }

        migrate_flasher_only_checkbox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && getPrefBoolean(PREF_SHOW_MANDATORY_FLASHER_WARNING, true))
                    showFlasherOnlyWarning(true)
                putPrefBoolean(CommonToolsKotlin.PREF_USE_FLASHER_ONLY, isChecked)
            }
            isChecked = !getPrefBoolean(PREF_SHOW_MANDATORY_FLASHER_WARNING, true)
                    && getPrefBoolean(CommonToolsKotlin.PREF_USE_FLASHER_ONLY, false)
        }

        backup_name_edit_text.apply {
            setText(backupName)
            imeOptions = EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE){
                    sendResult(true)
                }
                return@setOnEditorActionListener false
            }
        }

        updateDestinationLabel()

        cancel_ask_for_name.setOnClickListener {
            sendResult(false)
        }

        start_backup_ask_for_name.setOnClickListener {
            sendResult(true)
        }

        change_storage_button.setOnClickListener {
            startActivityForResult(
                    Intent(this, StorageSelectorActivity::class.java),
                    REQUEST_CODE_STORAGE_SELECTOR
            )
        }
    }

    private fun updateDestinationLabel(){
        val label: String = StorageDisplayUtils.getSubdirectoryForInternalStorage(destination).let {
            if (it == null) destination
            else getString(R.string.intenal_storage_location_for_display) + it
        }
        destination_name.text = label

        val storageType = getPrefString(PREF_STORAGE_TYPE, StorageType.CONVENTIONAL.value)
        storage_type.text = when(storageType){
            StorageType.CONVENTIONAL.value -> getString(R.string.label_traditional_storage)
            StorageType.ALL_FILES_STORAGE.value -> getString(R.string.label_all_files_storage)
            StorageType.SAF.value -> getString(R.string.label_saf_storage)
            else -> ""
        }
    }

    private fun showFlasherOnlyWarning(fromPreference: Boolean){

        val flasherWarning = View.inflate(this, R.layout.flasher_only_warning, null)
        flasherWarning.get_flasher_button.setOnClickListener {
            playStoreLink(PACKAGE_MIGRATE_FLASHER)
        }

        val ad = AlertDialog.Builder(this).apply {
            setView(flasherWarning)
            setIcon(R.drawable.ic_error)
        }

        if (fromPreference){
            flasherWarning.flasher_warning_dont_show_again.visibility = View.VISIBLE
            ad.apply {
                setPositiveButton(R.string.proceed) {_, _ ->
                    putPrefBoolean(PREF_SHOW_MANDATORY_FLASHER_WARNING, !flasherWarning.flasher_warning_dont_show_again.isChecked)
                }
                setNegativeButton(R.string.disable_this) { _, _ ->
                    migrate_flasher_only_checkbox.isChecked = false
                }
                setCancelable(false)
            }.show()
        }
        else {
            flasherWarning.flasher_warning_dont_show_again.visibility = View.GONE
            ad.setPositiveButton(R.string.close, null).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_STORAGE_SELECTOR) {
            if (resultCode == Activity.RESULT_OK){
                destination = getPrefString(PREF_DEFAULT_BACKUP_PATH, "")
                updateDestinationLabel()
            }
        }
    }

    private fun sendResult(success: Boolean){
        setResult(if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED, Intent().apply {
            putExtra(EXTRA_BACKUP_NAME, backup_name_edit_text.text.toString())
        })
        finish()
    }
}