package balti.migrate.extraBackupsActivity.installer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ParentFragmentForExtras
import balti.migrate.extraBackupsActivity.ParentReaderForExtras
import balti.migrate.extraBackupsActivity.engines.calls.LoadCallsForSelection

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
            AppInstance.selectedBackupDataPackets.forEach {
                this[it.PACKAGE_INFO.packageName] = it.installerName
            }
        }
    }

    override fun onCreateView(savedInstanceState: Bundle?) {
        delegateCheckbox?.setOnCheckedChangeListener { _, isChecked ->

            mActivity?.run {

                if (isChecked){
                    startReadTask()
                    updateInstallerCount()
                    delegateMainItem?.setOnClickListener {
                        selectorLauncher.launch(Intent(mActivity, LoadCallsForSelection::class.java))
                    }
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

        delegateStatusText?.text = "$count ${getString(R.string.of)} ${AppInstance.selectedBackupDataPackets.size}"
    }

    override val viewIdStatusText: Int = R.id.keyboard_read_text_status
    override val viewIdCheckbox: Int = R.id.keyboard_fragment_checkbox

}