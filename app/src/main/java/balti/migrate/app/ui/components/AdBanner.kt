package balti.migrate.app.ui.components

import android.view.ViewGroup
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import balti.migrate.app.ads.AdsController
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun rememberBannerAdView(
    adUnitId: String,
    adsController: AdsController = koinInject(),
): AdView? {
    val activity = LocalActivity.current ?: return null
    val isReady by adsController.isReady.collectAsState()

    val adView = remember(isReady, adUnitId) {
        if (isReady && adUnitId.isNotBlank()) AdView(activity) else null
    }

    DisposableEffect(adView) {
        adView?.loadAd(
            BannerAdRequest.Builder(adUnitId, AdSize.BANNER).build(),
            object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    Timber.d("Banner ad loaded")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Timber.d("Banner ad failed to load: $adError")
                }
            },
        )
        onDispose {
            (adView?.parent as? ViewGroup)?.removeView(adView)
            adView?.destroy()
        }
    }

    return adView
}

@Composable
fun AdBanner(
    adView: AdView?,
    modifier: Modifier = Modifier,
) {
    if (adView == null) return

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = {
                (adView.parent as? ViewGroup)?.removeView(adView)
                adView
            },
            onRelease = { (it.parent as? ViewGroup)?.removeView(it) },
        )
    }
}
