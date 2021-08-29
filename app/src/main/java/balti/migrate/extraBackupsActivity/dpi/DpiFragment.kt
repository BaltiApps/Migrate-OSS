package balti.migrate.extraBackupsActivity.dpi

import android.os.Bundle
import android.view.View
import balti.migrate.AppInstance.Companion.dpiText
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ParentFragmentForExtras

class DpiFragment: ParentFragmentForExtras(R.layout.extra_fragment_dpi) {

    override fun onCreateFragment() {
        super.onCreateFragment()
    }

    private fun startReadTask(){
        showStockWarning({
            delegateSalView?.visibility = View.GONE
            // call Read dpi kotlin
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
                    dpiText = null
                    deselectExtra(null, listOf(delegateProgressBar, delegateStatusText), listOf(delegateSalView))
                }
            }
        }
    }

    override val viewIdSalView: Int = R.id.sal_dpi

    override val viewIdStatusText: Int = R.id.dpi_read_text_status
    override val viewIdProgressBar: Int = R.id.dpi_read_progress
    override val viewIdCheckbox: Int = R.id.calls_fragment_checkbox
}