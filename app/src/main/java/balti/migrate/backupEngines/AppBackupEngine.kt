package balti.migrate.backupEngines

import android.content.Context
import android.os.AsyncTask
import balti.migrate.extraBackupsActivity.apps.AppBatch
import javax.inject.Inject

abstract class AppBackupEngine(private var backupName: String, private var destination: String,
                               private var appBatch: AppBatch, private var doBackupInstallers : Boolean) : AsyncTask<Any, Any, Any>() {

    @Inject lateinit var engineContext: Context
    private val backupDependencyComponent: BackupDependencyComponent
            by lazy { DaggerBackupDependencyComponent.create() }

    override fun onPreExecute() {
        super.onPreExecute()
        backupDependencyComponent.inject(this)
    }

    override fun doInBackground(vararg params: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}