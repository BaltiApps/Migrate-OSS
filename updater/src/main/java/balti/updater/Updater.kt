package balti.updater

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import balti.updater.Constants.Companion.FILE_PREF
import balti.updater.Constants.Companion.PREF_CHANNEL
import balti.updater.Constants.Companion.PREF_CHANNEL_BETA_STABLE
import balti.updater.Constants.Companion.PREF_CHANNEL_STABLE
import balti.updater.Constants.Companion.PREF_HOST
import balti.updater.Constants.Companion.PREF_HOST_GITHUB
import balti.updater.Constants.Companion.PREF_HOST_GITLAB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

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

        fun isUpdateAvailable(): Boolean = runBlocking {  GetUpdateInfo(context).isUpdateAvailable() }
        fun onUpdateAvailable(f: () -> Unit){
            CoroutineScope(Default).launch{
                if (isUpdateAvailable()) withContext(Main) { Tools().tryIt(f) }
            }
        }

        fun launchUpdaterScreen(){
            context.startActivity(Intent(context, UpdaterMain::class.java).setFlags(FLAG_ACTIVITY_NEW_TASK))
        }

        fun getUpdateHosts(): Array<String> = arrayOf(PREF_HOST_GITHUB, PREF_HOST_GITLAB)
        fun getUpdateActiveHost(): String = sharedPreferences.getString(PREF_HOST, PREF_HOST_GITLAB).let { it?:"" }
        fun setUpdateActiveHost(host : String) {
            if (host in getUpdateHosts()) editor.putString(PREF_HOST, host).apply()
        }

        fun getChannels(): Array<String> = arrayOf(PREF_CHANNEL_BETA_STABLE, PREF_CHANNEL_STABLE)
        fun getActiveChannel(): String = sharedPreferences.getString(PREF_CHANNEL, PREF_CHANNEL_BETA_STABLE).let { it?:"" }
        fun setActiveChannel(channel : String) {
            if (channel in getChannels()) editor.putString(PREF_CHANNEL, channel).apply()
        }
    }

}