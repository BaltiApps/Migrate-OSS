package balti.migrate.extraBackupsActivity.engines.adb

import android.view.View
import balti.migrate.R
import balti.migrate.extraBackupsActivity.utils.ParentReaderForExtras
import balti.migrate.extraBackupsActivity.utils.ReaderJobResultHolder
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ReadAdbKotlin(fragment: AdbFragment): ParentReaderForExtras(fragment) {

    private var error = ""
    private var adbText = ""
    private var adbState = -1

    override val className: String = "ReadAdbKotlin"

    override fun preExecute() {
        mainItem?.isClickable = false
        doBackupCheckBox?.isEnabled = false
        readStatusText?.visibility = View.GONE
        readProgressBar?.visibility = View.VISIBLE
    }

    override suspend fun backgroundProcessing(): ReaderJobResultHolder {

        writeLog("Starting reading")

        try {
            val adbReader = Runtime.getRuntime().exec("su")
            BufferedWriter(OutputStreamWriter(adbReader.outputStream)).let {

                writeLog("Writing to input buffer")

                heavyTask {
                    it.write("settings get global adb_enabled\n")
                    it.write("exit\n")
                    it.flush()
                }
            }

            BufferedReader(InputStreamReader(adbReader.inputStream)).let {

                writeLog("Reading result buffer")

                it.readLines().forEach {line ->
                    writeLog("ADB_RESULT: \'$line\'")
                    adbText += line + "\n"
                }
            }

            BufferedReader(InputStreamReader(adbReader.errorStream)).let {

                writeLog("Reading error buffer")

                it.readLines().forEach {line ->
                    writeLog("ADB_ERROR: \'$line\'")
                    error += line + "\n"
                }
            }

            adbText = adbText.trim()

            if (adbText.isBlank() || adbText == "null") {
                // Usually means ADB is not toggled. Hence return 0.
                writeLog("Blank ADB state: $adbText. Returning default value: $adbState")
            }
            else try {
                writeLog("Casting to ADB: $adbText")
                adbState = adbText.trim().toInt()
            } catch (e: Exception){
                writeLog("Casting to ADB exception: ${e.message}")
                error += e.message + "\n"
            }

            if (adbState !in arrayOf(0,1,-1))
                error = "${getStringFromRes(R.string.adb_unknown)} ($adbState)\n\n$error".trim()

        } catch (e: Exception){
            error += adbText + "\n\n"
            error += e.message
            e.printStackTrace()
        }

        error = error.trim()

        return if (error == "") {
            writeLog("Read success. Read - $adbState")
            ReaderJobResultHolder(true, adbState)
        } else {
            writeLog("Read fail. Error - $error")
            ReaderJobResultHolder(false, error)
        }

    }

    override fun postExecute(result: Any?) {

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