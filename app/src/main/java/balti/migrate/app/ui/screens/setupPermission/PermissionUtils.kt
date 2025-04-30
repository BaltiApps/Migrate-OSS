package balti.migrate.app.ui.screens.setupPermission

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import baltiapps.migrate.domain.PermissionConstants

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

    val allFilesAccessPermissionConstant = PermissionConstants.MANAGE_EXTERNAL_STORAGE

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
}