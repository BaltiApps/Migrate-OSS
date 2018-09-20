package balti.migrate;

public class SmsDataPacket {
    String smsAddress, smsBody, smsDate, smsDateSent, smsType, smsCreator, smsPerson, smsProtocol, smsSeen, smsServiceCenter, smsStatus, smsSubject, smsThreadID;
    int smsError, smsRead, smsLocked, smsReplyPathPresent;

    boolean selected;

    public SmsDataPacket(String smsAddress, String smsBody, String smsDate, String smsDateSent, String smsType, String smsCreator, String smsPerson, String smsProtocol, String smsSeen, String smsServiceCenter, String smsStatus, String smsSubject, String smsThreadId, int smsError, int smsRead, int smsLocked, int smsReplyPathPresent, boolean selected) {
        this.smsAddress = smsAddress;
        this.smsBody = smsBody;
        this.smsDate = smsDate;
        this.smsDateSent = smsDateSent;
        this.smsType = smsType;
        this.smsCreator = smsCreator;
        this.smsPerson = smsPerson;
        this.smsProtocol = smsProtocol;
        this.smsSeen = smsSeen;
        this.smsServiceCenter = smsServiceCenter;
        this.smsStatus = smsStatus;
        this.smsSubject = smsSubject;
        this.smsThreadID = smsThreadId;
        this.smsError = smsError;
        this.smsRead = smsRead;
        this.smsLocked = smsLocked;
        this.smsReplyPathPresent = smsReplyPathPresent;
        this.selected = selected;
    }

    public SmsDataPacket(SmsDataPacket obj) {
        this.smsAddress = obj.smsAddress;
        this.smsBody = obj.smsBody;
        this.smsDate = obj.smsDate;
        this.smsDateSent = obj.smsDateSent;
        this.smsType = obj.smsType;
        this.smsCreator = obj.smsCreator;
        this.smsPerson = obj.smsPerson;
        this.smsProtocol = obj.smsProtocol;
        this.smsSeen = obj.smsSeen;
        this.smsServiceCenter = obj.smsServiceCenter;
        this.smsStatus = obj.smsStatus;
        this.smsSubject = obj.smsSubject;
        this.smsThreadID = obj.smsThreadID;
        this.smsError = obj.smsError;
        this.smsRead = obj.smsRead;
        this.smsLocked = obj.smsLocked;
        this.smsReplyPathPresent = obj.smsReplyPathPresent;
        this.selected = obj.selected;
    }
}
