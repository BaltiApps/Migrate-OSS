package balti.updater

import android.content.Context
import balti.updater.Constants.Companion.UPDATE_ERROR
import balti.updater.Constants.Companion.UPDATE_URL
import balti.updater.Constants.Companion.UPDATE_VERSION
import balti.updater.Updater.Companion.thisVersion
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL

internal class GetUpdateInfo(context: Context) {

    private val dFile by lazy { File(context.filesDir, "update_info.txt") }

    private fun downloadData(){
        dFile.delete()
        URL(UPDATE_URL()).openStream().use { input ->
            FileOutputStream(dFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    suspend fun getInfo(getError: Boolean = false): JSONObject {
        return try {
            withContext(IO) {
                downloadData()
            }
            withContext(Default) {
                val data = StringBuilder("")
                dFile.readLines().forEach {
                    data.append(it)
                }
                JSONObject(data.toString())
            }
        }
        catch (e: Exception){
            if (getError) JSONObject().put(UPDATE_ERROR, "${e.message}")
            else JSONObject()
        }
    }

    suspend fun isUpdateAvailable(): Boolean {
        val info = getInfo(false)
        return if (info.has(UPDATE_VERSION)) {
            try {
                info.getInt(UPDATE_VERSION) > thisVersion
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } else false
    }

}