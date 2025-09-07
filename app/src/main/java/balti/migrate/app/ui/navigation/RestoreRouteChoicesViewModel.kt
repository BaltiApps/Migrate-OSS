package balti.migrate.app.ui.navigation

import androidx.lifecycle.ViewModel
import balti.migrate.common.utils.SuperuserUtils
import baltiapps.migrate.domain.common.sources.Preferences
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository
import kotlinx.coroutines.runBlocking

class RestoreRouteChoicesViewModel(
    private val preferences: Preferences,
    private val superuserUtils: SuperuserUtils,
    private val dataRepository: RestoreDataRepository
) : ViewModel() {

    private val routeMap = mapOf(
        RouteCallLogRestoreSelection to { dataRepository.getCallLogBackupFile() != null },
        RouteSmsRestoreSelection to { dataRepository.getSmsBackupFile() != null },
        RouteContactRestoreSelection to { dataRepository.getContactBackupFile() != null },
        RouteAppRestoreSelection to {
            dataRepository.getAppInfoFiles().isNotEmpty() &&
                    preferences.wasSuperuserPermissionPreviouslyGranted() &&
                    checkSuperuserPermission()
        },
        RouteRestoreSummary to { true },
    )

    private var pointerIndex = -1

    private fun checkSuperuserPermission(): Boolean {
        return runBlocking {
            superuserUtils.checkSuperuserPermission().isSuccess
        }
    }

    fun findNextRoute(): Any {
        while (pointerIndex < routeMap.size) {
            pointerIndex++
            val route = routeMap.keys.elementAtOrNull(pointerIndex)
            if (route != null && routeMap[route]?.invoke() == true) {
                return route
            }
        }
        return routeMap.keys.last()
    }

    fun onBackFromRoute() {
        pointerIndex--
    }

    fun resetRestorePointer() {
        pointerIndex = -1
    }
}