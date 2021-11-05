package balti.migrate.extraBackupsActivity.engines.sms

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import balti.migrate.AppInstance.Companion.smsList
import balti.migrate.R
import balti.migrate.extraBackupsActivity.engines.sms.containers.SmsDataPacketKotlin
import balti.migrate.extraBackupsActivity.utils.ParentFragmentForExtras
import balti.migrate.extraBackupsActivity.utils.ParentReaderForExtras
import balti.module.baltitoolbox.functions.Misc.runOnMainThread
import balti.module.baltitoolbox.functions.Misc.runSuspendFunction
import balti.module.baltitoolbox.functions.Misc.showErrorDialog
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.extra_fragment_sms.*

class SmsFragment: ParentFragmentForExtras(R.layout.extra_fragment_sms) {

    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var selectorLauncher: ActivityResultLauncher<Intent>

    override lateinit var readTask: ParentReaderForExtras

    override fun onCreateFragment() {
        super.onCreateFragment()
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                startReadTask()
            }
            else {
                Toast.makeText(mActivity, R.string.sms_access_needed, Toast.LENGTH_SHORT).show()
                sms_fragment_checkbox.isChecked = false
            }
        }
        selectorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if (it.resultCode == Activity.RESULT_OK){
                updateSms()
            }
        }
    }

    override fun onCreateView(savedInstanceState: Bundle?) {

        delegateCheckbox?.setOnCheckedChangeListener { _, isChecked ->

            mActivity?.run {

                if (isChecked){
                    permissionLauncher.launch(Manifest.permission.READ_SMS)
                }
                else deselectExtra(smsList, listOf(delegateProgressBar, delegateStatusText))
            }
        }
    }

    private fun startReadTask(){
        readTask = ReadSmsKotlin(this)

        runSuspendFunction {
            val jobResults = readTask.executeWithResult()
            if (jobResults.success) {
                runOnMainThread {
                    tryIt({
                        updateSms(jobResults.result as ArrayList<SmsDataPacketKotlin>)
                    }, true)
                }
            } else {
                delegateCheckbox?.isChecked = false
                showErrorDialog(
                    jobResults.result.toString(),
                    getString(R.string.error_reading_sms)
                )
            }
        }
    }

    private fun updateSms(newList: ArrayList<SmsDataPacketKotlin> = smsList){

        smsList.apply {
            if (newList != this){
                clear()
                addAll(newList)
            }
        }

        delegateStatusText?.text = getString(R.string.reading)
        delegateProgressBar?.visibility = View.GONE

        var n = 0
        newList.forEach {
            if (it.selected) n++
        }

        if (smsList.size > 0){
            delegateStatusText?.text = "$n ${getString(R.string.of)} ${smsList.size}"
            delegateMainItem?.setOnClickListener {
                selectorLauncher.launch(Intent(mActivity, LoadSmsForSelection::class.java))
            }
        }
        else {
            mActivity?.let {
                AlertDialog.Builder(it)
                    .setMessage(R.string.empty_sms)
                    .setPositiveButton(R.string.close) {_, _ -> delegateCheckbox?.isChecked = false }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    override val viewIdStatusText: Int = R.id.sms_read_text_status
    override val viewIdProgressBar: Int = R.id.sms_read_progress
    override val viewIdCheckbox: Int = R.id.sms_fragment_checkbox

}