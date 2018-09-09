package balti.migrate;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

public class SmsTools {
    Context context;

    public SmsTools(Context context) {
        this.context = context;
    }

    public Cursor getSmsInboxCursor(){
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI, null, null, null, null);
        }
        catch (Exception e){}
        return cursor;
    }

    public Cursor getSmsOutboxCursor(){
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Telephony.Sms.Outbox.CONTENT_URI, null, null, null, null);
        }
        catch (Exception e){}
        return cursor;
    }

    public Cursor getSmsSentCursor(){
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Telephony.Sms.Sent.CONTENT_URI, null, null, null, null);
        }
        catch (Exception e){}
        return cursor;
    }

    public Cursor getSmsDraftCursor(){
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Telephony.Sms.Draft.CONTENT_URI, null, null, null, null);
        }
        catch (Exception e){}
        return cursor;
    }

    public SmsDataPacket getSmsPacket(Cursor cursor, boolean selected) {
        String smsAddress, smsBody, smsDate, smsDateSent, smsType, smsCreator, smsPerson, smsProtocol, smsSeen, smsServiceCenter, smsStatus, smsSubject, smsThreadId;
        int smsError, smsRead, smsLocked, smsReplyPathPresent;
        smsAddress = smsBody = smsDate = smsDateSent = smsType = smsCreator = smsPerson = smsProtocol = smsSeen = smsServiceCenter = smsStatus = smsSubject = smsThreadId = "";
        smsError = smsRead = smsLocked = smsReplyPathPresent = 0;

        try {
            smsAddress = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
            smsBody = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
            smsDate = cursor.getString(cursor.getColumnIndex(Telephony.Sms.DATE));
            smsDateSent = cursor.getString(cursor.getColumnIndex(Telephony.Sms.DATE_SENT));
            smsType = cursor.getString(cursor.getColumnIndex(Telephony.Sms.TYPE));
            smsCreator = cursor.getString(cursor.getColumnIndex(Telephony.Sms.CREATOR));

            smsPerson = cursor.getString(cursor.getColumnIndex(Telephony.Sms.PERSON));
            smsProtocol = cursor.getString(cursor.getColumnIndex(Telephony.Sms.PROTOCOL));
            smsSeen = cursor.getString(cursor.getColumnIndex(Telephony.Sms.SEEN));
            smsServiceCenter = cursor.getString(cursor.getColumnIndex(Telephony.Sms.SERVICE_CENTER));
            smsStatus = cursor.getString(cursor.getColumnIndex(Telephony.Sms.STATUS));
            smsSubject = cursor.getString(cursor.getColumnIndex(Telephony.Sms.SUBJECT));
            smsThreadId = cursor.getString(cursor.getColumnIndex(Telephony.Sms.THREAD_ID));

            smsError = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.ERROR_CODE));
            smsRead = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.READ));
            smsLocked = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.LOCKED));
            smsReplyPathPresent = cursor.getInt(cursor.getColumnIndex(Telephony.Sms.REPLY_PATH_PRESENT));
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return new SmsDataPacket(smsAddress, smsBody, smsDate, smsDateSent, smsType, smsCreator, smsPerson, smsProtocol, smsSeen, smsServiceCenter, smsStatus, smsSubject, smsThreadId, smsError, smsRead, smsLocked, smsReplyPathPresent, selected);
    }
}
