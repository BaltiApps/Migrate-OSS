package balti.migrate.app.ui.screens.purchase

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PurchaseScreenViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        PurchaseScreenState(
            items = listOf(
                PurchaseItem("Thanks", "$2.00"),
                PurchaseItem("Awesome", "$4.00"),
                PurchaseItem("Legend", "$6.00"),
            )
        )
    )
    val state = _state.asStateFlow()

    fun performAction(action: PurchaseScreenAction) {
        when (action) {
            is PurchaseScreenAction.OnItemSelected -> _state.update { it.copy(isLoading = true) }
        }
    }
}
