package balti.migrate.utilities

import balti.migrate.AppInstance
import balti.migrate.utilities.CommonToolKotlin.Companion.UPDATE_URL
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class GetUpdateInfo {

    companion object {
        val UPDATE_NAME = "name"
        val UPDATE_VERSION = "version"
        val UPDATE_LAST_TESTED_ANDROID = "last_tested_android"
        val UPDATE_STATUS = "status"
        val UPDATE_MESSAGE = "message"
        val UPDATE_URL = "url"
    }

    private val dFile by lazy {File(AppInstance.appContext.filesDir, "update_info.txt")}

    private fun downloadData(){
        URL(UPDATE_URL()).openStream().use { input ->
            FileOutputStream(dFile.apply { this.delete() }).use { output ->
                input.copyTo(output)
            }
        }
    }

    suspend fun getInfo(): JSONObject {
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
            JSONObject()
        }
    }

}