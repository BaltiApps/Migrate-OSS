package balti.migrate;

public class CallsDataPacket {

    String callsCachedFormattedNumber, callsCachedLookupUri, callsCachedMatchedNumber, callsCachedName, callsCachedNormalizedNumber, callsCachedNumberLabel,
            callsCachedNumberType, callsCachedPhotoId, callsCountryIso, callsDataUsage,
            callsFeatures, callsGeocodedLocation, callsIsRead, callsNumber, callsNumberPresentation,
            callsPhoneAccountComponentName, callsPhoneAccountId, callsTranscription, callsType, callsVoicemailUri;
    long callsDate, callsDuration, callsNew;
    boolean selected;

    public CallsDataPacket(String callsCachedFormattedNumber, String callsCachedLookupUri, String callsCachedMatchedNumber,
                           String callsCachedName, String callsCachedNormalizedNumber, String callsCachedNumberLabel,
                           String callsCachedNumberType, String callsCachedPhotoId, String callsCountryIso,
                           String callsDataUsage, String callsFeatures,
                           String callsGeocodedLocation, String callsIsRead, String callsNumber, String callsNumberPresentation,
                           String callsPhoneAccountComponentName, String callsPhoneAccountId, String callsTranscription,
                           String callsType, String callsVoicemailUri, long callsDate, long callsDuration, long callsNew, boolean selected) {

        this.callsCachedFormattedNumber = callsCachedFormattedNumber;
        this.callsCachedLookupUri = callsCachedLookupUri;
        this.callsCachedMatchedNumber = callsCachedMatchedNumber;
        this.callsCachedName = callsCachedName;
        this.callsCachedNormalizedNumber = callsCachedNormalizedNumber;
        this.callsCachedNumberLabel = callsCachedNumberLabel;
        this.callsCachedNumberType = callsCachedNumberType;
        this.callsCachedPhotoId = callsCachedPhotoId;
        this.callsCountryIso = callsCountryIso;
        this.callsDataUsage = callsDataUsage;
        this.callsFeatures = callsFeatures;
        this.callsGeocodedLocation = callsGeocodedLocation;
        this.callsIsRead = callsIsRead;
        this.callsNumber = callsNumber;
        this.callsNumberPresentation = callsNumberPresentation;
        this.callsPhoneAccountComponentName = callsPhoneAccountComponentName;
        this.callsPhoneAccountId = callsPhoneAccountId;
        this.callsTranscription = callsTranscription;
        this.callsType = callsType;
        this.callsVoicemailUri = callsVoicemailUri;

        this.callsDate = callsDate;
        this.callsDuration = callsDuration;
        this.callsNew = callsNew;

        this.selected = selected;
    }

    public CallsDataPacket(CallsDataPacket obj) {
        this.callsCachedFormattedNumber = obj.callsCachedFormattedNumber;
        this.callsCachedLookupUri = obj.callsCachedLookupUri;
        this.callsCachedMatchedNumber = obj.callsCachedMatchedNumber;
        this.callsCachedName = obj.callsCachedName;
        this.callsCachedNormalizedNumber = obj.callsCachedNormalizedNumber;
        this.callsCachedNumberLabel = obj.callsCachedNumberLabel;
        this.callsCachedNumberType = obj.callsCachedNumberType;
        this.callsCachedPhotoId = obj.callsCachedPhotoId;
        this.callsCountryIso = obj.callsCountryIso;
        this.callsDataUsage = obj.callsDataUsage;
        this.callsFeatures = obj.callsFeatures;
        this.callsGeocodedLocation = obj.callsGeocodedLocation;
        this.callsIsRead = obj.callsIsRead;
        this.callsNumber = obj.callsNumber;
        this.callsNumberPresentation = obj.callsNumberPresentation;
        this.callsPhoneAccountComponentName = obj.callsPhoneAccountComponentName;
        this.callsPhoneAccountId = obj.callsPhoneAccountId;
        this.callsTranscription = obj.callsTranscription;
        this.callsType = obj.callsType;
        this.callsVoicemailUri = obj.callsVoicemailUri;

        this.callsDate = obj.callsDate;
        this.callsDuration = obj.callsDuration;
        this.callsNew = obj.callsNew;

        this.selected = obj.selected;
    }
}
