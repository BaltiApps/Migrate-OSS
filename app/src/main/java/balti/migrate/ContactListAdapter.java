package balti.migrate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

public class ContactListAdapter extends BaseAdapter {


    LayoutInflater layoutInflater;

    Vector<ContactsDataPacket> contactsList;

    ContactListAdapter(Context context, Vector<ContactsDataPacket> contactsList){
        this.contactsList = contactsList;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Collections.sort(contactsList, new Comparator<ContactsDataPacket>() {
            @Override
            public int compare(ContactsDataPacket t1, ContactsDataPacket t2) {
                return String.CASE_INSENSITIVE_ORDER.compare(t1.fullName, t2.fullName);
            }
        });
    }

    @Override
    public int getCount() {
        return contactsList.size();
    }

    @Override
    public Object getItem(int i) {
        return contactsList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return getCount();
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {

        final ContactsDataPacket dataPacket = contactsList.get(i);
        view = layoutInflater.inflate(R.layout.contacts_item, null);

        CheckBox checkBox = view.findViewById(R.id.contacts_item_checkbox);
        checkBox.setText(dataPacket.fullName);
        checkBox.setChecked(dataPacket.selected);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                dataPacket.selected = b;
            }
        });

        return view;
    }


    void checkAll(boolean b){
        for (ContactsDataPacket dataPacket : contactsList){
            dataPacket.selected = b;
        }
    }
}
