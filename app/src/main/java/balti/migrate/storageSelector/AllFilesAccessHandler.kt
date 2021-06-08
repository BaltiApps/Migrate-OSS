package balti.migrate.storageSelector

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AlertDialog
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString

class AllFilesAccessHandler(private val context: Context, private val defaultInternalStorage: String) {

    private var finalPath = defaultInternalStorage

    private val chooserDialog by lazy {
        AlertDialog.Builder(context).apply {
            setTitle(R.string.all_files_access_custom_location)
            setNeutralButton(R.string.use_default, null)
            setNegativeButton(R.string.choose_custom, null)
            setPositiveButton(android.R.string.ok, null)
            setMessage("")
            setCancelable(false)
        }.create()
    }


    private fun isPathSameAsDefault(pathToCheck: String): Boolean {
        return FileX.new(pathToCheck, true).let {
            val defaultInternalStorageFile = FileX.new(defaultInternalStorage, true)
            it.canonicalPath == defaultInternalStorageFile.canonicalPath
        }
    }

    private fun setChooserDialogMessage(newPath: String? = null) {

        val pathToCheck = newPath ?: getPrefString(PREF_DEFAULT_BACKUP_PATH, defaultInternalStorage)

        val displayLocation: String =
                if (isPathSameAsDefault(pathToCheck)) {
                    finalPath = defaultInternalStorage
                    context.getString(R.string.intenal_storage_location_for_display)
                } else {
                    pathToCheck.apply {
                        finalPath = this
                    }
                }


        val message =
                context.getString(R.string.all_files_access_custom_location_desc1) + "\n\n" +
                displayLocation + "\n\n" +
                context.getText(R.string.all_files_access_custom_location_desc2)
        chooserDialog.setMessage(message)
    }

    private fun showInvalidLocation(){
        AlertDialog.Builder(context).apply {
            setCancelable(false)
            setTitle(R.string.this_location_cannot_be_selected)
            setMessage(R.string.all_files_access_invalid_location)
            setPositiveButton(R.string.close, null)
        }.show()
    }

    private fun startChooser(){
        FileXInit.setTraditional(false)
        FileXInit.requestUserPermission(reRequest = true){ resultCode, _ ->
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

    fun showCustomStorageDialog(onSuccess: (finalPath: String) -> Unit){
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
                    onSuccess(finalPath)
                }
            }
        }
        chooserDialog.show()
    }
}