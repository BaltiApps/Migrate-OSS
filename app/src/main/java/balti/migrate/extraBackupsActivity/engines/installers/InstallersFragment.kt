package balti.migrate.extraBackupsActivity.engines.installers

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.extraBackupsActivity.utils.ParentFragmentForExtras
import balti.migrate.extraBackupsActivity.utils.ParentReaderForExtras
import balti.migrate.utilities.CommonToolsKotlin

class InstallersFragment: ParentFragmentForExtras(R.layout.extra_fragment_installers) {

    override val readTask: ParentReaderForExtras? = null
    private lateinit var selectorLauncher: ActivityResultLauncher<Intent>

    override fun onCreateFragment() {
        super.onCreateFragment()
        selectorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if (it.resultCode == Activity.RESULT_OK){
                updateInstallerCount()
            }
        }
    }

    private fun startReadTask(){
        AppInstance.appInstallers.apply {
            clear()
            AppInstance.selectedBackupDataPackets.forEach { packet ->
                this[packet.PACKAGE_INFO.packageName] =
                    packet.installerName.let {
                        if (it in CommonToolsKotlin.QUALIFIED_PACKAGE_INSTALLERS.keys) it else ""
                    }
            }
        }
    }

    override fun onCreateView(savedInstanceState: Bundle?) {
        delegateCheckbox?.setOnCheckedChangeListener { _, isChecked ->

            mActivity?.run {

                if (isChecked){
                    startReadTask()
                    updateInstallerCount()
                }
                else {
                    deselectExtra(null, listOf(delegateStatusText))
                    AppInstance.appInstallers.clear()
                }
            }
        }
    }

    private fun updateInstallerCount(){
        var count = 0
        AppInstance.appInstallers.forEach {
            if (it.value.isNotBlank()) count++
        }

        val selectedAppCount = AppInstance.selectedBackupDataPackets.size

        delegateStatusText?.apply {
            visibility = View.VISIBLE
            text = if (selectedAppCount > 0)
                "$count ${getString(R.string.of)} ${AppInstance.selectedBackupDataPackets.size}"
            else getString(R.string.no_app_selected)
        }

        if (selectedAppCount > 0){
            delegateMainItem?.setOnClickListener {
                selectorLauncher.launch(Intent(mActivity, LoadInstallersForSelection::class.java))
            }
        }
    }

    override val viewIdStatusText: Int = R.id.installers_read_text_status
    override val viewIdCheckbox: Int = R.id.installers_fragment_checkbox

}