package balti.migrate.restore.ui

import androidx.lifecycle.ViewModel
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository

class ScreenRestoreViewModel(
    private val dataRepository: RestoreDataRepository,
) : ViewModel() {

    private val routeMap = mapOf(
        RouteContactRestoreSelection to { dataRepository.getContactBackupFile() != null },
        RouteCallLogRestoreSelection to { dataRepository.getCallLogBackupFile() != null },
        RouteSmsRestoreSelection to { dataRepository.getSmsBackupFile() != null },
        RouteRestoreSummary to { true },
    )

    private var pointerIndex = -1

    fun findNextRoute(): Any {
        while (pointerIndex < routeMap.size) {
            pointerIndex++
            val route = routeMap.keys.elementAt(pointerIndex)
            if (routeMap[route]?.invoke() == true) {
                return route
            }
        }
        return routeMap.keys.last()
    }
}