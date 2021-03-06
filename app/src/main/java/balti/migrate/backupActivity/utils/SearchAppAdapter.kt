package balti.migrate.backupActivity.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import balti.migrate.R
import balti.migrate.backupActivity.containers.BackupDataPacketKotlin
import balti.migrate.utilities.CommonToolsKotlin.Companion.PREF_FILE_APPS
import balti.migrate.utilities.CommonToolsKotlin.Companion.PROPERTY_APP_SELECTION
import balti.migrate.utilities.CommonToolsKotlin.Companion.PROPERTY_DATA_SELECTION
import balti.migrate.utilities.CommonToolsKotlin.Companion.PROPERTY_PERMISSION_SELECTION
import balti.migrate.utilities.ExclusionsKotlin.Companion.EXCLUDE_APP
import balti.migrate.utilities.ExclusionsKotlin.Companion.EXCLUDE_DATA
import balti.migrate.utilities.ExclusionsKotlin.Companion.EXCLUDE_PERMISSION
import balti.migrate.utilities.IconTools
import kotlinx.android.synthetic.main.app_info.view.*
import kotlinx.android.synthetic.main.app_item.view.*
import java.util.*

class SearchAppAdapter(private val tmpList: ArrayList<BackupDataPacketKotlin>, private val context: Context): BaseAdapter() {

    private val pm: PackageManager by lazy { context.packageManager }
    private val main: SharedPreferences by lazy { context.getSharedPreferences(PREF_FILE_APPS, Context.MODE_PRIVATE) }
    private val editor: SharedPreferences.Editor by lazy { main.edit() }
    private val iconTools by lazy { IconTools() }

    init {
        tmpList.sortWith(Comparator { o1, o2 ->
            String.CASE_INSENSITIVE_ORDER.compare(pm.getApplicationLabel(o1.PACKAGE_INFO.applicationInfo).toString(), pm.getApplicationLabel(o2.PACKAGE_INFO.applicationInfo).toString())
        })
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder : ViewHolder
        var view = convertView

        if (view == null){

            view = View.inflate(context, R.layout.app_item, null)

            viewHolder = ViewHolder()
            viewHolder.appIcon = view.appIcon
            viewHolder.appName = view.appName
            viewHolder.appInfo = view.appInfo
            viewHolder.appCheckBox = view.appCheckbox
            viewHolder.dataCheckBox = view.dataCheckbox
            viewHolder.permCheckBox = view.permissionCheckbox

            view.tag = viewHolder
        }
        else viewHolder = view.tag as ViewHolder

        val appItem = tmpList[position]

        viewHolder.appName.text = pm.getApplicationLabel(appItem.PACKAGE_INFO.applicationInfo)
        iconTools.loadIconFromApplication(viewHolder.appIcon, appItem, pm)

        viewHolder.appInfo.setOnClickListener {

            val adView = View.inflate(context, R.layout.app_info, null)
            adView.app_info_package_name.text = context.getString(R.string.packageName) + " " + appItem.PACKAGE_INFO.packageName
            adView.app_info_version_name.text = context.getString(R.string.versionName) + " " + appItem.PACKAGE_INFO.versionName
            adView.app_info_uid.text = context.getString(R.string.uid) + " " + appItem.PACKAGE_INFO.applicationInfo.uid
            adView.app_info_source_dir.text = context.getString(R.string.sourceDir) + " " + appItem.PACKAGE_INFO.applicationInfo.sourceDir
            adView.app_info_data_dir.text = context.getString(R.string.dataDir) + " " + appItem.PACKAGE_INFO.applicationInfo.dataDir

            AlertDialog.Builder(context)
                    .setTitle(viewHolder.appName.text)
                    .setView(adView)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }

        if (appItem.PACKAGE_INFO.applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) > 0) {
            if (appItem.PACKAGE_INFO.applicationInfo.sourceDir.startsWith("/system")
                    || appItem.PACKAGE_INFO.applicationInfo.sourceDir.startsWith("/vendor")) viewHolder.appName.setTextColor(Color.RED)
            else viewHolder.appName.setTextColor(Color.YELLOW)
        }

        viewHolder.appCheckBox.setFromProperty(appItem.PACKAGE_INFO.packageName, appItem)
        viewHolder.dataCheckBox.setFromProperty(appItem.PACKAGE_INFO.packageName, appItem)
        viewHolder.permCheckBox.setFromProperty(appItem.PACKAGE_INFO.packageName, appItem)

        view?.setOnClickListener {
            val isAllSelected = (appItem.APP || appItem.EXCLUSIONS.contains(EXCLUDE_APP))
                    && (appItem.DATA || appItem.EXCLUSIONS.contains(EXCLUDE_DATA))
                    && (appItem.PERMISSION || appItem.EXCLUSIONS.contains(EXCLUDE_PERMISSION))

            if (!appItem.EXCLUSIONS.contains(EXCLUDE_APP))
                viewHolder.appCheckBox.isChecked = !isAllSelected

            if (!appItem.EXCLUSIONS.contains(EXCLUDE_DATA))
                viewHolder.dataCheckBox.isChecked = !isAllSelected

            if (!appItem.EXCLUSIONS.contains(EXCLUDE_PERMISSION))
                viewHolder.permCheckBox.isChecked = !isAllSelected

        }

        return view!!
    }

    private fun CheckBox.setFromProperty(packageName: String, appItem: BackupDataPacketKotlin, defaultValue: Boolean = false){

        val property = when (this.id){
            R.id.appCheckbox -> PROPERTY_APP_SELECTION
            R.id.dataCheckbox -> PROPERTY_DATA_SELECTION
            R.id.permissionCheckbox -> PROPERTY_PERMISSION_SELECTION
            else -> ""
        }

        this.setOnCheckedChangeListener {_, isChecked ->
            editor.putBoolean(packageName + "_" + property, isChecked)
            editor.commit()
            when (property) {
                PROPERTY_APP_SELECTION -> appItem.APP = isChecked
                PROPERTY_DATA_SELECTION -> appItem.DATA = isChecked
                PROPERTY_PERMISSION_SELECTION -> appItem.PERMISSION = isChecked
            }
        }

        if (appItem.EXCLUSIONS.contains(EXCLUDE_APP) && property == PROPERTY_APP_SELECTION) this.isEnabled = false
        if (appItem.EXCLUSIONS.contains(EXCLUDE_DATA) && property == PROPERTY_DATA_SELECTION) this.isEnabled = false
        if (appItem.EXCLUSIONS.contains(EXCLUDE_PERMISSION) && property == PROPERTY_PERMISSION_SELECTION) this.isEnabled = false

        if (this.isEnabled) this.isChecked = main.getBoolean("${packageName}_$property", defaultValue)

    }

    override fun getItem(position: Int): Any = tmpList[position]
    override fun getItemId(position: Int): Long = 0
    override fun getCount(): Int = tmpList.size
    override fun getViewTypeCount(): Int = count
    override fun getItemViewType(position: Int): Int = position

    private class ViewHolder {

        lateinit var appIcon: ImageView
        lateinit var appName: TextView
        lateinit var appInfo: ImageView
        lateinit var appCheckBox: CheckBox
        lateinit var dataCheckBox: CheckBox
        lateinit var permCheckBox: CheckBox

    }
}