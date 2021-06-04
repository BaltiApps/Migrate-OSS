package balti.migrate.storageSelector

import android.app.Activity
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.DEFAULT_INTERNAL_STORAGE_DIR
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_STORAGE_TYPE
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

    private fun sendResult(success: Boolean = false, storageType: StorageType? = null, storagePath: String = defaultInternalStorage){
        if (success){
            storageType?.value?.let { putPrefString(PREF_STORAGE_TYPE, it) }
            putPrefString(PREF_DEFAULT_BACKUP_PATH, storagePath)
            setResult(Activity.RESULT_OK)
        }
        else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }
}