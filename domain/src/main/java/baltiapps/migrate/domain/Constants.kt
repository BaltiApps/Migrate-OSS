package baltiapps.migrate.domain

const val BACKUP_FILE_NAME_CONTACTS = "contacts.db"
const val BACKUP_FILE_NAME_CALL_LOGS = "call_logs.db"
const val BACKUP_FILE_NAME_SMS = "sms.db"
const val BACKUP_LOG = "backup_log.txt"
const val BACKUP_ERROR_LOG = "backup_error_log.txt"
const val RESTORE_LOG = "restore_log.txt"
const val RESTORE_ERROR_LOG = "restore_error_log.txt"

const val ACTION_START_BACKUP = "action_start_backup"
const val ACTION_CANCEL_BACKUP = "action_stop_backup"

const val ACTION_START_RESTORE = "action_start_restore"
const val ACTION_CANCEL_RESTORE = "action_stop_restore"

const val EXTRA_BACKUP_LOCATION = "backup_location"
const val EXTRA_BACKUP_NAME = "backup_name"

const val DEFAULT_BACKUP_ROOT = "/storage/emulated/0/Migrate"

const val INTERNAL_ROUGH_WORK_DIRECTORY = "rough_work"

const val BREAK_LINE = "===================================="

class CallLogDBConstants {
    companion object{
        const val CALLS_TABLE_NAME = "calls"

        const val CALLS_CACHED_NAME = "callsCachedName"
        const val CALLS_COUNTRY_ISO = "callsCountryIso"
        const val CALLS_DATA_USAGE = "callsDataUsage"
        const val CALLS_FEATURES = "callsFeatures"
        const val CALLS_GEOCODED_LOCATION = "callsGeocodedLocation"
        const val CALLS_IS_READ = "callsIsRead"
        const val CALLS_NUMBER = "callsNumber"
        const val CALLS_NUMBER_PRESENTATION = "callsNumberPresentation"
        const val CALLS_PHONE_ACCOUNT_COMPONENT_NAME = "callsPhoneAccountComponentName"
        const val CALLS_PHONE_ACCOUNT_ID = "callsPhoneAccountId"
        const val CALLS_TRANSCRIPTION = "callsTranscription"
        const val CALLS_TYPE = "callsType"
        const val CALLS_VOICEMAIL_URI = "callsVoicemailUri"
        const val CALLS_DATE = "callsDate"
        const val CALLS_DURATION = "callsDuration"
        const val CALLS_NEW = "callsNew"
    }
}

class SmsDBConstant {
    companion object{
        const val SMS_TABLE_NAME = "sms"

        const val SMS_ADDRESS = "smsAddress"
        const val SMS_BODY = "smsBody"
        const val SMS_DATE = "smsDate"
        const val SMS_DATE_SENT = "smsDateSent"
        const val SMS_TYPE = "smsType"
        const val SMS_PERSON = "smsPerson"
        const val SMS_PROTOCOL = "smsProtocol"
        const val SMS_SEEN = "smsSeen"
        const val SMS_SERVICE_CENTER = "smsServiceCenter"
        const val SMS_STATUS = "smsStatus"
        const val SMS_SUBJECT = "smsSubject"
        const val SMS_THREAD_ID = "smsThreadId"
        const val SMS_ERROR_CODE = "smsErrorCode"
        const val SMS_READ = "smsRead"
        const val SMS_LOCKED = "smsLocked"
        const val SMS_REPLY_PATH_PRESENT = "smsReplyPathPresent"
    }
}

class ContactsDBConstants {
    companion object {
        const val CONTACTS_TABLE_NAME = "contacts"

        const val DISPLAY_NAME = "displayName"
        const val DISPLAY_NUMBER = "displayNumber"
        const val VCF_CONTENT = "vcfContent"
    }
}

class PermissionConstants {
    companion object {
        const val MANAGE_EXTERNAL_STORAGE = "android.permission.MANAGE_EXTERNAL_STORAGE"
        const val DEFAULT_SMS_APP = "android.provider.Telephony.ACTION_CHANGE_DEFAULT"
    }
}