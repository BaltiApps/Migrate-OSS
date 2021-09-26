package balti.migrate.extraBackupsActivity.engines.installers

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import balti.migrate.R
import balti.migrate.extraBackupsActivity.engines.installers.containers.PackageVsInstaller
import balti.migrate.utilities.CommonToolsKotlin
import balti.migrate.utilities.IconTools
import balti.module.baltitoolbox.functions.Misc
import kotlinx.android.synthetic.main.installer_set_item.view.*

class InstallerListAdapter(val context: Context,
                           private val appVsInstallers: ArrayList<PackageVsInstaller>): BaseAdapter() {

    private val pm by lazy { context.packageManager }
    private val iconTools by lazy { IconTools() }

    private val availableInstallerPackageNames = ArrayList<String>(0)
    private val availableInstallersAppNames = ArrayList<String>(0)

    init {

        CommonToolsKotlin.QUALIFIED_PACKAGE_INSTALLERS.forEach {
            availableInstallerPackageNames.add(it.key)
            availableInstallersAppNames.add(it.value)
        }

        appVsInstallers.sortWith(Comparator { o1, o2 ->
            String.CASE_INSENSITIVE_ORDER.compare(
                Misc.getAppName(o1.packageName),
                Misc.getAppName(o2.packageName)
            )
        })
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

        val viewHolder: ViewHolder
        var view = convertView
        val appItem = appVsInstallers[position]

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
        val actualSelectedInstaller = ArrayList<String>(0)

        for (i in availableInstallersAppNames.indices){
            installerDisplayList.add(availableInstallersAppNames[i])
            actualSelectedInstaller.add(availableInstallerPackageNames[i])
        }
        if (appItem.installerName !in availableInstallerPackageNames && appItem.installerName.isNotBlank()) {
            installerDisplayList.add(Misc.getAppName(appItem.installerName))
            actualSelectedInstaller.add(appItem.installerName)
        }

        installerDisplayList.add(context.getString(R.string.not_set))
        actualSelectedInstaller.add("")

        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, installerDisplayList)

        viewHolder.installerSpinner.apply {
            this.adapter = adapter

            setSelection(actualSelectedInstaller.indexOf(appItem.installerName))

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    try {
                        appItem.installerName = actualSelectedInstaller[position]
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

        iconTools.loadIconFromApplication(viewHolder.appIcon, appItem.packageName, pm)

        viewHolder.appName.text = Misc.getAppName(appItem.packageName)
        viewHolder.packageName.text = appItem.packageName

        return view!!

    }

    override fun getItem(position: Int): Any = appVsInstallers[position]
    override fun getItemId(position: Int): Long = 0
    override fun getCount(): Int = appVsInstallers.size
    override fun getViewTypeCount(): Int = count
    override fun getItemViewType(position: Int): Int = position

    private class ViewHolder {

        lateinit var appIcon: ImageView
        lateinit var appName: TextView
        lateinit var packageName: TextView
        lateinit var installerSpinner: Spinner
    }
}