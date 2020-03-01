package balti.updater

internal class Constants {

    companion object {

        val FILE_PREF = "updater_pref"
        val FILE_UPDATER_INFO = "update_info.txt"
        val FILE_UPDATE_APK = "update_apk.apk"

        val OK = "OK"

        val PREF_HOST = "update_host"
        val PREF_HOST_GITHUB = "Github"
        val PREF_HOST_GITLAB = "GitLab"
        val PREF_CHANNEL = "update_channel"
        val PREF_CHANNEL_BETA_STABLE = "Beta+Stable"
        val PREF_CHANNEL_STABLE = "Stable"

        val PREF_INSTALL_METHOD = "install_method"
        val PREF_INSTALL_PM = 332
        val PREF_INSTALL_SU = 333

        val TELEGRAM_GROUP = "t.me/migrateApp"

        val UPDATE_NAME = "name"
        val UPDATE_VERSION = "version"
        val UPDATE_LAST_TESTED_ANDROID = "last_tested_android"
        val UPDATE_STATUS = "status"
        val UPDATE_MESSAGE = "message"
        val UPDATE_URL = "url"
        val UPDATE_ERROR = "error"

        val NOTIFICATION_CHANNEL_DOWNLOAD = "Update download"
        val NOTIFICATION_ID = 113

        val EXTRA_DOWNLOAD_URL = "download_url"
        val EXTRA_CANCEL_DOWNLOAD = "cancel_download"
        val EXTRA_ENTIRE_JSON_DATA = "entire_json_data"
        val EXTRA_DOWNLOAD_FINISHED = "download_finished"
        val EXTRA_DOWNLOAD_MESSAGE = "download_final_message"
        val EXTRA_FILE_SIZE = "download_file_size"
        val EXTRA_HOST = "download_host"

        fun UPDATE_URL() : String =
                if (Updater.sharedPreferences.getString(PREF_HOST, PREF_HOST_GITLAB) == PREF_HOST_GITLAB) {
                    if (Updater.sharedPreferences.getString(PREF_CHANNEL, PREF_CHANNEL_BETA_STABLE) == PREF_CHANNEL_BETA_STABLE)
                        "https://gitlab.com/SayantanRC/update-files/-/raw/master/migrate/migrate_update_info_beta+stable.txt"
                    else "https://gitlab.com/SayantanRC/update-files/-/raw/master/migrate/migrate_update_info_stable.txt"
                } else {
                    if (Updater.sharedPreferences.getString(PREF_CHANNEL, PREF_CHANNEL_BETA_STABLE) == PREF_CHANNEL_BETA_STABLE)
                        ""
                    else ""
                }
    }
}