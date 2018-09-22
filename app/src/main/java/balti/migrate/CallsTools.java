package balti.migrate;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;

public class CallsTools {

    Context context;

    public CallsTools(Context context) {
        this.context = context;
    }

    public Cursor getCallsCursor() {
        Cursor cursor = null;
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
                cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, null);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return cursor;
    }

    public CallsDataPacket getCallsPacket(Cursor cursor, boolean selected){

        String callsCachedFormattedNumber, callsCachedLookupUri, callsCachedMatchedNumber, callsCachedName, callsCachedNormalizedNumber,
                callsCachedNumberLabel, callsCachedNumberType, callsCachedPhotoId,
                callsCountryIso, callsDataUsage, callsFeatures, callsGeocodedLocation,
                callsIsRead, callsNumber, callsNumberPresentation, callsPhoneAccountComponentName,
                callsPhoneAccountId, callsTranscription, callsType, callsVoicemailUri;

        long callsDate, callsDuration, callsNew;

        callsCachedFormattedNumber = callsCachedLookupUri = callsCachedMatchedNumber = callsCachedName = callsCachedNormalizedNumber =
                callsCachedNumberLabel = callsCachedNumberType = callsCachedPhotoId = callsCountryIso = callsDataUsage =
                        callsFeatures = callsGeocodedLocation = callsIsRead = callsNumber =
                                callsNumberPresentation = callsPhoneAccountComponentName = callsPhoneAccountId = callsTranscription =
                                        callsType = callsVoicemailUri = "";

        callsDate = callsDuration = callsNew = 0;

        try {

            callsCachedFormattedNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_FORMATTED_NUMBER));
            callsCachedLookupUri = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_LOOKUP_URI));
            callsCachedMatchedNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_MATCHED_NUMBER));
            callsCachedName = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
            callsCachedNormalizedNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NORMALIZED_NUMBER));
            callsCachedNumberLabel = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NUMBER_LABEL));
            callsCachedNumberType = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NUMBER_TYPE));
            callsCachedPhotoId = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_PHOTO_ID));
            callsCountryIso = cursor.getString(cursor.getColumnIndex(CallLog.Calls.COUNTRY_ISO));
            callsDataUsage = cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATA_USAGE));
            callsFeatures = cursor.getString(cursor.getColumnIndex(CallLog.Calls.FEATURES));
            callsGeocodedLocation = cursor.getString(cursor.getColumnIndex(CallLog.Calls.GEOCODED_LOCATION));
            callsIsRead = cursor.getString(cursor.getColumnIndex(CallLog.Calls.IS_READ));
            callsNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
            callsNumberPresentation = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER_PRESENTATION));
            callsPhoneAccountComponentName = cursor.getString(cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME));
            callsPhoneAccountId = cursor.getString(cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID));
            callsTranscription = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TRANSCRIPTION));
            callsType = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
            callsVoicemailUri = cursor.getString(cursor.getColumnIndex(CallLog.Calls.VOICEMAIL_URI));

            callsDate = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
            callsDuration = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DURATION));
            callsNew = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.NEW));

        }
        catch (Exception e){
            e.printStackTrace();
        }

        return new CallsDataPacket(callsCachedFormattedNumber, callsCachedLookupUri, callsCachedMatchedNumber, callsCachedName, callsCachedNormalizedNumber,
                callsCachedNumberLabel, callsCachedNumberType, callsCachedPhotoId, callsCountryIso, callsDataUsage,
                callsFeatures, callsGeocodedLocation, callsIsRead, callsNumber, callsNumberPresentation,
                callsPhoneAccountComponentName, callsPhoneAccountId, callsTranscription, callsType, callsVoicemailUri, callsDate,
                callsDuration, callsNew, selected);
    }
}
