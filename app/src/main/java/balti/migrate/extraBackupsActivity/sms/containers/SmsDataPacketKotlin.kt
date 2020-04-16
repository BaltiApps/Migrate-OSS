package balti.migrate.extraBackupsActivity.sms.containers

data class SmsDataPacketKotlin(val smsAddress: String?, val smsBody: String?, val smsDate: String?,
                               val smsDateSent: String?, val smsType: String?, val smsCreator: String?,
                               val smsPerson: String?, val smsProtocol: String?, val smsSeen: String?,
                               val smsServiceCenter: String?, val smsStatus: String?,
                               val smsSubject: String?, val smsThreadID: String?,
                               val smsError: Int, val smsRead: Int, val smsLocked: Int,
                               val smsReplyPathPresent: Int, var selected: Boolean)