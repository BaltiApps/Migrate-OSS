package balti.migrate.simpleActivities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.filex.FileX
import balti.migrate.R
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.SIMPLE_LOG_VIEWER_FILEPATH
import balti.migrate.utilities.CommonToolsKotlin.Companion.SIMPLE_LOG_VIEWER_HEAD
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import kotlinx.android.synthetic.main.simple_log_display.*


class SimpleLogViewer: AppCompatActivity() {

    private val commonTools by lazy { CommonToolsKotlin(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_log_display)

        logViewBackButton.setOnClickListener {
            finish()
        }

        logSendButton.setOnClickListener {
            commonTools.reportLogs(false)
        }

        class LoadLogText: AsyncCoroutineTask(){

            lateinit var err : String
            lateinit var filePath : String

            override suspend fun onPreExecute() {
                super.onPreExecute()
                log_view_progress_bar.visibility = View.VISIBLE
                logBody.text = ""
                err = ""

                intent?.let {
                    if (!it.hasExtra(SIMPLE_LOG_VIEWER_HEAD))
                        err += getString(R.string.no_header) + "\n"
                    else logViewHeader.text = it.getStringExtra(SIMPLE_LOG_VIEWER_HEAD)

                    //if (!it.hasExtra(SIMPLE_LOG_VIEWER_FILEPATH))
                    it.getStringExtra(SIMPLE_LOG_VIEWER_FILEPATH).let {
                        if (it == null)
                            err += getString(R.string.no_filepath) + "\n"
                        else filePath = it
                    }
                }

                err = err.trim()
            }

            override suspend fun doInBackground(arg: Any?): Any? {

                try {
                    intent?.let {

                        if (err != "") return 1

                        // logs are stored in external private storage of the app.
                        // Conventional Java file works pretty good there, hence setting conventional flag as true.
                        FileX.new(filePath, true).readLines().forEach { it1 ->
                            publishProgress(it1)
                            //tryIt { Thread.sleep(1) }
                            sleepTask(1)
                        }

                        return 0
                    }
                }
                catch (e: Exception){
                    e.printStackTrace()
                    err += e.message.toString() + "\n"
                    return 1
                }

                return 1
            }

            override suspend fun onProgressUpdate(vararg values: Any) {
                super.onProgressUpdate(*values)
                logBody.append("${values[0]}\n")
            }

            override suspend fun onPostExecute(result: Any?) {
                super.onPostExecute(result)

                log_view_progress_bar.visibility = View.GONE

                if (err != "") {
                    AlertDialog.Builder(this@SimpleLogViewer)
                            .setTitle(R.string.log_reading_error)
                            .setMessage(err)
                            .setNegativeButton(R.string.close) {_, _ ->
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                }
            }

        }

        LoadLogText().execute()
    }

}