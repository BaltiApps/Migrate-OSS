package balti.migrate.app.ui.navigation

import androidx.lifecycle.ViewModel
import baltiapps.migrate.domain.backup.repository.BackupDataRepository
import baltiapps.migrate.domain.restore.repository.RestoreDataRepository

class GraphViewModel(
    private val backupRepository: BackupDataRepository,
    private val restoreRepository: RestoreDataRepository
): ViewModel() {

    private val restoreRouteMap = mapOf(
        RouteCallLogRestoreSelection to { restoreRepository.getCallLogBackupFile() != null },
        RouteSmsRestoreSelection to { restoreRepository.getSmsBackupFile() != null },
        RouteContactRestoreSelection to { restoreRepository.getContactBackupFile() != null },
        RouteRestoreSummary to { true },
    )

    private var restorePointerIndex = -1

    fun findNextRestoreRoute(): Any {
        while (restorePointerIndex < restoreRouteMap.size) {
            restorePointerIndex++
            val route = restoreRouteMap.keys.elementAtOrNull(restorePointerIndex)
            if (route != null && restoreRouteMap[route]?.invoke() == true) {
                return route
            }
        }
        return restoreRouteMap.keys.last()
    }

    fun onBackFromRestoreRoute() {
        restorePointerIndex--
    }

    fun resetBackupRepository() {
        backupRepository.resetRepository()
    }
}