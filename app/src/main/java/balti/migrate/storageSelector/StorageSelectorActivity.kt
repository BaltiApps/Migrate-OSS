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
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEBUG_TAG
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_STORAGE_TYPE
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefString
import kotlinx.android.synthetic.main.storage_selector.*


class StorageSelectorActivity: AppCompatActivity() {

    private val ALL_FILES_ACCESS_PERMISSION = "android.permission.MANAGE_EXTERNAL_STORAGE"
    private var isOnlySafAvailable = false
    private val defaultInternalStorage = DEFAULT_INTERNAL_STORAGE_DIR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.storage_selector)

        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        setFinishOnTouchOutside(false)

        getManifestPermissions().run {

            /**
             * 1st case for all older androids where All Files access is not present.
             * i.e. [ALL_FILES_ACCESS_PERMISSION] is not applicable.
             * Show conventional storage access prompt, i.e. simple file permission dialog.
             */
            if (!CommonToolsKotlin.IS_API_A11) {
                storage_select_all_files_access.visibility = View.GONE
                storage_select_conventional.visibility = View.VISIBLE
            }

            /**
             * 2nd case: This will be false if Google decides to deny all files access.
             * In that case [ALL_FILES_ACCESS_PERMISSION] will need to be removed from the manifest.
             * Then this check will be false.
             * Hence hide the option of All Files access.
             */
            else if (contains(ALL_FILES_ACCESS_PERMISSION)){
                storage_select_all_files_access.visibility = View.VISIBLE
                storage_select_conventional.visibility = View.GONE
            }

            /**
             * 3rd case: For 2nd case fail i.e.
             * - conventional storage not possible
             * - all files access denied by Google.
             * Only option is SAF.
             */
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

    private val onCancelDialogListener = DialogInterface.OnClickListener { _, _ -> if (isOnlySafAvailable) sendResult(false) }

    /**
     * Function to handle logic if user selects conventional storage.
     * Visible only on Android 10 and below.
     */
    private fun conventionalStorageRequest(){
        FileXInit.setTraditional(true)
        FileXInit.requestUserPermission(reRequest = true) { resultCode, data ->
            if (resultCode == Activity.RESULT_OK){
                val root = FileX.new(defaultInternalStorage).apply {
                    tryIt { mkdirs() }
                }
                if (root.canWrite()){
                    val handleSdSelector = HandleSdSelector(this)
                    val rootView = handleSdSelector.getView()
                    /**
                     * This view has the option for [Internal Storage]/Migrate by default.
                     * This view can also show available SD cards,
                     * i.e. the SD cards which are writable using the ExSDCard Access Enabler Magisk module.
                     */
                    AlertDialog.Builder(this).apply {
                        setView(rootView)
                        setPositiveButton(android.R.string.ok){_, _ ->
                            sendResult(true, StorageType.CONVENTIONAL, handleSdSelector.getStoragePath())
                        }
                    }.show()
                }
                else {
                    AlertDialog.Builder(this).apply {
                        setTitle(R.string.write_not_allowed)
                        setMessage(root.canonicalPath + "\n\n" + getString(R.string.please_select_any_other_location))
                        setNegativeButton(R.string.close, null)
                    }.show()
                }
            }
        }
    }

    /**
     * Function to handle logic if user selects Storage Access Framework.
     */
    private fun safStorageRequest(){

        fun safStorageValidator(){
            FileXInit.setTraditional(false)
            FileXInit.requestUserPermission(reRequest = true) { resultCode, data ->

                if (resultCode == Activity.RESULT_OK) {

                    val root = FileX.new("/")

                    if (root.volumePath.isNullOrBlank()) {
                        AlertDialog.Builder(this).apply {
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
        }
                .show()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun isAllFilesAccessGranted() = Environment.isExternalStorageManager()

    /**
     * Function to handle logic if user select All Files Access.
     * Available only from Android 11 and above.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun allFilesAccess(){

        /**
         * If All Files Access is already granted,
         * then this shows layout to select a custom location for backup.
         */
        if (isAllFilesAccessGranted()){
            val allFilesAccessHandler = AllFilesAccessHandler(this, defaultInternalStorage)
            allFilesAccessHandler.showCustomStorageDialog {
                sendResult(true, StorageType.ALL_FILES_STORAGE, it)
            }
        }

        /**
         * If not granted, then launch intent to ask for permission.
         */
        else {
            val allFilesRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                if (isAllFilesAccessGranted()) {
                    allFilesAccess()
                }
            }
            allFilesRequestLauncher.launch(
                Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }

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

    /**
     * Send activity result to calling activity.
     * Then close this activity.
     */
    private fun sendResult(success: Boolean = false, storageType: StorageType? = null, storagePath: String = defaultInternalStorage){
        Log.d(DEBUG_TAG, "Storage send result: $success, ${storageType?.value}, $storagePath")
        if (success){
            storageType?.value?.let { putPrefString(PREF_STORAGE_TYPE, it) }
            putPrefString(PREF_DEFAULT_BACKUP_PATH, storagePath, true)
            setResult(Activity.RESULT_OK)
            Toast.makeText(this, getString(R.string.selected_storage_path) + " : " + storagePath, Toast.LENGTH_SHORT).show()
        }
        else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }

    override fun onBackPressed() {
        sendResult(false)
    }
}