package balti.migrate.extraBackupsActivity.engines.installers

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import balti.migrate.R
import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.FDROID_POSITION
import balti.migrate.utilities.CommonToolsKotlin.Companion.NOT_SET_POSITION
import balti.migrate.utilities.CommonToolsKotlin.Companion.PACKAGE_NAMES_PACKAGE_INSTALLER
import balti.migrate.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_FDROID
import balti.migrate.utilities.CommonToolsKotlin.Companion.PACKAGE_NAME_PLAY_STORE
import balti.migrate.utilities.CommonToolsKotlin.Companion.PLAY_STORE_POSITION
import balti.migrate.utilities.IconTools
import kotlinx.android.synthetic.main.installer_set_item.view.*

class InstallerListAdapter_legacy(val context: Context,
                                  private val appList: ArrayList<BackupDataPacketKotlin>): BaseAdapter() {

    private val pm by lazy { context.packageManager }
    private val iconTools by lazy { IconTools() }

    private var isPlayStoreAvailable = false
    private var isFdroidAvailable = false

    val installerCopiedAppList by lazy { ArrayList<BackupDataPacketKotlin>(0) }

    init {

        pm.getInstalledPackages(0).forEach {
            if (it.packageName == PACKAGE_NAME_PLAY_STORE) isPlayStoreAvailable = true
            if (it.packageName == PACKAGE_NAME_FDROID) isFdroidAvailable = true
        }

        installerCopiedAppList.addAll(appList)
        installerCopiedAppList.sortWith(Comparator { o1, o2 ->
            String.CASE_INSENSITIVE_ORDER.compare(pm.getApplicationLabel(o1.PACKAGE_INFO.applicationInfo).toString(), pm.getApplicationLabel(o2.PACKAGE_INFO.applicationInfo).toString())
        })
        appList.sortWith(Comparator { o1, o2 ->
            String.CASE_INSENSITIVE_ORDER.compare(pm.getApplicationLabel(o1.PACKAGE_INFO.applicationInfo).toString(), pm.getApplicationLabel(o2.PACKAGE_INFO.applicationInfo).toString())
        })
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val viewHolder : ViewHolder
        var view = convertView
        val appItem = installerCopiedAppList[position]

        if (view == null){

            view = View.inflate(context, R.layout.installer_set_item, null)

            viewHolder = ViewHolder()
            viewHolder.appIcon = view.installer_item_icon
            viewHolder.appName = view.installer_item_appName
            viewHolder.packageName = view.installer_item_package
            viewHolder.installerSpinner = view.installer_item_spinner

            view.tag = viewHolder
        }
        else viewHolder = view.tag as ViewHolder

        val installerDisplayList = ArrayList<String>(0)

        installerDisplayList.add(context.getString(R.string.not_set))
        if (isPlayStoreAvailable) installerDisplayList.add(context.getString(R.string.play_store))
        if (isFdroidAvailable) installerDisplayList.add(context.getString(R.string.f_droid))

        appList[position].installerName.run {
            if (this != PACKAGE_NAME_PLAY_STORE && this != PACKAGE_NAME_FDROID && this != "" && this !in PACKAGE_NAMES_PACKAGE_INSTALLER)
                installerDisplayList.add(this)
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, installerDisplayList)

        viewHolder.installerSpinner.apply {
            this.adapter = adapter
            setSelection(appItem.installerName.let {
                when (it) {
                    PACKAGE_NAME_PLAY_STORE -> PLAY_STORE_POSITION
                    PACKAGE_NAME_FDROID -> FDROID_POSITION
                    else -> NOT_SET_POSITION
                }
            })
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    when(position){
                        NOT_SET_POSITION -> appItem.installerName = ""
                        PLAY_STORE_POSITION -> appItem.installerName = PACKAGE_NAME_PLAY_STORE
                        FDROID_POSITION -> appItem.installerName = PACKAGE_NAME_FDROID
                    }
                }

            }
        }

        iconTools.loadIconFromApplication(viewHolder.appIcon, appItem, pm)

        viewHolder.appName.text = pm.getApplicationLabel(appItem.PACKAGE_INFO.applicationInfo)
        viewHolder.packageName.text = appItem.PACKAGE_INFO.packageName

        return view!!

    }

    fun checkAll(installerIndex: Int){
        for (item in installerCopiedAppList) {
            when (installerIndex) {
                NOT_SET_POSITION -> item.installerName = ""
                PLAY_STORE_POSITION -> item.installerName = PACKAGE_NAME_PLAY_STORE
                FDROID_POSITION -> item.installerName = PACKAGE_NAME_FDROID
            }
        }
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): Any = appList[position]
    override fun getItemId(position: Int): Long = 0
    override fun getCount(): Int = appList.size
    override fun getViewTypeCount(): Int = count
    override fun getItemViewType(position: Int): Int = position

    private class ViewHolder {

        lateinit var appIcon: ImageView
        lateinit var appName: TextView
        lateinit var packageName: TextView
        lateinit var installerSpinner: Spinner
    }
}