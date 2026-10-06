package balti.migrate.app.ads

import android.content.Context
import balti.migrate.BuildConfig
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class AdsController(private val context: Context) {

    val isConfigured: Boolean = BuildConfig.ADMOB_APP_ID.isNotBlank()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private var initializeCalled = false

    fun initialize() {
        if (!isConfigured || initializeCalled) return
        initializeCalled = true
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(
                context,
                InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID).build(),
            ) {
                Timber.d("Ads SDK initialized")
                _isReady.value = true
            }
        }
    }
}
