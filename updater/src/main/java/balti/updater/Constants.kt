package balti.updater

internal class Constants {

    companion object {

        val FILE_PREF = "updater_pref"
        val PREF_SERVER = "update_server"
        val PREF_SERVER_GITHUB = "Github"
        val PREF_SERVER_GITLAB = "GitLab"
        val PREF_CHANNEL = "update_channel"
        val PREF_CHANNEL_BETA_STABLE = "Beta+Stable"
        val PREF_CHANNEL_STABLE = "Stable"

        val TELEGRAM_GROUP = "t.me/migrateApp"

        val UPDATE_NAME = "name"
        val UPDATE_VERSION = "version"
        val UPDATE_LAST_TESTED_ANDROID = "last_tested_android"
        val UPDATE_STATUS = "status"
        val UPDATE_MESSAGE = "message"
        val UPDATE_URL = "url"
        val UPDATE_ERROR = "error"


        fun UPDATE_URL() : String =
                if (Updater.sharedPreferences.getString(PREF_SERVER, PREF_SERVER_GITLAB) == PREF_SERVER_GITLAB) {
                    if (Updater.sharedPreferences.getString(PREF_CHANNEL, PREF_CHANNEL_BETA_STABLE) == PREF_CHANNEL_BETA_STABLE)
                        "https://gitlab.com/SayantanRC/update-files/-/raw/master/migrate_update_info_beta+stable.txt"
                    else "https://gitlab.com/SayantanRC/update-files/-/raw/master/migrate_update_info_stable.txt"
                } else {
                    if (Updater.sharedPreferences.getString(PREF_CHANNEL, PREF_CHANNEL_BETA_STABLE) == PREF_CHANNEL_BETA_STABLE)
                        ""
                    else ""
                }
    }
}