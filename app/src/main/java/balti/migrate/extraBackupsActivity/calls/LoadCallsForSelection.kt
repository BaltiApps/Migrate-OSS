package balti.migrate.extraBackupsActivity.calls

import android.view.View
import balti.migrate.AppInstance.Companion.callsList
import balti.migrate.R
import balti.migrate.extraBackupsActivity.ParentSelectorActivityForExtras
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.calls.utils.CallsListAdapterKotlin
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.extra_item_selector.*

class LoadCallsForSelection: ParentSelectorActivityForExtras(R.layout.extra_item_selector) {

    private var dataPackets: ArrayList<CallsDataPacketsKotlin> = ArrayList(0)
    private var adapter: CallsListAdapterKotlin? = null

    override fun setup(){
        eis_ok.setOnClickListener(null)
        eis_cancel.setOnClickListener {
            sendResult(false)
        }
        eis_no_data.setText(R.string.no_calls)
        eis_title.setText(R.string.calls_selector_label)

        eis_top_bar.visibility = View.GONE
        eis_button_bar.visibility = View.GONE
        eis_progressBar.visibility = View.VISIBLE
        eis_listView.visibility = View.INVISIBLE
        eis_no_data.visibility = View.GONE
    }

    override fun backgroundProcessing() {
        writeLog("Copying to temp list.")
        for (cdp in callsList){
            dataPackets.add(cdp.copy())
        }
        writeLog("Creating adapter.")
        if (dataPackets.size > 0) {
            tryIt {
                adapter = CallsListAdapterKotlin(this, dataPackets)
            }
        }
        writeLog("Adapter created. Is null - ${adapter == null}")
    }

    override fun postProcessing() {
        if (dataPackets.size > 0 && adapter != null){
            writeLog("Showing list for selection.")

            tryIt { eis_listView.adapter = adapter }
            eis_top_bar.visibility = View.VISIBLE
            eis_button_bar.visibility = View.VISIBLE
            eis_progressBar.visibility = View.GONE
            eis_listView.visibility = View.VISIBLE

            eis_select_all.setOnClickListener {
                adapter?.checkAll(true)
            }

            eis_clear_all.setOnClickListener {
                adapter?.checkAll(false)
            }
        }
        else {
            writeLog("No data.")
            eis_no_data.visibility = View.VISIBLE
        }

        eis_ok.setOnClickListener {
            sendResult(true)
        }
    }

    override fun successBlock(){
        callsList.apply {
            clear()
            addAll(dataPackets)
        }
    }

    override val className = "LoadCallsForSelection"

}