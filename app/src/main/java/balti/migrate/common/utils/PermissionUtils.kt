package balti.migrate.common.utils

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

object PermissionUtils {

    val callLogPermissions = listOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
    )
    val smsReadPermission = Manifest.permission.READ_SMS
    val contactsReadPermission = Manifest.permission.READ_CONTACTS


    val notificationsPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else null

    val allRuntimePermissions = (
            callLogPermissions +
                    smsReadPermission +
                    contactsReadPermission +
                    notificationsPermission
            ).filterNotNull()

    @Composable
    fun requestPermission(
        permission: String,
        onResult: (isGranted: Boolean) -> Unit,
    ): () -> Unit {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            onResult(isGranted)
        }
        return {
            permissionLauncher.launch(permission)
        }
    }

    @Composable
    fun requestPermissions(
        permission: List<String>,
        onResult: (isGranted: Boolean) -> Unit,
    ): () -> Unit {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissionMap ->
            onResult(permissionMap.values.all { it })
        }
        return {
            permissionLauncher.launch(permission.toTypedArray())
        }
    }

    @Composable
    fun requestSpecialPermission(
        intent: Intent,
        onResult: () -> Unit,
    ): () -> Unit {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            onResult()
        }
        return {
            permissionLauncher.launch(intent)
        }
    }

    @Composable
    fun requestSafLocation(
        onResult: (uriString: String?) -> Unit,
    ): () -> Unit {
        val context = LocalContext.current
        val documentPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri == null) {
                onResult(null)
                return@rememberLauncherForActivityResult
            }
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            onResult(uri.toString())
        }
        return {
            documentPicker.launch(null)
        }
    }
}