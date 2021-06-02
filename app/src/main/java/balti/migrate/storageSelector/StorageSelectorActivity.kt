package balti.migrate.storageSelector

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.R
import kotlinx.android.synthetic.main.storage_selector.*


class StorageSelectorActivity: AppCompatActivity() {

    private val ALL_FILES_ACCESS_PERMISSION = "android.permission.MANAGE_EXTERNAL_STORAGE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.storage_selector)

        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        setFinishOnTouchOutside(false)

        getManifestPermissions().run {
            if (contains(ALL_FILES_ACCESS_PERMISSION)){
                storage_select_all_files_access.visibility = View.VISIBLE
                storage_select_conventional.visibility = View.GONE
            }
            else {
                storage_select_all_files_access.visibility = View.GONE
                storage_select_conventional.visibility = View.VISIBLE
            }
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
}