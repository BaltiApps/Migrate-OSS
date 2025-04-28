package balti.migrate.app.ui.navigation

import androidx.lifecycle.ViewModel
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository

class RestoreRouteChoicesViewModel(
    private val dataRepository: RestoreDataRepository
) : ViewModel() {

    private val routeMap = mapOf(
        RouteCallLogRestoreSelection to { dataRepository.getCallLogBackupFile() != null },
        RouteSmsRestoreSelection to { dataRepository.getSmsBackupFile() != null },
        RouteContactRestoreSelection to { dataRepository.getContactBackupFile() != null },
        RouteRestoreSummary to { true },
    )

    private var pointerIndex = -1

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
}