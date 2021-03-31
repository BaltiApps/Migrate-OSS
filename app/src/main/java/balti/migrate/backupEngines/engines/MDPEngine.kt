package balti.migrate.backupEngines.engines

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import balti.filex.FileX
import balti.migrate.AppInstance.Companion.CACHE_DIR
import balti.migrate.R
import balti.migrate.backupEngines.BackupServiceKotlin
import balti.migrate.backupEngines.ParentBackupClass
import balti.migrate.backupEngines.containers.BackupIntentData
import balti.migrate.extraBackupsActivity.apps.containers.MDP_Packet
import balti.migrate.utilities.CommonToolsKotlin.Companion.ERR_MDP_TRY_CATCH
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_PROGRESS_TYPE_MDP
import balti.migrate.utilities.CommonToolsKotlin.Companion.FILE_MDP_LOG
import balti.migrate.utilities.CommonToolsKotlin.Companion.WARNING_MDP_PROGRESS_COPY
import balti.migrate.utilities.constants.MDP_Constants
import balti.module.baltitoolbox.functions.GetResources.getStringFromRes
import balti.module.baltitoolbox.functions.Misc

class MDPEngine(private val jobcode: Int,
                private val bd: BackupIntentData,
                private val busyboxPath: String,
                private val ignoreCache: Boolean,
                private val mdpPacket: MDP_Packet): ParentBackupClass(bd, EXTRA_PROGRESS_TYPE_MDP) {

    private val mdpPackageListFile by lazy { FileX.new(actualDestination, mdpPacket.fileName) }
    private val errors by lazy { ArrayList<String>(0) }
    private val warnings by lazy { ArrayList<String>(0) }
    private val LBM by lazy { LocalBroadcastManager.getInstance(engineContext) }

    private var handler : Handler? = null
    private var runnable: Runnable? = null

    private var exitWait = false
    private var alreadyFinished = false

    private var CANCELLED_MAX_COUNT = 10
    private var alreadyCancelled = false

    private val newBusybox: FileX by lazy {
        val newBusyboxFile = FileX.new(actualDestination, "busybox")
        FileX.new(busyboxPath, true).copyTo(newBusyboxFile)
        newBusyboxFile
    }

    private val mdpResultsReceiver by lazy {
        object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {

                    val type = it.getStringExtra(MDP_Constants.EXTRA_OPERATION_TYPE)

                    if (type == MDP_Constants.EXTRA_OPERATION_BACKUP_DONE){
                        it.getStringArrayListExtra(MDP_Constants.EXTRA_ERRORS)?.let { e ->
                            errors.addAll(e)
                        }
                        if (it.getBooleanExtra(MDP_Constants.EXTRA_WAS_CANCELLED, false)) {
                            broadcastProgress("", getStringFromRes(R.string.mdp_cancelled), true)
                        }
                        else {
                            broadcastProgress("",
                                    if (errors.isEmpty()) getStringFromRes(R.string.mdp_finished)
                                    else getStringFromRes(R.string.mdp_finished_with_errors),
                                    true)
                        }
                        try {
                            it.getParcelableExtra<Uri>(MDP_Constants.EXTRA_PROGRESS_LOG_URI)?.let { fileUri ->
                                val inputStream = engineContext.contentResolver.openInputStream(fileUri)
                                val outputStream = FileX.new(CACHE_DIR, FILE_MDP_LOG, true).let {
                                    it.createNewFile()
                                    it.outputStream()
                                }
                                inputStream!!.use { input ->
                                    outputStream!!.use { output ->
                                        input.copyTo(output, DEFAULT_BUFFER_SIZE)
                                    }
                                }
                            }
                        }
                        catch (e: Exception){
                            e.printStackTrace()
                            warnings.add("$WARNING_MDP_PROGRESS_COPY: ${getStringFromRes(R.string.mdp_progress_copy_failed)} - ${e.message}")
                        }
                        exitWait = true
                    }
                    else {
                        val progressIn100 = it.getIntExtra(MDP_Constants.EXTRA_PROGRESS_IN_100, 1)
                        val title = it.getStringExtra(MDP_Constants.EXTRA_TITLE) ?: engineContext.getString(R.string.running_mdp_plugin)
                        val packageName = it.getStringExtra(MDP_Constants.EXTRA_PACKAGE_NAME) ?: ""
                        broadcastProgress(title, packageName, true, progressIn100)
                    }
                }
            }
        }
    }

    private fun backupData() {

        Thread.sleep(MDP_Constants.MDP_DELAY)

        engineContext.startActivity(
                MDP_Constants.getMdpIntent(Bundle().apply {
                    putString(MDP_Constants.EXTRA_BACKUP_PACKAGES_LIST_FILE_LOCATION, mdpPackageListFile.canonicalPath)
                    putString(MDP_Constants.EXTRA_BACKUP_LOCATION, FileX.new("/").canonicalPath)
                    putString(MDP_Constants.EXTRA_BUSYBOX_LOCATION, newBusybox.canonicalPath)
                    putBoolean(MDP_Constants.EXTRA_IGNORE_CACHE, ignoreCache)
                    putInt(MDP_Constants.EXTRA_NUMBER_OF_APPS, mdpPacket.dataPackageNames.size)
                }, MDP_Constants.EXTRA_OPERATION_BACKUP_START)
        )

        runnable = Runnable {
            handler?.run {

                if (exitWait || CANCELLED_MAX_COUNT == 0) {
                    runnable?.let { removeCallbacks(it) }
                    finishJob()
                }
                else {

                    if (BackupServiceKotlin.cancelAll){
                        if (!alreadyCancelled){
                            engineContext.startActivity(MDP_Constants.getMdpIntent(Bundle(), MDP_Constants.EXTRA_OPERATION_STOP))
                            alreadyCancelled = true
                        }
                        CANCELLED_MAX_COUNT--
                    }

                    runnable?.let { postDelayed(it, 500) }
                }
            }
        }

        handler = Handler(Looper.getMainLooper()).apply {
            post(runnable!!)
        }

    }

    private fun finishJob(){
        if (!alreadyFinished) {

            alreadyFinished = true
            Misc.tryIt { LBM.unregisterReceiver(mdpResultsReceiver) }

            newBusybox.delete()

            onEngineTaskComplete.onComplete(jobcode, errors, warnings)
        }
    }

    override suspend fun doInBackground(arg: Any?): Any {
        try {

            FileX.new(actualDestination).mkdirs()
            if (mdpPackageListFile.exists()) mdpPackageListFile.delete()

            val title = getTitle(R.string.running_mdp_plugin)

            resetBroadcast(true, title)

            heavyTask {
                mdpPackageListFile.startWriting(object : FileX.Writer(){
                    override fun writeLines() {
                        for (packageName in mdpPacket.dataPackageNames) {
                            if (BackupServiceKotlin.cancelAll) break
                            writeLine(packageName)
                        }
                    }
                })


            }

            LBM.registerReceiver(mdpResultsReceiver, IntentFilter(MDP_Constants.ACTION_TO_MDP_ENGINE))

            try {
                backupData()
            }
            catch (e: Exception) {
                e.printStackTrace()
                errors.add("$ERR_MDP_TRY_CATCH: ${getStringFromRes(R.string.mdp_function_error)} - ${e.message}")
                finishJob()
            }
        }
        catch (e: Exception){
            e.printStackTrace()
            errors.add("$ERR_MDP_TRY_CATCH: ${e.message}")
            finishJob()
        }

        return 0
    }

    override fun postExecuteFunction() {}

}