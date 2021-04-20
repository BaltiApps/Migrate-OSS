package balti.migrate.backupActivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import balti.migrate.AppInstance.Companion.appBackupDataPackets
import balti.migrate.AppInstance.Companion.selectedBackupDataPackets
import balti.migrate.R
import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
import balti.migrate.backupActivity.utils.AppListAdapterKotlin
import balti.migrate.backupActivity.utils.SearchAppAdapter
import balti.migrate.extraBackupsActivity.ExtraBackupsKotlin
import balti.migrate.simpleActivities.ProgressShowActivity
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_BACKUP_PROGRESS
import balti.migrate.utilities.CommonToolsKotlin.Companion.ACTION_REQUEST_BACKUP_DATA
import balti.migrate.utilities.CommonToolsKotlin.Companion.EXTRA_IS_ALL_APP_SELECTED
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_FILE_APPS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_SYSTEM_APPS_WARNING
import balti.migrate.utilities.ExclusionsKotlin
import balti.migrate.utilities.ExclusionsKotlin.Companion.EXCLUDE_APP
import balti.migrate.utilities.ExclusionsKotlin.Companion.EXCLUDE_DATA
import balti.migrate.utilities.ExclusionsKotlin.Companion.EXCLUDE_PERMISSION
import balti.module.baltitoolbox.functions.Misc.doBackgroundTask
import balti.module.baltitoolbox.functions.Misc.tryIt
import balti.module.baltitoolbox.functions.SharedPrefs.getPrefBoolean
import balti.module.baltitoolbox.functions.SharedPrefs.putPrefBoolean
import balti.module.baltitoolbox.jobHandlers.AsyncCoroutineTask
import kotlinx.android.synthetic.main.app_search_layout.view.*
import kotlinx.android.synthetic.main.backup_layout.*

class BackupActivityKotlin : AppCompatActivity() {

    //val appList by lazy { ArrayList<BackupDataPacketKotlin>(0) }

    private val USER_PACKAGES = 0
    private val SYSTEM_NOT_UPDATED_PACKAGES = 1
    private val SYSTEM_UPDATE_ONLY_PACKAGES = 2
    private val ALL_SYSTEM_PACKAGES = 3
    private val USER_SYSTEM_UPDATED_PACKAGES = 4
    private val ALL_PACKAGES = 5

    private val commonTools by lazy { CommonToolsKotlin(this) }

    private val appPrefs by lazy { getSharedPreferences(PREF_FILE_APPS, Context.MODE_PRIVATE) }

    lateinit var adapter: AppListAdapterKotlin

    private val progressReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                startActivity(Intent(this@BackupActivityKotlin, ProgressShowActivity::class.java)   /*kotlin*/
                        .apply {
                            intent?.let {
                                this.putExtras(it)
                                this.action = it.action
                            }
                        }
                )
                tryIt { commonTools.LBM?.unregisterReceiver(this) }
                finish()
            }
        }
    }

    private inner class AppUpdate: AsyncCoroutineTask() {

        override suspend fun onPreExecute() {
            super.onPreExecute()

            appBackupList.adapter = null

            backupActivityNext.isEnabled = false

            appAllSelect.isEnabled = false
            dataAllSelect.isEnabled = false
            permissionsAllSelect.isEnabled = false
            appSearch.isEnabled = false

            selectAll.isEnabled = false
            clearAll.isEnabled = false

            noAppsInCategoryLabel.visibility = View.GONE
            appLoadingView.visibility = View.VISIBLE

            appAllSelect.setOnCheckedChangeListener(null)
            appAllSelect.isChecked = false

            dataAllSelect.setOnCheckedChangeListener(null)
            dataAllSelect.isChecked = false

            permissionsAllSelect.setOnCheckedChangeListener(null)
            permissionsAllSelect.isChecked = false

            appBackupDataPackets.clear()
        }

        override suspend fun doInBackground(arg: Any?): Any? {
            if (arg is Int) updateAllApps(arg)
            return null
        }

        override suspend fun onPostExecute(result: Any?) {
            super.onPostExecute(result)

            if (appBackupDataPackets.size > 0) appBackupList.adapter = adapter
            else {
                appBackupList.invalidate()
                noAppsInCategoryLabel.visibility = View.VISIBLE
            }

            backupActivityNext.isEnabled = true

            appAllSelect.isEnabled = true
            dataAllSelect.isEnabled = true
            permissionsAllSelect.isEnabled = true
            appSearch.isEnabled = true

            selectAll.isEnabled = true
            clearAll.isEnabled = true

            appLoadingView.visibility = View.GONE
        }

        private fun updateAllApps(type: Int){

            val tempAppList = packageManager.getInstalledPackages(0)
            appBackupDataPackets.clear()
            tempAppList.forEach {
                val installer = packageManager.getInstallerPackageName(it.packageName).let {installer ->
                    installer ?: ""
                }
                val p: BackupDataPacketKotlin? = when (type){
                    USER_PACKAGES -> {
                        if (it.applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0){
                            BackupDataPacketKotlin(it, appPrefs, installer)
                        } else null
                    }
                    SYSTEM_NOT_UPDATED_PACKAGES -> {
                        if (it.applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) > 0 &&
                                it.applicationInfo.sourceDir.startsWith("/system")){
                            BackupDataPacketKotlin(it, appPrefs, installer)
                        } else null
                    }
                    SYSTEM_UPDATE_ONLY_PACKAGES -> {
                        if (it.applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) > 0 &&
                                it.applicationInfo.sourceDir.startsWith("/data")){
                            BackupDataPacketKotlin(it, appPrefs, installer)
                        } else null
                    }
                    ALL_SYSTEM_PACKAGES -> {
                        if (it.applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) > 0){
                            BackupDataPacketKotlin(it, appPrefs, installer)
                        } else null
                    }
                    USER_SYSTEM_UPDATED_PACKAGES -> {
                        if (it.applicationInfo.sourceDir.startsWith("/data")){
                            BackupDataPacketKotlin(it, appPrefs, installer)
                        } else null
                    }
                    else -> BackupDataPacketKotlin(it, appPrefs, installer)
                }
                p?.let {appBackupDataPackets.add(p)}
            }
            if (appBackupDataPackets.size > 0) adapter = AppListAdapterKotlin(this@BackupActivityKotlin, appAllSelect, dataAllSelect, permissionsAllSelect)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.backup_layout)

        appType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if ((position == USER_SYSTEM_UPDATED_PACKAGES || position == SYSTEM_NOT_UPDATED_PACKAGES || position == ALL_SYSTEM_PACKAGES || position == SYSTEM_UPDATE_ONLY_PACKAGES)
                        && getPrefBoolean(PREF_SYSTEM_APPS_WARNING, true)){
                    AlertDialog.Builder(this@BackupActivityKotlin)
                            .setTitle(R.string.bootloop_warning)
                            .setMessage(R.string.bootloop_warning_desc)
                            .setPositiveButton(android.R.string.ok, null)
                            .setNegativeButton(R.string.dont_show_again) { _, _ ->
                                putPrefBoolean(PREF_SYSTEM_APPS_WARNING, value = false, immediate = true)
                            }
                            .show()
                }
                AppUpdate().execute(position)
            }

        }

        backupActivityNext.setOnClickListener {
            var isAllAppSelected = true
            doBackgroundTask({
                isAllAppSelected = filterApps()
                return@doBackgroundTask null
            }, {
                startActivity(Intent(this, ExtraBackupsKotlin::class.java)
                        .putExtra(EXTRA_IS_ALL_APP_SELECTED, isAllAppSelected)
                )
            })
        }

        selectAll.setOnClickListener {
            appAllSelect.isChecked = true
            dataAllSelect.isChecked = true
            permissionsAllSelect.isChecked = true
        }

        clearAll.setOnClickListener {
            appAllSelect.isChecked = true
            dataAllSelect.isChecked = true
            permissionsAllSelect.isChecked = true


            appAllSelect.isChecked = false
            dataAllSelect.isChecked = false
            permissionsAllSelect.isChecked = false
        }

        appSearch.setOnClickListener {


            val searchAD = AlertDialog.Builder(this)
                    .setCancelable(false)
                    .create()

            val searchView = View.inflate(this, R.layout.app_search_layout, null)

            searchView.app_search_category_display.text = appType.selectedItem.toString()
            searchView.app_search_close.setOnClickListener {
                searchAD.dismiss()
                adapter.notifyDataSetChanged()
            }

            searchView.app_search_editText.addTextChangedListener(object : TextWatcher{

                var loadApps: LoadSearchApps? = null

                override fun afterTextChanged(s: Editable?) {
                    tryIt {
                        loadApps?.cancel(true)
                    }

                    loadApps = LoadSearchApps(s.toString())
                    loadApps?.execute()
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                inner class LoadSearchApps(val term: String) : AsyncCoroutineTask(){

                    lateinit var adapter: SearchAppAdapter
                    var tmpList = ArrayList<BackupDataPacketKotlin>(0)

                    override suspend fun onPreExecute() {
                        super.onPreExecute()
                        searchView.app_search_list.adapter = null
                        searchView.app_search_loading.visibility = View.VISIBLE
                        searchView.app_search_app_unavailable.visibility = View.GONE
                    }

                    override suspend fun doInBackground(arg: Any?): Any? {

                        if (term.trim() != "") makeTmpList(term)
                        else tmpList.clear()

                        if (tmpList.size > 0) adapter = SearchAppAdapter(tmpList, this@BackupActivityKotlin)

                        return null
                    }

                    override suspend fun onPostExecute(result: Any?) {
                        super.onPostExecute(result)
                        searchView.app_search_loading.visibility = View.GONE
                        if (tmpList.size > 0) searchView.app_search_list.adapter = adapter
                        else {
                            searchView.app_search_list.invalidate()
                            if (term.trim() != "") searchView.app_search_app_unavailable.visibility = View.VISIBLE
                        }
                    }

                    fun makeTmpList(term: String){
                        tmpList.clear()
                        for (dp in appBackupDataPackets){
                            if (dp.PACKAGE_INFO.packageName.contains(term, true)
                                    || packageManager.getApplicationLabel(dp.PACKAGE_INFO.applicationInfo).contains(term, true))
                                tmpList.add(dp)
                        }
                    }
                }
            })

            searchAD.setView(searchView)
            searchAD.window?.run { setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) }
            searchAD.show()

        }

        backupLayoutBackButton.setOnClickListener { finish() }

        backup_activity_help.setOnClickListener {
            AlertDialog.Builder(this)
                    .setView(View.inflate(this, R.layout.backup_activity_help, null))
                    .setPositiveButton(R.string.close, null)
                    .show()
        }

        commonTools.LBM?.registerReceiver(progressReceiver, IntentFilter(ACTION_BACKUP_PROGRESS))
        commonTools.LBM?.sendBroadcast(Intent(ACTION_REQUEST_BACKUP_DATA))
    }

    override fun onDestroy() {
        super.onDestroy()
        tryIt { commonTools.LBM?.unregisterReceiver(progressReceiver) }
    }

    private fun filterApps(): Boolean {
        var isAllAppSelected = true
        val exclusion = ExclusionsKotlin()

        selectedBackupDataPackets.run {
            clear()
            try {
                appBackupDataPackets.forEach {
                    val e = exclusion.returnExclusionState(it.PACKAGE_INFO.packageName)
                    if (isAllAppSelected) {
                        if (!e.contains(EXCLUDE_APP) && !it.APP) isAllAppSelected = false
                        if (!e.contains(EXCLUDE_DATA) && !it.DATA) isAllAppSelected = false
                        if (!e.contains(EXCLUDE_PERMISSION) && !it.PERMISSION) isAllAppSelected = false
                    }
                    if (it.APP || it.DATA || it.PERMISSION) add(it)
                }
            }
            catch (e: Exception){
                e.printStackTrace()
                isAllAppSelected = false
                selectedBackupDataPackets.run {
                    clear()
                    addAll(appBackupDataPackets.filter { it.APP || it.DATA || it.PERMISSION })
                }
            }
        }

        return isAllAppSelected
    }
}