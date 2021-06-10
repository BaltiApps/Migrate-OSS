package balti.migrate.storageSelector

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import balti.filex.FileX
import balti.filex.FileXInit
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_DEFAULT_BACKUP_PATH
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_STORAGE_TYPE
import balti.module.baltitoolbox.functions.GetResources
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefString

class AllFilesAccessHandler(private val context: Context, private val defaultInternalStorage: String) {

    // Final path to be sent to activity.
    private var finalPath = defaultInternalStorage

    // Alert dialog which allows to choose a custom location.
    // Also shows the selected path.
    private val chooserDialog by lazy {
        AlertDialog.Builder(context).apply {
            setTitle(R.string.all_files_access_custom_location)
            setNeutralButton(R.string.use_default, null)
            setNegativeButton(R.string.custom, null)
            setPositiveButton(android.R.string.ok, null)
            setMessage("")
        }.create()
    }

    // Method to update the message of chooserDialog
    // newPath: A string path to be set in the message of chooserDialog.
    //          Leave blank to get the last selected path from shared preference.
    private fun setChooserDialogMessage(newPath: String? = null) {

        val pathToCheck = newPath ?: getPrefString(PREF_DEFAULT_BACKUP_PATH, defaultInternalStorage)
        val prefType = getPrefString(PREF_STORAGE_TYPE, StorageType.CONVENTIONAL.value)

        // If path begins with /sdcard/Migrate, display the path in terms of [Internal storage]/Migrate.
        // Else show the raw path.
        // Also set the finalPath class variable.
        val displayLocation: String =

                // case if previously SAF type was selected. In that case, use defaultInternalStorage.
                if (prefType != StorageType.ALL_FILES_STORAGE.value) {
                    finalPath = defaultInternalStorage
                    context.getString(R.string.intenal_storage_location_for_display)
                }

                // check if the path is same as or subdirectory of defaultInternalStorage
                else {
                    StorageDisplayUtils.getSubdirectoryForInternalStorage(pathToCheck).let {
                        if (it == null) {
                            pathToCheck.apply {
                                finalPath = this
                            }
                        } else {
                            finalPath = defaultInternalStorage + it
                            context.getString(R.string.intenal_storage_location_for_display) + it
                        }
                    }
                }

        // create the final message and set to chooserDialog
        val message =
                context.getString(R.string.all_files_access_custom_location_desc1) + "\n\n" +
                displayLocation + "\n\n" +
                context.getText(R.string.all_files_access_custom_location_desc2)
        chooserDialog.setMessage(message)
    }

    // Simple dialog to be shown if the selected custom path is invalid.
    // A custom path is invalid if it cannot be written to by a traditional FileX object.
    // Path is usually inaccessible if it is not in Internal storage or SD-CARD.
    private fun showInvalidLocation(){
        AlertDialog.Builder(context).apply {
            setTitle(R.string.this_location_cannot_be_selected)
            setMessage(R.string.all_files_access_invalid_location)
            setPositiveButton(R.string.close, null)
        }.show()
    }

    // Method to start System UI file picker to select a custom path.
    // Also validates the path and set the message in chooserDialog.
    private fun openDocumentsUIForPath(){
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

    // Public method to actually start showing the chooserDialog.
    // onSuccess: Method to execute when the "OK" button is pressed on chooserDialog.
    fun showCustomStorageDialog(onSuccess: (finalPath: String) -> Unit){
        chooserDialog.setOnShowListener {
            chooserDialog.apply {

                // buttons are being set after dialog is shown to
                // prevent the dialog from being dismissed on button press.
                setChooserDialogMessage()

                // Button "Choose custom"
                getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                    openDocumentsUIForPath()
                }

                // Button "Use default"
                getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    setChooserDialogMessage(defaultInternalStorage)
                }

                // Button "OK"
                getButton(AlertDialog.BUTTON_POSITIVE).apply {
                    background = GetResources.getDrawableFromRes(R.drawable.approximate_active_button)
                    setTextColor(GetResources.getColorFromRes(android.R.color.white))
                    setOnClickListener {

                        // check if finalPath class variable can be written to.
                        FileX.new(finalPath, true).let {
                            tryIt { it.mkdirs() }

                            // If can write, then dismiss and send finalPath value.
                            if (it.canWrite()) {
                                dismiss()
                                onSuccess(finalPath)
                            }

                            // Else show a toast message.
                            else {
                                Toast.makeText(context, R.string.all_files_access_please_use_custom_location, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        // Finally show the dialog.
        chooserDialog.show()
    }
}