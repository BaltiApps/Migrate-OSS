package balti.migrate.restore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScreenRestoreViewModel: ViewModel() {

    private val _state = MutableStateFlow(ScreenRestoreState(0))
    val state = _state.asStateFlow()

    private fun increment() {
        viewModelScope.launch {
            repeat(5) { count ->
                delay(1000)
                _state.update {
                    it.copy(count+1)
                }
            }
        }
    }

    init {
        increment()
    }

}