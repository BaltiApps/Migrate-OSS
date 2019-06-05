package balti.migrate.inAppRestore;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import balti.migrate.R;

public class ZipItemAdapter extends BaseAdapter {

    private Context context;
    private ZipFileItem[] zipFileItems;
    private OnZipItemClick onZipItemClick;

    public ZipItemAdapter(Context context, ZipFileItem[] zipFileItems) {
        this.context = context;
        this.zipFileItems = zipFileItems;

        onZipItemClick = (OnZipItemClick)context;
    }

    @Override
    public int getCount() {
        return zipFileItems.length;
    }

    @Override
    public Object getItem(int position) {
        return zipFileItems[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return getCount();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        view = View.inflate(context, R.layout.zip_picker_item, null);

        final ZipFileItem zfi = zipFileItems[position];

        ImageView icon = view.findViewById(R.id.zip_item_icon);
        TextView name = view.findViewById(R.id.zip_item_name);
        final CheckBox cb = view.findViewById(R.id.zip_item_checkbox);

        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                zfi.isSelected = isChecked;
                onZipItemClick.onZipItemClick(zfi);
            }
        });

        if (zfi.file == null){
            icon.setVisibility(View.GONE);
            cb.setVisibility(View.GONE);
            name.setText(R.string.empty);
            cb.setChecked(false);

            return view;
        }

        name.setText(zfi.file.getName());
        cb.setChecked(zfi.isSelected);

        for (ZipFileItem z : ZipPicker.zipFileItems){
            if (zfi.file.getAbsolutePath().equals(z.file.getAbsolutePath())){
                cb.setChecked(z.isSelected);
                break;
            }
        }

        if (zfi.file.isDirectory()){
            zfi.isSelected = false;
            cb.setChecked(false);
            cb.setVisibility(View.GONE);
            icon.setImageResource(R.drawable.ic_zip_picker_folder);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onZipItemClick.onZipItemClick(zfi);
                }
            });
        }
        else {
            if (zfi.file.getName().endsWith(".zip")) {
                cb.setVisibility(View.VISIBLE);
                icon.setImageResource(R.drawable.ic_zip_picker_zip);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cb.setChecked(!cb.isChecked());
                    }
                });
            }
            else {
                zfi.isSelected = false;
                cb.setChecked(false);
                cb.setVisibility(View.GONE);
            }
        }

        return view;
    }
}
