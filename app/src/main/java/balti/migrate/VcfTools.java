package balti.migrate;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class VcfTools {

    Context context;

    public VcfTools(Context context) {
        this.context = context;
    }

    public Cursor getContactsCursor() {
        Cursor cursor = null;
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            }
        } catch (Exception e) {
        }
        return cursor;
    }

    public String[] getVcfData(Cursor cursor) {
        String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
        String vcardstring;
        String fullName;
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
        AssetFileDescriptor fd;
        try {
            fd = context.getContentResolver().openAssetFileDescriptor(uri, "r");

            assert fd != null;
            FileInputStream fis = fd.createInputStream();
            byte[] buf = readBytes(fis);
            fis.read(buf);
            vcardstring = new String(buf);
            fullName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        } catch (Exception e1) {
            e1.printStackTrace();
            fullName = "";
            vcardstring = "";
        }
        return new String[]{fullName, vcardstring};
    }

    private byte[] readBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int len;
        byte[] buffer = new byte[2048];
        while ((len = stream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, len);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
