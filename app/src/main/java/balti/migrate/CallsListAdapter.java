package balti.migrate;

import android.content.Context;
import android.provider.CallLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;

public class CallsListAdapter extends BaseAdapter {


    LayoutInflater layoutInflater;

    Vector<CallsDataPacket> callsList;

    CallsListAdapter(Context context, Vector<CallsDataPacket> callsList){
        this.callsList = callsList;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Collections.sort(callsList, new Comparator<CallsDataPacket>() {
            @Override
            public int compare(CallsDataPacket o1, CallsDataPacket o2) {
                return String.CASE_INSENSITIVE_ORDER.compare("" + o2.callsDate, "" + o1.callsDate);
            }
        });
    }

    @Override
    public int getCount() {
        return callsList.size();
    }

    @Override
    public Object getItem(int i) {
        return callsList.get(i);
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

        final CallsDataPacket dataPacket = callsList.get(i);
        view = layoutInflater.inflate(R.layout.calls_item, null);

        final CheckBox checkBox = view.findViewById(R.id.calls_item_checkbox);
        checkBox.setChecked(dataPacket.selected);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                dataPacket.selected = b;
            }
        });

        TextView displayName = view.findViewById(R.id.calls_item_person);
        String name = dataPacket.callsCachedName;
        String number = dataPacket.callsNumber;

        TextView displayNumber = view.findViewById(R.id.calls_item_number);

        if (name != null && !name.equals("")) {
            displayName.setText(name);
            displayNumber.setVisibility(View.VISIBLE);
            displayNumber.setText(number);
        }
        else {
            displayNumber.setVisibility(View.GONE);
            displayName.setText(number);
        }

        TextView date = view.findViewById(R.id.calls_item_date);
        date.setText(getDate(dataPacket.callsDate));

        TextView duration = view.findViewById(R.id.calls_item_duration);
        duration.setText(getDuration(dataPacket.callsDuration));


        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBox.setChecked(!checkBox.isChecked());
            }
        });

        ImageView icon = view.findViewById(R.id.calls_item_icon);
        switch (dataPacket.callsType) {
            case "" + CallLog.Calls.INCOMING_TYPE:
                icon.setImageResource(R.drawable.ic_incoming_call);
                break;
            case "" + CallLog.Calls.OUTGOING_TYPE:
                icon.setImageResource(R.drawable.ic_outgoing_call);
                break;
            case "" + CallLog.Calls.MISSED_TYPE:
                icon.setImageResource(R.drawable.ic_missed_call);
                break;
        }


        return view;
    }


    void checkAll(boolean b){
        for (CallsDataPacket dataPacket : callsList){
            dataPacket.selected = b;
        }
    }

    String getDate(long l){
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        return df.format(new Date(l));
    }

    String getDuration(long l){
        String duration = "";

        try {

            long d = l / (60 * 60 * 24);
            if (d != 0) duration = duration + d + " days ";
            l = l % (60 * 60 * 24);

            long h = l / (60 * 60);
            if (h != 0) duration = duration + h + " hrs ";
            l = l % (60 * 60);

            long m = l / 60;
            if (m != 0) duration = duration + m + " mins ";
            l = l % 60;

            long s = l;
            duration = duration + s + " secs";

        }
        catch (Exception ignored){}

        return duration;
    }
}
