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
        RouteAppRestoreSelection to { dataRepository.getAppInfoFiles().isNotEmpty() },
        RouteRestoreSummary to { true },
    )

    fun findNextRoute(currentRoute: Any?): Any {
        val routePositionInMap = routeMap.keys.indexOf(currentRoute)
        var nextPosition = routePositionInMap + 1
        while (nextPosition < routeMap.size) {
            val route = routeMap.keys.elementAtOrNull(nextPosition)
            if (route != null && routeMap[route]?.invoke() == true) {
                return route
            } else {
                nextPosition++
            }
        }
        return routeMap.keys.last()
    }
}