package balti.migrate.extraBackupsActivity.calls

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import balti.migrate.R
import balti.migrate.extraBackupsActivity.calls.containers.CallsDataPacketsKotlin
import balti.migrate.extraBackupsActivity.calls.utils.CallsListAdapterKotlin
import balti.migrate.extraBackupsActivity.utils.OnJobCompletion
import balti.migrate.extraBackupsActivity.utils.ViewOperations
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import kotlinx.android.synthetic.main.extra_item_selector.view.*

class LoadCallsForSelectionKotlin(private val jobCode: Int, val context: Context,
                                  private val itemList: ArrayList<CallsDataPacketsKotlin> = ArrayList(0)):
        AsyncCoroutineTask() {

    private val selectorView by lazy { View.inflate(context, R.layout.extra_item_selector, null) }
    private var dataPackets: ArrayList<CallsDataPacketsKotlin> = ArrayList(0)
    private val callsSelectorDialog by lazy { AlertDialog.Builder(context)
                .setView(selectorView)
                .setCancelable(false)
                .create()
    }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private val vOp by lazy { ViewOperations(context) }

    private lateinit var adapter: CallsListAdapterKotlin

    init {
        selectorView.eis_ok.setOnClickListener(null)
        selectorView.eis_cancel.setOnClickListener {
            callsSelectorDialog.dismiss()
            onJobCompletion.onComplete(jobCode, false, itemList)
        }
        vOp.textSet(selectorView.eis_no_data, R.string.no_calls)
        vOp.textSet(selectorView.eis_title, R.string.calls_selector_label)
    }

    override suspend fun onPreExecute() {
        super.onPreExecute()
        vOp.doSomething { callsSelectorDialog.show() }
        vOp.visibilitySet(selectorView.eis_top_bar, View.GONE)
        vOp.visibilitySet(selectorView.eis_button_bar, View.GONE)
        vOp.visibilitySet(selectorView.eis_progressBar, View.VISIBLE)
        vOp.visibilitySet(selectorView.eis_listView, View.INVISIBLE)
        vOp.visibilitySet(selectorView.eis_no_data, View.GONE)
    }

    override suspend fun doInBackground(arg: Any?): Any? {
        for (cdp in itemList){
            dataPackets.add(cdp.copy())
        }
        if (dataPackets.size > 0) adapter = CallsListAdapterKotlin(context, dataPackets)
        return null
    }

    override suspend fun onPostExecute(result: Any?) {
        super.onPostExecute(result)
        if (dataPackets.size > 0){
            vOp.doSomething { selectorView.eis_listView.adapter = adapter }
            vOp.visibilitySet(selectorView.eis_top_bar, View.VISIBLE)
            vOp.visibilitySet(selectorView.eis_button_bar, View.VISIBLE)
            vOp.visibilitySet(selectorView.eis_progressBar, View.GONE)
            vOp.visibilitySet(selectorView.eis_listView, View.VISIBLE)
        }
        else {
            vOp.visibilitySet(selectorView.eis_no_data, View.VISIBLE)
            vOp.doSomething { callsSelectorDialog.setCancelable(true) }
        }

        selectorView.eis_ok.setOnClickListener {
            onJobCompletion.onComplete(jobCode, true, dataPackets)
            callsSelectorDialog.dismiss()
        }

        selectorView.eis_select_all.setOnClickListener {
            adapter.checkAll(true)
        }

        selectorView.eis_clear_all.setOnClickListener {
            adapter.checkAll(false)
        }
    }

}