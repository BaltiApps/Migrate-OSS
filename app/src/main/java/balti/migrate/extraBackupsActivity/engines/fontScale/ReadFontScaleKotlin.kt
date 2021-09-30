package balti.migrate.extraBackupsActivity.engines.fontScale

import android.view.View
import balti.migrate.R
import balti.migrate.extraBackupsActivity.utils.ParentReaderForExtras
import balti.migrate.extraBackupsActivity.utils.ReaderJobResultHolder
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc.tryIt
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ReadFontScaleKotlin(fragment: FontScaleFragment): ParentReaderForExtras(fragment) {

    private var error = ""
    private var fontScaleText = ""
    private var scale = -1.0

    override val className: String = "ReadFontScaleKotlin"

    override suspend fun onPreExecute() {
        super.onPreExecute()
        mainItem?.isClickable = false
        doBackupCheckBox?.isEnabled = false
        readStatusText?.visibility = View.GONE
        readProgressBar?.visibility = View.VISIBLE
    }

    override suspend fun backgroundProcessing(): ReaderJobResultHolder {

        writeLog("Starting reading")

        try {
            val fontScaleReader = Runtime.getRuntime().exec("su")
            BufferedWriter(OutputStreamWriter(fontScaleReader.outputStream)).let {

                writeLog("Writing to input buffer")

                heavyTask {
                    it.write("settings get system font_scale\n")
                    it.write("exit\n")
                    it.flush()
                }
            }

            BufferedReader(InputStreamReader(fontScaleReader.inputStream)).let {

                writeLog("Reading result buffer")

                it.readLines().forEach {line ->
                    writeLog("FONT_SCALE_RESULT: \'$line\'")
                    fontScaleText += line + "\n"
                }
            }

            BufferedReader(InputStreamReader(fontScaleReader.errorStream)).let {

                writeLog("Reading error buffer")

                it.readLines().forEach {line ->
                    writeLog("FONT_SCALE_ERROR: \'$line\'")
                    error += line + "\n"
                }
            }

            fontScaleText = fontScaleText.trim()

            if (fontScaleText.isNotBlank() && fontScaleText != "null") tryIt {
                writeLog("Casting to font scale: $fontScaleText")
                scale = fontScaleText.trim().toDouble()
            }

            if (scale <= 0 && scale != -1.0) {
                writeLog("Weird font scale: $fontScaleText")
                error = "${getStringFromRes(R.string.weird_font_scale)} ($scale)\n\n$error".trim()
            }

        } catch (e: Exception){
            error += e.message
            e.printStackTrace()
        }

        return if (error == "") {
            writeLog("Read success. Read - $fontScaleText")
            ReaderJobResultHolder(true, scale)
        } else {
            writeLog("Read fail. Error - $error")
            ReaderJobResultHolder(false, error)
        }

    }

    override suspend fun onPostExecute(result: Any?) {
        super.onPostExecute(result)

        writeLog("Post execute")

        doBackupCheckBox?.isEnabled = true
        readProgressBar?.visibility = View.GONE

        if (error == "") {
            mainItem?.isClickable = true
            readStatusText?.visibility = View.VISIBLE
        }

        writeLog("Post execute complete")
    }

}