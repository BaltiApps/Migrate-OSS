package balti.migrate.backupActivity.containers

import android.content.SharedPreferences
import android.content.pm.PackageInfo
import balti.migrate.utilities.CommonToolKotlin.Companion.PROPERTY_APP_SELECTION
import balti.migrate.utilities.CommonToolKotlin.Companion.PROPERTY_DATA_SELECTION
import balti.migrate.utilities.CommonToolKotlin.Companion.PROPERTY_PERMISSION_SELECTION
import balti.migrate.utilities.ExclusionsKotlin

data class BackupDataPacketKotlin(val PACKAGE_INFO: PackageInfo, val prefs: SharedPreferences, var installerName: String = "") {

    var APP: Boolean = prefs.getBoolean("${PACKAGE_INFO.packageName}_$PROPERTY_APP_SELECTION", false)
    var DATA: Boolean = prefs.getBoolean("${PACKAGE_INFO.packageName}_$PROPERTY_DATA_SELECTION", false)
    var PERMISSION: Boolean = prefs.getBoolean("${PACKAGE_INFO.packageName}_$PROPERTY_PERMISSION_SELECTION", false)
    var EXCLUSIONS: ArrayList<Int> = ExclusionsKotlin().returnExclusionState(PACKAGE_INFO.packageName)

}