package balti.migrate.storageSelector

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import balti.filex.FileX
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin

object StorageDisplayUtils {

    fun showSdCardSupportDialog(context: Context) {
        context.let {
            AlertDialog.Builder(it)
                    .setView(View.inflate(it, R.layout.learn_about_sd_card, null))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }
    }

    private val defaultInternalStorage = CommonToolsKotlin.DEFAULT_INTERNAL_STORAGE_DIR

    // Return extra part after the canonical path of defaultInternalStorage.
    // Returns null if pathToCheck is not a subdirectory of defaultInternalStorage.
    fun getSubdirectoryForInternalStorage(pathToCheck: String): String? {
        val fullLocationToEvaluate = FileX.new(pathToCheck, true).canonicalPath
        val defaultInternalStorageFull = FileX.new(defaultInternalStorage, true).canonicalPath

        return if (!fullLocationToEvaluate.startsWith(defaultInternalStorageFull)) null
        else fullLocationToEvaluate.substring(defaultInternalStorageFull.length)
    }
}