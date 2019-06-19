package balti.migrate.extraBackupsActivity.contacts

import android.content.Context
import android.os.AsyncTask
import android.support.v7.app.AlertDialog
import android.view.View
import balti.migrate.R
import balti.migrate.extraBackupsActivity.OnJobCompletion
import balti.migrate.extraBackupsActivity.ViewOperations
import kotlinx.android.synthetic.main.extra_item_selector.view.*

class LoadContactsForSelectionKotlin(private val jobCode: Int, val context: Context,
                                     private val itemList: ArrayList<ContactsDataPacketKotlin> = ArrayList(0)):     //unique
        AsyncTask<Any, Any, Any>() {

    private val selectorView by lazy { View.inflate(context, R.layout.extra_item_selector, null) }
    private var dataPackets: ArrayList<ContactsDataPacketKotlin> = ArrayList(0)
    private val contactsSelectorDialog by lazy { AlertDialog.Builder(context)
            .setView(selectorView)
            .setCancelable(false)
            .create() }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private val vOp by lazy { ViewOperations(context) }

    private lateinit var adapter: ContactListAdapterKotlin              //unique

    init {
        selectorView.eis_ok.setOnClickListener(null)
        selectorView.eis_cancel.setOnClickListener {
            contactsSelectorDialog.dismiss()
            onJobCompletion.onComplete(jobCode, false, itemList)
        }
        vOp.textSet(selectorView.eis_no_data, R.string.no_contacts)         //unique
        vOp.textSet(selectorView.eis_title, R.string.sms_selector_label)            //unique
    }

    override fun onPreExecute() {
        vOp.doSomething { contactsSelectorDialog.show() }
        super.onPreExecute()
        vOp.visibilitySet(selectorView.eis_top_bar, View.GONE)
        vOp.visibilitySet(selectorView.eis_button_bar, View.GONE)
        vOp.visibilitySet(selectorView.eis_progressBar, View.VISIBLE)
        vOp.visibilitySet(selectorView.eis_listView, View.INVISIBLE)
        vOp.visibilitySet(selectorView.eis_no_data, View.GONE)
    }

    override fun doInBackground(vararg params: Any?): Any? {
        for (cdp in itemList){
            dataPackets.add(cdp.copy())
        }
        if (dataPackets.size > 0) adapter = ContactListAdapterKotlin(context, dataPackets)  //unique
        return null
    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)

        if (dataPackets.size > 0){
            vOp.doSomething { selectorView.eis_listView.adapter = adapter }
            vOp.visibilitySet(selectorView.eis_top_bar, View.VISIBLE)
            vOp.visibilitySet(selectorView.eis_button_bar, View.VISIBLE)
            vOp.visibilitySet(selectorView.eis_progressBar, View.GONE)
            vOp.visibilitySet(selectorView.eis_listView, View.VISIBLE)
        }
        else vOp.visibilitySet(selectorView.eis_no_data, View.VISIBLE)

        selectorView.eis_ok.setOnClickListener {
            onJobCompletion.onComplete(jobCode, true, dataPackets)
            contactsSelectorDialog.dismiss()
        }

        selectorView.eis_select_all.setOnClickListener {
            adapter.checkAll(true)
        }

        selectorView.eis_clear_all.setOnClickListener {
            adapter.checkAll(false)
        }

    }
}