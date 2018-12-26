package balti.migrate;

import android.content.Context;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

public class SmsListAdapter extends BaseAdapter {

    LayoutInflater layoutInflater;

    Vector<SmsDataPacket> smsList;

    SmsListAdapter(Context context, Vector<SmsDataPacket> smsList) {
        this.smsList = smsList;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Collections.sort(smsList, new Comparator<SmsDataPacket>() {
            @Override
            public int compare(SmsDataPacket t1, SmsDataPacket t2) {
                return t1.smsThreadID.compareTo(t2.smsThreadID);
            }
        });
    }

    @Override
    public int getCount() {
        return smsList.size();
    }

    @Override
    public Object getItem(int i) {
        return smsList.get(i);
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
    public View getView(int i, View view, ViewGroup viewGroup) {

        final SmsDataPacket dataPacket = smsList.get(i);

        int smsT = Integer.parseInt(dataPacket.smsType);

        view = layoutInflater.inflate(R.layout.sms_item, null);

        final CheckBox checkBox = view.findViewById(R.id.sms_item_checkbox);
        checkBox.setChecked(dataPacket.selected);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                dataPacket.selected = b;
            }
        });

        TextView sender = view.findViewById(R.id.sms_item_sender);
        sender.setText(dataPacket.smsAddress);

        TextView body = view.findViewById(R.id.sms_item_body);
        body.setText(dataPacket.smsBody);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBox.setChecked(!checkBox.isChecked());
            }
        });

        ImageView icon = view.findViewById(R.id.sms_item_icon);
        switch (smsT) {
            case Telephony.Sms.MESSAGE_TYPE_INBOX:
                icon.setImageResource(R.drawable.ic_incoming_sms);
                break;
            case Telephony.Sms.MESSAGE_TYPE_SENT:
                icon.setImageResource(R.drawable.ic_outgoing_sms);
                break;
            case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                icon.setImageResource(R.drawable.ic_outbox_sms);
                break;
            case Telephony.Sms.MESSAGE_TYPE_DRAFT:
                icon.setImageResource(R.drawable.ic_draft_sms);
                break;
            default:
                icon.setImageResource(R.drawable.ic_outgoing_sms);
        }


        return view;
    }

    void checkAll(boolean b) {
        for (SmsDataPacket dataPacket : smsList) {
            dataPacket.selected = b;
        }
    }
}
