package balti.updater

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import balti.updater.Constants.Companion.FILE_PREF
import balti.updater.Constants.Companion.PREF_CHANNEL
import balti.updater.Constants.Companion.PREF_CHANNEL_BETA_STABLE
import balti.updater.Constants.Companion.PREF_CHANNEL_STABLE
import balti.updater.Constants.Companion.PREF_SERVER
import balti.updater.Constants.Companion.PREF_SERVER_GITHUB
import balti.updater.Constants.Companion.PREF_SERVER_GITLAB
import balti.updater.Constants.Companion.UPDATE_ERROR
import balti.updater.Constants.Companion.UPDATE_LAST_TESTED_ANDROID
import balti.updater.Constants.Companion.UPDATE_MESSAGE
import balti.updater.Constants.Companion.UPDATE_NAME
import balti.updater.Constants.Companion.UPDATE_STATUS
import balti.updater.Constants.Companion.UPDATE_URL
import balti.updater.Constants.Companion.UPDATE_VERSION

class Updater {

    companion object{

        internal lateinit var context: Context
        internal var thisVersion: Int = 0

        internal val sharedPreferences by lazy { context.getSharedPreferences(FILE_PREF, MODE_PRIVATE) }
        internal val editor by lazy { sharedPreferences.edit() }

        fun init(context: Context, thisVersion: Int){
            this.context = context
            this.thisVersion = thisVersion
        }

        suspend fun isUpdateAvailable(): Boolean = GetUpdateInfo(context).isUpdateAvailable()

        fun launchUpdaterScreen(){
            context.startActivity(Intent(context, UpdaterMain::class.java).setFlags(FLAG_ACTIVITY_NEW_TASK))
        }

        fun getJsonKeys(): Array<String> = arrayOf(UPDATE_NAME, UPDATE_VERSION, UPDATE_LAST_TESTED_ANDROID,
                UPDATE_STATUS, UPDATE_MESSAGE, UPDATE_URL, UPDATE_ERROR)

        fun getUpdateServers(): Array<String> = arrayOf(PREF_SERVER_GITHUB, PREF_SERVER_GITLAB)
        fun getUpdateActiveServer(): String = sharedPreferences.getString(PREF_SERVER, PREF_SERVER_GITLAB).let { it?:"" }
        fun setUpdateActiveServer(server : String) {
            if (server in getUpdateServers()) editor.putString(PREF_SERVER, server).apply()
        }

        fun getChannels(): Array<String> = arrayOf(PREF_CHANNEL_BETA_STABLE, PREF_CHANNEL_STABLE)
        fun getActiveChannel(): String = sharedPreferences.getString(PREF_CHANNEL, PREF_CHANNEL_BETA_STABLE).let { it?:"" }
        fun setActiveChannel(channel : String) {
            if (channel in getChannels()) editor.putString(PREF_CHANNEL, channel).apply()
        }
    }

}