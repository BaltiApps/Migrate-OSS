package balti.migrate.backup.ui.screens.listScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ListScreenRootViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        ListScreenRootState(
            isLoading = false,
            isStaging = false,
            currentRoute = RouteBackupList.RouteContactBackup,
        )
    )
    val state = _state.asStateFlow()

    private val routeList = listOf(
        RouteBackupList.RouteContactBackup,
        RouteBackupList.RouteCallLogBackup,
        RouteBackupList.RouteSmsBackup,
    )

    private var stagingBlock: (() -> Unit)? = null
    private var toggleAllBlock: ((Boolean) -> Unit)? = null
    private var goToNextNavRoot: (() -> Unit)? = null

    fun setGoToNextNavRoot(block: () -> Unit) {
        this.goToNextNavRoot = block
    }

    fun setStagingBlock(block: () -> Unit) {
        this.stagingBlock = block
    }

    fun setToggleAllBlock(block: (Boolean) -> Unit) {
        this.toggleAllBlock = block
    }

    private fun requestStaging() {
        stagingBlock?.invoke()
    }

    fun performAction(action: ListScreenRootAction) {
        viewModelScope.launch {
            when (action) {
                is ListScreenRootAction.OnNextClicked -> {
                    onPreStaging()
                    requestStaging()
                }

                is ListScreenRootAction.SetLoading -> {
                    _state.update {
                        it.copy(isLoading = action.isLoading)
                    }
                }

                is ListScreenRootAction.SelectAll -> {
                    toggleAllBlock?.invoke(true)
                }

                is ListScreenRootAction.DeselectAll -> {
                    toggleAllBlock?.invoke(false)
                }
            }
        }
    }

    private fun onPreStaging() {
        _state.update {
            it.copy(isStaging = true)
        }
    }

    fun onPostStaging(navHostController: NavHostController) {
        _state.update {
            val currentRouteIndex = routeList.indexOf(_state.value.currentRoute)
            if (currentRouteIndex < routeList.size - 1) {
                it.copy(
                    isStaging = false,
                    currentRoute = routeList[currentRouteIndex + 1],
                ).apply {
                    navHostController.navigate(currentRoute)
                }
            } else {
                goToNextNavRoot?.invoke()
                it.copy(
                    isStaging = false,
                )
            }
        }
    }
}