package balti.migrate.extraBackupsActivity.calls

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import balti.migrate.AppInstance.Companion.callsList
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ParentFragmentForExtras
import balti.migrate.extraBackupsActivity.ParentReaderForExtras
import balti.migrate.extraBackupsActivity.ReaderJobResultHolder
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.module.baltitoolbox.functions.Misc.runOnMainThread
import balti.module.baltitoolbox.functions.Misc.runSuspendFunction
import balti.module.baltitoolbox.functions.Misc.showErrorDialog
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.extra_fragment_calls.*

class CallsFragment: ParentFragmentForExtras(R.layout.extra_fragment_calls) {

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
                Toast.makeText(mActivity, R.string.calls_access_needed, Toast.LENGTH_SHORT).show()
                calls_fragment_checkbox.isChecked = false
            }
        }
        selectorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if (it.resultCode == Activity.RESULT_OK){
                updateCalls()
            }
        }
    }

    override fun onCreateView(savedInstanceState: Bundle?) {

        delegateCheckbox?.setOnCheckedChangeListener { _, isChecked ->

            mActivity?.run {

                if (isChecked){
                    permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                }
                else deselectExtra(callsList, listOf(calls_read_progress, calls_read_text_status))
            }
        }
    }

    private fun startReadTask(){
        readTask = ReadCallsKotlin(this)

        runSuspendFunction {
            val jobResults = readTask.executeWithResult() as ReaderJobResultHolder
            if (jobResults.success) {
                runOnMainThread {
                    tryIt({
                        updateCalls(jobResults.result as ArrayList<CallsDataPacketsKotlin>)
                    }, true)
                }
            } else {
                delegateCheckbox?.isChecked = false
                showErrorDialog(
                    jobResults.result.toString(),
                    getString(R.string.error_reading_contacts)
                )
            }
        }
    }

    private fun updateCalls(newList: ArrayList<CallsDataPacketsKotlin> = callsList){

        callsList.apply {
            if (newList != callsList){
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

        if (callsList.size > 0){
            delegateStatusText?.text = "$n ${getString(R.string.of)} ${callsList.size}"
            delegateMainItem?.setOnClickListener {
                //LoadCallsForSelectionKotlin(JOBCODE_LOAD_CALLS, this, callsList).execute()
            }
        }
        else {
            mActivity?.let {
                AlertDialog.Builder(it)
                    .setMessage(R.string.empty_call_logs)
                    .setPositiveButton(R.string.close) {_, _ -> delegateCheckbox?.isChecked = false }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    override val viewIdStatusText: Int = R.id.calls_read_text_status
    override val viewIdProgressBar: Int = R.id.calls_read_progress
    override val viewIdCheckbox: Int = R.id.calls_fragment_checkbox

    override fun isChecked(): Boolean? = try {
        delegateCheckbox?.isChecked
    } catch (e: Exception) { null }

}