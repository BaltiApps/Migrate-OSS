package balti.migrate.extraBackupsActivity.contacts.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import balti.migrate.R
import balti.migrate.extraBackupsActivity.contacts.containers.ContactsDataPacketKotlin
import kotlinx.android.synthetic.main.contacts_item.view.*

class ContactListAdapterKotlin(val context: Context,
                               private val contactsList: ArrayList<ContactsDataPacketKotlin>): BaseAdapter() {

    init {
        contactsList.sortWith(Comparator { t1, t2 -> String.CASE_INSENSITIVE_ORDER.compare(t1.fullName, t2.fullName) })
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val cdp = contactsList[position]

        val view = View.inflate(context, R.layout.contacts_item, null)
        view.contacts_item_checkbox.apply {
            text = cdp.fullName
            isChecked = cdp.selected
            setOnCheckedChangeListener { _, isChecked ->
                cdp.selected = isChecked
            }
        }

        return view
    }

    fun checkAll(isChecked: Boolean){
        for (cdp in contactsList)
            cdp.selected = isChecked
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): Any = contactsList[position]
    override fun getItemId(position: Int): Long = 0
    override fun getCount(): Int = contactsList.size
    override fun getViewTypeCount(): Int = count
    override fun getItemViewType(position: Int): Int = position
}