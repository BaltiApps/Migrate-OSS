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
                                     private val contactsList: ArrayList<ContactsDataPacketKotlin> = ArrayList(0)):
        AsyncTask<Any, Any, Any>() {

    private val selectorView by lazy { View.inflate(context, R.layout.extra_item_selector, null) }
    private var dataPackets: ArrayList<ContactsDataPacketKotlin> = ArrayList(0)
    private val contactsSelectorDialog by lazy { AlertDialog.Builder(context)
            .setView(selectorView)
            .setCancelable(false)
            .create() }
    private val onJobCompletion by lazy { context as OnJobCompletion }
    private val vOp by lazy { ViewOperations(context) }

    private lateinit var adapter: ContactListAdapterKotlin

    init {
        selectorView.extra_item_selector_ok.setOnClickListener(null)
        selectorView.extra_item_selector_cancel.setOnClickListener {
            contactsSelectorDialog.dismiss()
            onJobCompletion.onComplete(jobCode, false, contactsList)
        }
        vOp.textSet(selectorView.no_data_label, R.string.no_contacts)
        vOp.textSet(selectorView.extra_item_selector_title, R.string.sms_selector_label)
    }

    override fun onPreExecute() {
        vOp.doSomething { contactsSelectorDialog.show() }
        super.onPreExecute()
        vOp.visibilitySet(selectorView.extra_item_selector_top_bar, View.GONE)
        vOp.visibilitySet(selectorView.extra_item_selector_button_bar, View.GONE)
        vOp.visibilitySet(selectorView.extra_item_selector_round_progress, View.VISIBLE)
        vOp.visibilitySet(selectorView.extra_item_selector_item_holder, View.INVISIBLE)
        vOp.visibilitySet(selectorView.no_data_label, View.GONE)
    }

    override fun doInBackground(vararg params: Any?): Any? {
        for (cdp in contactsList){
            val cdp1 = cdp.copy()
            dataPackets.add(cdp1)
        }
        if (dataPackets.size > 0) adapter = ContactListAdapterKotlin(context, dataPackets)
        return null
    }

    override fun onPostExecute(result: Any?) {
        super.onPostExecute(result)

        if (dataPackets.size > 0){
            vOp.doSomething { selectorView.extra_item_selector_item_holder.adapter = adapter }
            vOp.visibilitySet(selectorView.extra_item_selector_top_bar, View.VISIBLE)
            vOp.visibilitySet(selectorView.extra_item_selector_button_bar, View.VISIBLE)
            vOp.visibilitySet(selectorView.extra_item_selector_round_progress, View.GONE)
            vOp.visibilitySet(selectorView.extra_item_selector_item_holder, View.VISIBLE)
        }
        else vOp.visibilitySet(selectorView.no_data_label, View.VISIBLE)

        selectorView.extra_item_selector_ok.setOnClickListener {
            onJobCompletion.onComplete(jobCode, true, dataPackets)
            contactsSelectorDialog.dismiss()
        }

        selectorView.extra_item_selector_select_all.setOnClickListener {
            adapter.checkAll(true)
        }

        selectorView.extra_item_selector_clear_all.setOnClickListener {
            adapter.checkAll(false)
        }

    }
}