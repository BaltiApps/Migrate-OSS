package balti.migrate.app.ads

import android.app.Activity
import android.content.Context
import balti.migrate.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class ConsentManager(context: Context) {

    private val consentInformation = UserMessagingPlatform.getConsentInformation(context)

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds

    fun gatherConsent(activity: Activity) {
        val consentDebugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId(BuildConfig.CONSENT_DEBUG_DEVICE_ID)
            .build()

        val consentRequestParameters = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG) {
                setConsentDebugSettings(consentDebugSettings)
            }
        }.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            consentRequestParameters,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    formError?.let { Timber.w("Consent form error: ${it.message}") }
                    onConsentUpdated()
                }
            },
            { requestError ->
                Timber.w("Consent info update error: ${requestError.message}")
                onConsentUpdated()
            },
        )
        onConsentUpdated()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let { Timber.w("Privacy options form error: ${it.message}") }
            onConsentUpdated()
        }
    }

    private fun onConsentUpdated() {
        _isPrivacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus == PrivacyOptionsRequirementStatus.REQUIRED
        _canRequestAds.value = consentInformation.canRequestAds()
    }
}
