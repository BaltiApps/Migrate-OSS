package balti.migrate.app

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import baltiapps.migrate.domain.common.sources.Preferences
import baltiapps.migrate.domain.common.sources.Preferences.DarkMode
import baltiapps.migrate.domain.purchases.usecase.GetSavedPurchaseUseCase
import baltiapps.migrate.domain.purchases.usecase.ObservePurchaseStateUseCase
import baltiapps.migrate.domain.purchases.usecase.RefreshPurchaseStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivityViewModel(
    private val preferences: Preferences,
    private val refreshPurchaseStateUseCase: RefreshPurchaseStateUseCase,
    private val getSavedPurchaseUseCase: GetSavedPurchaseUseCase,
    private val observePurchaseStateUseCase: ObservePurchaseStateUseCase,
): ViewModel() {

    val darkMode = mutableStateOf(preferences.getDarkMode())
    val shouldFollowSystemColors = mutableStateOf(preferences.shouldFollowSystemColors())

    private val _isAdFree = MutableStateFlow(hasSavedPurchase())
    val isAdFree: StateFlow<Boolean> = _isAdFree.asStateFlow()

    private val _isPurchaseStateResolved = MutableStateFlow(false)
    val isPurchaseStateResolved: StateFlow<Boolean> = _isPurchaseStateResolved.asStateFlow()

    init {
        viewModelScope.launch {
            Timber.d("Play purchase refresh started, isAdFree=${_isAdFree.value}")
            refreshPurchaseStateUseCase.invoke()
            _isAdFree.value = hasSavedPurchase()
            _isPurchaseStateResolved.value = true
            Timber.d("Play purchase refresh finished, isAdFree=${_isAdFree.value}")
        }
        viewModelScope.launch {
            observePurchaseStateUseCase.invoke(
                onProductsChanged = {},
                onPurchasedProductChanged = {
                    _isAdFree.value = it != null || hasSavedPurchase()
                    Timber.d("Purchased product changed: id=$it, isAdFree=${_isAdFree.value}")
                },
            )
        }
    }

    fun updateUiState(darkMode: DarkMode, followSystemColors: Boolean) {
        this.darkMode.value = darkMode
        shouldFollowSystemColors.value = followSystemColors
    }

    fun shouldShowPermissionScreen(): Boolean {
        return preferences.shouldShowPermissionScreen()
    }

    private fun hasSavedPurchase(): Boolean = getSavedPurchaseUseCase.invoke() != null
}
