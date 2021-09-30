package balti.migrate.extraBackupsActivity.engines.adb

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import balti.migrate.AppInstance
import balti.migrate.R
import balti.migrate.extraBackupsActivity.utils.ParentFragmentForExtras
import balti.migrate.extraBackupsActivity.utils.ParentReaderForExtras
import balti.module.baltitoolbox.functions.Misc

class AdbFragment: ParentFragmentForExtras(R.layout.extra_fragment_adb) {

    override lateinit var readTask: ParentReaderForExtras

    private fun startReadTask(){
        showStockWarning({
            delegateSalView?.visibility = View.GONE
            // call ReadAdbKotlin
            readTask = ReadAdbKotlin(this)

            Misc.runSuspendFunction {
                val jobResults = readTask.executeWithResult()

                if (jobResults.success) {

                    try {
                        AppInstance.adbState = jobResults.result.toString().toInt()
                    }
                    catch (e: Exception){
                        Toast.makeText(mActivity, "Failed to cast ADB: ${jobResults.result}", Toast.LENGTH_SHORT).show()
                    }

                    Misc.runOnMainThread {
                        Misc.tryIt({
                            delegateMainItem?.setOnClickListener {
                                mActivity?.let { it1 ->
                                    AlertDialog.Builder(it1)
                                        .setTitle(R.string.adb_label)
                                        .setNegativeButton(R.string.close, null)
                                        .apply {
                                            when (val state = AppInstance.adbState) {
                                                0 -> setMessage("${getString(R.string.adb_disabled)} ($state)")
                                                1 -> setMessage("${getString(R.string.adb_enabled)} ($state)")
                                            }
                                        }
                                        .show()
                                }
                            }
                        }, true)
                    }
                } else {
                    delegateCheckbox?.isChecked = false
                    Misc.showErrorDialog(
                        jobResults.result.toString(),
                        getString(R.string.error_reading_adb)
                    )
                }
            }

        }, {
            delegateCheckbox?.isChecked = false
        })
    }

    override fun onCreateView(savedInstanceState: Bundle?) {

        delegateCheckbox?.setOnCheckedChangeListener { _, isChecked ->

            mActivity?.run {

                if (isChecked){
                    startReadTask()
                }
                else {
                    AppInstance.adbState = null
                    deselectExtra(null, listOf(delegateProgressBar, delegateStatusText), listOf(delegateSalView))
                }
            }
        }
    }

    override val viewIdSalView: Int = R.id.sal_adb

    override val viewIdStatusText: Int = R.id.adb_read_text_status
    override val viewIdProgressBar: Int = R.id.adb_read_progress
    override val viewIdCheckbox: Int = R.id.adb_fragment_checkbox

}