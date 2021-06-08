package balti.migrate.storageSelector

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_STORAGE_TYPE
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefString
import kotlinx.android.synthetic.main.storage_selector.*


class StorageSelectorActivity: AppCompatActivity() {

    private val ALL_FILES_ACCESS_PERMISSION = "android.permission.MANAGE_EXTERNAL_STORAGE"
    private var isOnlySafAvailable = false
    private val defaultInternalStorage = DEFAULT_INTERNAL_STORAGE_DIR
    private val REQUEST_CODE_ALL_FILES_ACCESS = 1045

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.storage_selector)

        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        setFinishOnTouchOutside(false)

        getManifestPermissions().run {

            // 1st case for all older androids where SAF is avoided
            if (CommonToolsKotlin.ALLOW_CONVENTIONAL_STORAGE) {
                storage_select_all_files_access.visibility = View.GONE
                storage_select_conventional.visibility = View.VISIBLE
            }

            // 2nd case: This will be false if Google decides to deny all files access.
            // In that case the permission needs to be removed from manfest, and the check will be false.
            else if (contains(ALL_FILES_ACCESS_PERMISSION)){
                storage_select_all_files_access.visibility = View.VISIBLE
                storage_select_conventional.visibility = View.GONE
            }

            // 3rd case: For 2nd case fail i.e. conventional storage not possible, all files access denied by Google.
            // Only option is SAF.
            else {
                isOnlySafAvailable = true
                storage_select_root_view.visibility = View.GONE
                safStorageRequest()
            }
        }

        storage_select_saf.setOnClickListener {
            safStorageRequest()
        }

        storage_select_conventional.setOnClickListener {
            conventionalStorageRequest()
        }

        storage_select_all_files_access.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                allFilesAccess()
            }
        }
    }

    override fun onBackPressed() {
        sendResult(false)
    }

    private val onCancelDialogListener = DialogInterface.OnClickListener { _, _ -> if (isOnlySafAvailable) sendResult(false) }

    private fun conventionalStorageRequest(){
        FileXInit.setTraditional(true)
        FileXInit.requestUserPermission(reRequest = true) { resultCode, data ->
            if (resultCode == Activity.RESULT_OK){
                val root = FileX.new(defaultInternalStorage).apply {
                    mkdirs()
                }
                if (root.canWrite()){
                    val handleSdSelector = HandleSdSelector(this)
                    val rootView = handleSdSelector.getView()
                    AlertDialog.Builder(this).apply {
                        setView(rootView)
                        setPositiveButton(android.R.string.ok){_, _ ->
                            sendResult(true, StorageType.CONVENTIONAL, handleSdSelector.getStoragePath())
                        }
                        setCancelable(false)
                    }.show()
                }
                else {
                    AlertDialog.Builder(this).apply {
                        setCancelable(false)
                        setTitle(R.string.write_not_allowed)
                        setMessage(root.canonicalPath + "\n\n" + getString(R.string.please_select_any_other_location))
                        setNegativeButton(R.string.close, null)
                    }.show()
                }
            }
        }
    }

    private fun safStorageRequest(){

        fun safStorageValidator(){
            FileXInit.setTraditional(false)
            FileXInit.requestUserPermission(reRequest = true) { resultCode, data ->

                if (resultCode == Activity.RESULT_OK) {

                    val root = FileX.new("/")

                    if (root.volumePath.isNullOrBlank()) {
                        AlertDialog.Builder(this).apply {
                            setCancelable(false)
                            setTitle(R.string.this_location_cannot_be_selected)
                            setMessage(R.string.this_location_cannot_be_selected_desc)
                            setPositiveButton(R.string.proceed) { _, _ ->
                                safStorageValidator()
                            }
                            setNegativeButton(android.R.string.cancel, onCancelDialogListener)
                        }.show()

                    } else {
                        sendResult(true, StorageType.SAF, root.canonicalPath)
                    }
                }
            }
        }

        AlertDialog.Builder(this).apply {
            setTitle(R.string.choose_storage_location)
            setMessage(R.string.choose_storage_location_desc_saf)
            setPositiveButton(R.string.proceed) { _, _ -> safStorageValidator() }
            setNeutralButton(android.R.string.cancel, onCancelDialogListener)
            setCancelable(false)
        }
                .show()
    }

    private fun getManifestPermissions(): List<String>{
        val manifestPermissions = ArrayList<String>(0)
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            packageInfo?.run {
                requestedPermissions.forEach {
                    manifestPermissions.add(it)
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return manifestPermissions
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun isAllFilesAccessGranted() = Environment.isExternalStorageManager()

    @RequiresApi(Build.VERSION_CODES.R)
    private fun allFilesAccess(){

        var finalPath = defaultInternalStorage

        val chooserDialog = AlertDialog.Builder(this).apply {
            setTitle(R.string.all_files_access_custom_location)
            setNeutralButton(R.string.use_default, null)
            setNegativeButton(R.string.choose_custom, null)
            setPositiveButton(android.R.string.ok, null)
            setMessage("")
            setCancelable(false)
        }.create()

        fun setChooserDialogMessage(newPath: String? = null) {

            val pathToCheck = newPath ?: getPrefString(PREF_DEFAULT_BACKUP_PATH, defaultInternalStorage)

            val isPathSameAsDefault = FileX.new(pathToCheck, true).let {
                val defaultInternalStorageFile = FileX.new(defaultInternalStorage, true)
                it.canonicalPath == defaultInternalStorageFile.canonicalPath
            }

            val displayLocation: String =
                    if (isPathSameAsDefault) {
                        finalPath = defaultInternalStorage
                        getString(R.string.intenal_storage_location_for_display)
                    } else {
                        pathToCheck.apply {
                            finalPath = this
                        }
                    }


            val message = getString(R.string.all_files_access_custom_location_desc1) + "\n\n" +
                   displayLocation + "\n\n" +
                   getText(R.string.all_files_access_custom_location_desc2)
            chooserDialog.setMessage(message)
        }

        fun showInvalidLocation(){
            AlertDialog.Builder(this).apply {
                setCancelable(false)
                setTitle(R.string.this_location_cannot_be_selected)
                setMessage(R.string.all_files_access_invalid_location)
                setPositiveButton(R.string.close, null)
            }.show()
        }

        fun startChooser(){
            FileXInit.setTraditional(false)
            FileXInit.requestUserPermission(reRequest = true){resultCode, _ ->
                if (resultCode == Activity.RESULT_OK){
                    val root = FileX.new("/")
                    val rootCanonical = root.canonicalPath
                    if (!root.volumePath.isNullOrBlank() &&
                            FileX.new(rootCanonical, true).canWrite()){
                        setChooserDialogMessage(rootCanonical)
                    }
                    else {
                        showInvalidLocation()
                    }
                }
            }
        }

        fun showCustomStorageDialog(){
            chooserDialog.setOnShowListener {
                chooserDialog.apply {
                    setChooserDialogMessage()
                    getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                        startChooser()
                    }
                    getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        setChooserDialogMessage(defaultInternalStorage)
                    }
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        dismiss()
                        sendResult(true, StorageType.ALL_FILES_STORAGE, finalPath)
                    }
                }
            }
            chooserDialog.show()
        }

        if (isAllFilesAccessGranted()){
            showCustomStorageDialog()
        }
        else {
            startActivityForResult(Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }, REQUEST_CODE_ALL_FILES_ACCESS)
        }

    }

    private fun sendResult(success: Boolean = false, storageType: StorageType? = null, storagePath: String = defaultInternalStorage){
        if (success){
            storageType?.value?.let { putPrefString(PREF_STORAGE_TYPE, it) }
            putPrefString(PREF_DEFAULT_BACKUP_PATH, storagePath)
            setResult(Activity.RESULT_OK)
            Toast.makeText(this, getString(R.string.selected_storage_path) + " : " + storagePath, Toast.LENGTH_SHORT).show()
        }
        else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ALL_FILES_ACCESS){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (isAllFilesAccessGranted()) {
                    allFilesAccess()
                }
            }
        }
    }
}