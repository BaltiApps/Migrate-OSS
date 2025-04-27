package balti.migrate.common.ui.components

import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Composable function that keeps the screen on while the composable is in the foreground.
 *
 * This uses the FLAG_KEEP_SCREEN_ON window flag for a more efficient and system-friendly approach.
 * It also handles adding and removing the flag according to the lifecycle events of the composable.
 *
 * @param shouldKeepScreenOn Flag to dynamically control whether to keep the screen on.
 *                     Defaults to true.
 * @param lifecycleOwner The LifecycleOwner for this composable. Defaults to LocalLifecycleOwner.current.
 */
@Composable
fun KeepScreenOn(
    shouldKeepScreenOn: Boolean = true,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val activity = LocalActivity.current
    val window: Window? = activity?.window
    DisposableEffect(lifecycleOwner, shouldKeepScreenOn) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (shouldKeepScreenOn) {
                        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                Lifecycle.Event.ON_PAUSE -> {
                    if (shouldKeepScreenOn) {
                        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}