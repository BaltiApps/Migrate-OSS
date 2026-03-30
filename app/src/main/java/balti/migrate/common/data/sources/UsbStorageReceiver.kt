package balti.migrate.common.data.sources

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import timber.log.Timber

class UsbStorageReceiver(
    private val onAttached: () -> Unit,
    private val onDetached: () -> Unit,
) : BroadcastReceiver() {
    private val TAG = "UsbStorageReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        when (action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                Timber.tag(TAG).d("USB Storage ATTACHED")
                onAttached()
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                Timber.tag(TAG).d("USB Storage DETACHED")
                onDetached()
            }
        }
    }
}