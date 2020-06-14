package balti.module.baltitoolbox.jobHandlers

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main

abstract class AsyncCoroutineTask(private val backgroundDispatcher: CoroutineDispatcher = Default) {
    // similar to AsyncTask but using Coroutines

    companion object{
        val INIT = "initialised"
        val RUNNING = "running"
        val FINISHED = "finished"
        val CANCELLED = "cancelled"

        val DISP_IO = Dispatchers.IO
        val DISP_MAIN = Main
        val DISP_DEF = Default
    }

    var status: String = INIT
        private set
    private var jobResult: Any? = null

    private lateinit var bgrJob: Job
    private lateinit var postJob: Job

    private var doPostJob = true

    open suspend fun onPreExecute(){}
    abstract suspend fun doInBackground(arg: Any? = null): Any?
    open suspend fun onPostExecute(result: Any? = null){}
    open suspend fun onProgressUpdate(vararg values: Any){}
    open suspend fun onCancelled(){}

    fun publishProgress(vararg values: Any){
        MainScope().launch {
            onProgressUpdate(*values)
        }
    }

    fun cancel(doPostJob: Boolean = false){
        this.doPostJob = doPostJob
        status = CANCELLED
        if (::bgrJob.isInitialized) bgrJob.cancel()
        if (!doPostJob && ::postJob.isInitialized) postJob.cancel()
        CoroutineScope(DISP_MAIN).launch {
            onCancelled()
        }
    }

    private suspend fun executeBody(arg: Any? = null){
        status = RUNNING
        withContext(DISP_MAIN) {
            onPreExecute()
        }
        bgrJob = CoroutineScope(backgroundDispatcher).launch {
            jobResult = doInBackground(arg)
        }
        bgrJob.join()
        if (doPostJob) {
            postJob = CoroutineScope(DISP_MAIN).launch {
                onPostExecute(jobResult)
            }
            postJob.join()
        }
        if (status == RUNNING) status = FINISHED
    }

    fun execute(arg: Any? = null) {
        CoroutineScope(DISP_DEF).launch {
            executeBody(arg)
        }
    }

    suspend fun executeWithResult(arg: Any? = null): Any? {
        executeBody(arg)
        return jobResult
    }

    suspend fun sleepTask(millis: Long){
        delay(millis)
    }

    suspend fun heavyTask(f: () -> Unit){
        withContext(DISP_IO){
            f()
        }
    }

    fun doOnMainThreadParallel(f: () -> Unit){
        MainScope().launch {
            f()
        }
    }
}