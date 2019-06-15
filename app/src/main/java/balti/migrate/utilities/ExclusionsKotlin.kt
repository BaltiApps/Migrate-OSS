package balti.migrate.utilities

import java.util.*
import kotlin.collections.ArrayList

class ExclusionsKotlin() {

    class ExclusionItem(val packageName: String, vararg excl: Int) {
        val exclusions by lazy { ArrayList<Int>(0)}
        init {
            excl.forEach { e -> exclusions.add(e)  }
        }
    }

    val predefinedPackageNames by lazy { Vector<ExclusionItem>(0) }
    val manualPackageNames by lazy { Vector<ExclusionItem>(0) }

    val EXCLUSION_FILE_NAME = "Exclusions"

    companion object {
        val EXCLUDE_DATA = 234
        val EXCLUDE_APP = 35
        val EXCLUDE_PERMISSION = 211
        val NOT_EXCLUDED = 0
    }

    init {
        predefinedPackageNames.add(ExclusionItem("com.topjohnwu.magisk", EXCLUDE_DATA))
        predefinedPackageNames.add(ExclusionItem("eu.chainfire.supersu", EXCLUDE_DATA))
        predefinedPackageNames.add(ExclusionItem("com.noshufou.android.su", EXCLUDE_DATA))
        predefinedPackageNames.add(ExclusionItem("me.phh.superuser", EXCLUDE_DATA))
        predefinedPackageNames.add(ExclusionItem("com.bitcubate.android.su.installer", EXCLUDE_DATA))
        predefinedPackageNames.add(ExclusionItem("com.gorserapp.superuser", EXCLUDE_DATA))
        predefinedPackageNames.add(ExclusionItem("com.farma.superuser", EXCLUDE_DATA))
        predefinedPackageNames.add(ExclusionItem("com.koushikdutta.superuser", EXCLUDE_DATA))
        predefinedPackageNames.add(ExclusionItem("de.robv.android.xposed.installer", EXCLUDE_DATA))
        predefinedPackageNames.add(ExclusionItem("balti.migratehelper", EXCLUDE_APP, EXCLUDE_DATA))
        predefinedPackageNames.add(ExclusionItem("android", EXCLUDE_APP, EXCLUDE_DATA, EXCLUDE_PERMISSION))
    }

    fun returnExclusionState(packageName: String): ArrayList<Int> {
        (predefinedPackageNames + manualPackageNames).forEach {
            if (it.packageName == packageName) return it.exclusions
        }
        return arrayListOf(NOT_EXCLUDED)
    }
}