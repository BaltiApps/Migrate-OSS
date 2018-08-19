package balti.migrate;

public class ContactsDataPacket {
    String fullName;
    String vcfData;
    boolean selected;

    public ContactsDataPacket(String fullName, String vcfData) {
        this.fullName = fullName;
        this.vcfData = vcfData;
        selected = true;
    }
}
