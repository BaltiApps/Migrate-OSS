package balti.migrate.extraBackupsActivity

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ALLOW_CONVENTIONAL_STORAGE
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_BACKUP_NAME
import balti.migrate.utilities.CommonToolsKotlin.Companion.PACKAGE_MIGRATE_FLASHER
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_IGNORE_APP_CACHE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SHOW_MANDATORY_FLASHER_WARNING
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_STORAGE_TYPE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_USE_FILEX11
import balti.migrate.utilities.CommonToolsKotlin.Companion.STORAGE_TYPE_CUSTOM_LOCATION
import balti.migrate.utilities.CommonToolsKotlin.Companion.STORAGE_TYPE_INTERNAL_STORAGE
import balti.migrate.utilities.CommonToolsKotlin.Companion.STORAGE_TYPE_SD_CARD_STORAGE
import balti.module.baltitoolbox.functions.GetResources
import balti.module.baltitoolbox.functions.Misc.playStoreLink
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefString
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import kotlinx.android.synthetic.main.ask_for_backup_name.*
import kotlinx.android.synthetic.main.flasher_only_warning.view.*

class AskForName: AppCompatActivity() {

    private var destination: String = ""
    set(value) {
        field = value
        if (value.isBlank()) destination_name.setText(R.string.no_storage_selected)
        else destination_name.text = value
    }
    private val commonTools by lazy { CommonToolsKotlin(this) }
    private var backupName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ask_for_backup_name)

        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)

        destination = getPrefString(PREF_DEFAULT_BACKUP_PATH, "")
        backupName = intent.getStringExtra(EXTRA_BACKUP_NAME) ?: ""

        backup_name_edit_text.isSingleLine = true

        if (ALLOW_CONVENTIONAL_STORAGE) {
            storage_select_radio_group.visibility = View.VISIBLE
            filex_layout.visibility = View.GONE
        }
        else {
            storage_select_radio_group.visibility = View.GONE
            filex_layout.visibility = View.VISIBLE
        }

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
                    checkStorageAndSendPositiveResult()
                }
                return@setOnEditorActionListener false
            }
        }

        if (ALLOW_CONVENTIONAL_STORAGE) {
            storage_select_radio_group.setOnCheckedChangeListener { _, checkedId ->

                // Proceed if the event is for the radio button being checked, not unchecked.
                try {
                    if (!findViewById<RadioButton>(checkedId).isChecked) return@setOnCheckedChangeListener
                } catch (_: Exception){}

                val exSdCardImage = ImageView(this).apply {
                    this.setImageResource(R.drawable.ex_sd_card_enabler)
                    this.setPadding(10, 10, 10, 10)
                }

                when(checkedId){

                    R.id.internal_storage_radio_button -> setTraditionalStorage {
                        putPrefString(PREF_STORAGE_TYPE, STORAGE_TYPE_INTERNAL_STORAGE)
                        setBackupDestination(DEFAULT_INTERNAL_STORAGE_DIR)
                    }

                    R.id.sd_card_radio_button -> setTraditionalStorage {
                        val sdCardProbableDirs = commonTools.getTraditionalSdCardPaths()
                        // the new function is supposed to automatically remove "" (empty files)

                        when (sdCardProbableDirs.size) {
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
                                internal_storage_radio_button.isChecked = true
                            }

                            1 -> {
                                setBackupDestination(sdCardProbableDirs[0] + "/Migrate")
                                putPrefString(PREF_STORAGE_TYPE, STORAGE_TYPE_SD_CARD_STORAGE)
                            }

                            else -> {

                                val sdGroup = RadioGroup(this).apply {
                                    this.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                    this.setPadding(20, 20, 20, 20)
                                }

                                for (i in sdCardProbableDirs.indices) {

                                    val sdFile = FileX.new(sdCardProbableDirs[0], true)
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
                                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                                            internal_storage_radio_button.isChecked = true
                                            setBackupDestination(DEFAULT_INTERNAL_STORAGE_DIR)
                                            putPrefString(PREF_STORAGE_TYPE, STORAGE_TYPE_INTERNAL_STORAGE)
                                        }
                                        .setPositiveButton(android.R.string.cancel) { _, _ ->
                                            setBackupDestination(sdCardProbableDirs[sdGroup.checkedRadioButtonId] + "/Migrate")
                                            putPrefString(PREF_STORAGE_TYPE, STORAGE_TYPE_SD_CARD_STORAGE)
                                        }
                                        .show()

                            }
                        }
                    }

                    R.id.custom_location_radio_button -> setScopedStorage {
                        putPrefString(PREF_STORAGE_TYPE, STORAGE_TYPE_CUSTOM_LOCATION)
                        setBackupDestination(FileX.new("/").absolutePath)
                        location_change_button_for_radio_layout.setOnClickListener {
                            requestScopedStorage({
                                setBackupDestination(FileX.new("/").absolutePath)
                            }, 2)
                        }
                    }
                }
            }

            storage_select_radio_group.apply {
                if (getPrefString(PREF_DEFAULT_BACKUP_PATH, "") != "") {
                    when (getPrefString(PREF_STORAGE_TYPE)) {
                        STORAGE_TYPE_INTERNAL_STORAGE -> internal_storage_radio_button.isChecked = true
                        STORAGE_TYPE_SD_CARD_STORAGE -> {
                            if (FileX.new(getPrefString(PREF_DEFAULT_BACKUP_PATH, "")).canWrite())
                                sd_card_radio_button.isChecked = true
                        }
                        STORAGE_TYPE_CUSTOM_LOCATION -> custom_location_radio_button.isChecked = true
                    }
                }
            }
        }

        filex_layout_change.setOnClickListener {
            requestScopedStorage({
                setBackupDestination(FileX.new("/").absolutePath)
            }, 2)
        }

        cancel_ask_for_name.setOnClickListener {
            sendResult(false)
        }

        start_backup_ask_for_name.setOnClickListener {
            checkStorageAndSendPositiveResult()
        }
    }

    private fun clearStorageRadio(){
        storage_select_radio_group.clearCheck()
        destination = ""
        putPrefString(PREF_DEFAULT_BACKUP_PATH, "")
        putPrefString(PREF_STORAGE_TYPE, "")
        location_change_button_for_radio_layout.visibility = View.GONE
    }

    private fun setTraditionalStorage(functionToPerform: () -> Unit){
        location_change_button_for_radio_layout.visibility = View.GONE
        putPrefBoolean(PREF_USE_FILEX11, false)
        FileXInit.setTraditional(true)
        if (FileXInit.isUserPermissionGranted()){
            functionToPerform()
        }
        else {
            AlertDialog.Builder(this).apply {
                setCancelable(false)
                setMessage(R.string.grant_internal_storage_access)
                setPositiveButton(R.string.grant) {_, _ ->
                    FileXInit.requestUserPermission { resultCode, data ->
                        if (resultCode == Activity.RESULT_OK) functionToPerform()
                        else clearStorageRadio()
                    }
                }
                setNegativeButton(android.R.string.cancel) {_, _ ->
                    clearStorageRadio()
                }
            }
                    .show()
        }
    }

    private fun setScopedStorage(functionToPerform: () -> Unit){
        location_change_button_for_radio_layout.visibility = View.VISIBLE
        putPrefBoolean(PREF_USE_FILEX11, true)
        FileXInit.setTraditional(false)
        if (FileXInit.isUserPermissionGranted() && !FileX.new("/").volumePath.isNullOrBlank()){
            functionToPerform()
        }
        else {
            requestScopedStorage(functionToPerform, 0)
        }
    }

    /**
     * [mode] values:
     * 0 -> show dialog to choose storage
     * 1 -> show dialog about invalid location and choose storage again
     * 2 -> don't show any dialog and directly open Documents UI
     */
    private fun requestScopedStorage(functionToPerform: () -> Unit, mode: Int) {
        fun ask(){
            FileXInit.requestUserPermission (reRequest = true) { resultCode, data ->
                if (resultCode == Activity.RESULT_OK) {
                    if (FileX.new("/").volumePath.isNullOrBlank()) {
                        requestScopedStorage(functionToPerform, 1)
                    } else functionToPerform()
                } else clearStorageRadio()
            }
        }
        if (mode == 2) ask()
        else AlertDialog.Builder(this).apply {
            setCancelable(false)
            if (mode == 1) {
                setTitle(R.string.this_location_cannot_be_selected)
                setMessage(R.string.this_location_cannot_be_selected_desc)
            }
            else {
                setTitle(R.string.choose_storage_location)
                setMessage(R.string.choose_storage_location_desc)
            }
            setPositiveButton(R.string.proceed) { _, _ ->
                ask()
            }
            setNegativeButton(android.R.string.cancel) { _, _ ->
                clearStorageRadio()
            }
        }
                .show()
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

    private fun setBackupDestination(path: String) {

        if (FileXInit.isTraditional) {
            FileX.new(path).let {
                if (!it.exists()) it.mkdirs()
            }
        }

        destination = path
        putPrefString(PREF_DEFAULT_BACKUP_PATH, destination, true)
    }

    private fun highlightStorage(traditionalStorage: Boolean = true){
        class Highlight: AsyncCoroutineTask(){
            override suspend fun doInBackground(arg: Any?): Any? {
                repeat(2) {
                    val millis : Long = 200
                    tryIt { publishProgress(true) }
                    sleepTask(millis)
                    tryIt { publishProgress(false) }
                    sleepTask(millis)
                }
                return null
            }

            override suspend fun onProgressUpdate(vararg values: Any) {
                tryIt {
                    val view = if (traditionalStorage) storage_select_radio_group else filex_layout
                    if (values[0] == true)
                        view.setBackgroundColor(GetResources.getColorFromRes(R.color.less_transparent))
                    else view.setBackgroundColor(Color.TRANSPARENT)
                }
            }
        }
        Highlight().execute()
    }

    private fun sendResult(success: Boolean){
        setResult(if (success) Activity.RESULT_OK else Activity.RESULT_CANCELED, Intent().apply {
            putExtra(EXTRA_BACKUP_NAME, backup_name_edit_text.text.toString())
        })
        finish()
    }

    private fun checkStorageAndSendPositiveResult(){
        if (destination.isBlank()) {
            Toast.makeText(this, R.string.please_select_a_storage_option, Toast.LENGTH_SHORT).show()
            highlightStorage(ALLOW_CONVENTIONAL_STORAGE)
        }
        else {
            sendResult(true)
        }
    }
}