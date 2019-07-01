package balti.migrate.extraBackupsActivity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import balti.migrate.BackupProgressLayout
import balti.migrate.R
import balti.migrate.backupActivity.BackupActivityKotlin
import balti.migrate.backupActivity.BackupDataPacketKotlin
import balti.migrate.utilities.CommonToolKotlin
import balti.migrate.utilities.CommonToolKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolKotlin.Companion.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolKotlin.Companion.PREF_FILE_MAIN
import balti.migrate.utilities.CommonToolKotlin.Companion.SMS_AND_CALL_PERMISSION
import kotlinx.android.synthetic.main.ask_for_backup_name.view.*
import kotlinx.android.synthetic.main.extra_backups.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ExtraBackupsKotlin : AppCompatActivity(), OnJobCompletion, CompoundButton.OnCheckedChangeListener {

    // On adding extras, alter line 53-54 (onCreate), askForName FullBackupName

    private val commonTools by lazy { CommonToolKotlin(this) }
    private val main by lazy { getSharedPreferences(PREF_FILE_MAIN, Context.MODE_PRIVATE) }
    private val editor by lazy { main.edit() }
    val appListCopied by lazy { ArrayList<BackupDataPacketKotlin>(0) }

    lateinit var destination: String
    private var isAllAppsSelected = true

    private val progressReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                startActivity(Intent(this@ExtraBackupsKotlin, BackupProgressLayout::class.java)   /*kotlin*/
                        .apply {
                            putExtras(this.extras)
                            action = ACTION_BACKUP_PROGRESS
                        }
                )
                try {
                    LocalBroadcastManager.getInstance(this@ExtraBackupsKotlin).unregisterReceiver(this)
                } catch (_: Exception) {}
                finish()
            }
        }
    }

    init {
        try {
            destination = main.getString(PREF_DEFAULT_BACKUP_PATH, DEFAULT_INTERNAL_STORAGE_DIR)
            BackupActivityKotlin.appList.forEach {
                if (it.APP || it.PERMISSION || it.DATA) {
                    appListCopied.add(it)
                    if (isAllAppsSelected && !(it.APP && it.DATA && it.PERMISSION)) isAllAppsSelected = false
                } else if (isAllAppsSelected) isAllAppsSelected = false
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            AlertDialog.Builder(this)
                    .setTitle(R.string.error_occurred)
                    .setMessage(e.message)
                    .setCancelable(false)
                    .setNegativeButton(R.string.close){_, _ ->
                        finish()
                    }
                    .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.extra_backups)

        startBackupButton.setOnClickListener {
            if (appListCopied.size > 0 || do_backup_contacts.isChecked || do_backup_calls.isChecked
                    || do_backup_sms.isChecked || do_backup_dpi.isChecked || do_backup_keyboard.isChecked){
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

        LocalBroadcastManager.getInstance(this).registerReceiver(progressReceiver, IntentFilter(ACTION_BACKUP_PROGRESS))

        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(CommonToolKotlin.ACTION_REQUEST_BACKUP_DATA))

        do_backup_contacts.setOnCheckedChangeListener(this)
        do_backup_sms.setOnCheckedChangeListener(this)
        do_backup_calls.setOnCheckedChangeListener(this)
        do_backup_dpi.setOnCheckedChangeListener(this)
        do_backup_keyboard.setOnCheckedChangeListener(this)

        val isSmsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val isCallsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val isSmsAndCallsGranted = isSmsGranted && isCallsGranted

        when {
            isSmsAndCallsGranted -> ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CALL_LOG), SMS_AND_CALL_PERMISSION)
            isSmsGranted -> do_backup_sms.isChecked = true
            isCallsGranted -> do_backup_calls.isChecked = true
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {

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

                            for (i in 0 until sdCardProbableDirs.size){

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
        if (isAllAppsSelected &&
                do_backup_sms.isChecked &&
                do_backup_calls.isChecked)
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
            setOnEditorActionListener { v, actionId, event ->
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

    override fun onComplete(jobCode: Int, jobSuccess: Boolean, jobResult: Any?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}