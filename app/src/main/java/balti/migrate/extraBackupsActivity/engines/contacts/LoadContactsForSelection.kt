package balti.migrate.extraBackupsActivity.engines.contacts

import android.view.View
import balti.migrate.AppInstance.Companion.contactsList
import balti.migrate.R
import balti.migrate.extraBackupsActivity.engines.contacts.containers.ContactsDataPacketKotlin
import balti.migrate.extraBackupsActivity.engines.contacts.utils.ContactListAdapterKotlin
import balti.migrate.extraBackupsActivity.utils.ParentSelectorActivityForExtras
import balti.module.baltitoolbox.functions.Misc.tryIt
import kotlinx.android.synthetic.main.extra_item_selector.*

class LoadContactsForSelection: ParentSelectorActivityForExtras(R.layout.extra_item_selector) {

    private var dataPackets: ArrayList<ContactsDataPacketKotlin> = ArrayList(0)
    private var adapter: ContactListAdapterKotlin? = null

    override fun setup(){
        eis_ok.setOnClickListener(null)
        eis_cancel.setOnClickListener {
            sendResult(false)
        }
        eis_no_data.setText(R.string.no_contacts)
        eis_title.setText(R.string.contacts_selector_label)

        eis_top_bar.visibility = View.GONE
        eis_button_bar.visibility = View.GONE
        eis_progressBar.visibility = View.VISIBLE
        eis_listView.visibility = View.INVISIBLE
        eis_no_data.visibility = View.GONE
    }

    override fun backgroundProcessing(){
        writeLog("Copying to temp list.")
        for (cdp in contactsList){
            dataPackets.add(cdp.copy())
        }
        writeLog("Creating adapter.")
        if (dataPackets.size > 0) {
            tryIt {
                adapter = ContactListAdapterKotlin(this, dataPackets)
            }
        }
        writeLog("Adapter created. Is null - ${adapter == null}")
    }

    override fun postProcessing(){
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
        contactsList.apply {
            clear()
            addAll(dataPackets)
        }
    }

    override val className = "LoadContactsForSelection"
}