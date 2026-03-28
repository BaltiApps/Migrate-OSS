package balti.migrate.app.ui.screens.setupPermission

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.components.IconSource
import balti.migrate.common.utils.PermissionUtils
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SetupPermissionScreen(
    goToNextScreen: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupPermissionScreenViewModel = koinViewModel()
) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Content(
        state = state,
        requestCallLogPermissions = PermissionUtils.requestPermissions(PermissionUtils.callLogPermissions) {
            viewModel.performAction(SetupPermissionScreenAction.OnCallLogPermissionsResult(it))
        },
        requestSmsPermission = PermissionUtils.requestPermission(PermissionUtils.smsReadPermission) {
            viewModel.performAction(SetupPermissionScreenAction.OnSmsPermissionResult(it))
        },
        requestContactsPermission = PermissionUtils.requestPermission(PermissionUtils.contactsReadPermission) {
            viewModel.performAction(SetupPermissionScreenAction.OnContactsPermissionResult(it))
        },
        requestNotificationPermission = PermissionUtils.notificationsPermission?.run {
            PermissionUtils.requestPermission(this) {
                viewModel.performAction(SetupPermissionScreenAction.OnNotificationPermissionResult(it))
            }
        } ?: {
            // Permission string will be null in versions below Android 13.
            // For such devices, notification permission is already granted.
            viewModel.performAction(SetupPermissionScreenAction.OnNotificationPermissionResult(true))
        },
        requestSuperuserPermission = {
            viewModel.performAction(SetupPermissionScreenAction.CheckSuperuserPermission {
                showSuError(context, it)
            })
        },
        requestAllPermissions = PermissionUtils.requestPermissions(PermissionUtils.allRuntimePermissions) {
            viewModel.performAction(SetupPermissionScreenAction.OnAllPermissionsResult(it))
            viewModel.performAction(SetupPermissionScreenAction.CheckSuperuserPermission())
        },
        onAllPermissionsGranted = {
            viewModel.performAction(SetupPermissionScreenAction.OnAllPermissionsGranted)
        },
        skip = { dontShowAgain ->
            viewModel.performAction(SetupPermissionScreenAction.OnSkipClicked(dontShowAgain))
            goToNextScreen()
        },
        goToNextScreen = goToNextScreen,
        modifier = modifier,
    )
}

private fun showSuError(context: Context, error: String) {
    Toast.makeText(context, error.replace('\n', ' '), Toast.LENGTH_SHORT).show()
}

@Composable
private fun Content(
    state: SetupPermissionScreenState,
    requestCallLogPermissions: () -> Unit,
    requestSmsPermission: () -> Unit,
    requestContactsPermission: () -> Unit,
    requestNotificationPermission: () -> Unit,
    requestSuperuserPermission: () -> Unit,
    requestAllPermissions: () -> Unit,
    onAllPermissionsGranted: () -> Unit,
    skip: (dontShowAgain: Boolean) -> Unit,
    goToNextScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(state.isAllPermissionsGranted, state.isAskingSuperuserPermission) {
        if (state.isAllPermissionsGranted && !state.isAskingSuperuserPermission) {
            onAllPermissionsGranted()
            goToNextScreen()
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val scrollSlate = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .verticalScroll(scrollSlate),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TopLayout()
                PermissionItem(
                    title = stringResource(R.string.call_log_permission),
                    description = stringResource(R.string.call_log_permission_description),
                    icon = IconSource.Vector(Icons.Outlined.Call),
                    isGranted = state.isCallLogPermissionsGranted,
                    requestPermission = requestCallLogPermissions
                )
                PermissionItem(
                    title = stringResource(R.string.sms_permission),
                    description = stringResource(R.string.sms_permission_description),
                    icon = IconSource.Vector(Icons.Outlined.Sms),
                    isGranted = state.isSmsReadPermissionGranted,
                    requestPermission = requestSmsPermission
                )
                PermissionItem(
                    title = stringResource(R.string.contacts_permission),
                    description = stringResource(R.string.contacts_permission_description),
                    icon = IconSource.Vector(Icons.Outlined.Contacts),
                    isGranted = state.isContactsReadPermissionGranted,
                    requestPermission = requestContactsPermission
                )
                PermissionItem(
                    title = stringResource(R.string.notification_permission),
                    description = stringResource(R.string.notification_permission_description),
                    icon = IconSource.Vector(Icons.Outlined.Notifications),
                    isGranted = state.isNotificationPermissionGranted,
                    requestPermission = requestNotificationPermission,
                )
                PermissionItem(
                    title = stringResource(R.string.superuser_permission),
                    description = stringResource(R.string.superuser_permission_description),
                    icon = IconSource.Drawable(painterResource(R.drawable.root)),
                    isGranted = state.isSuperuserPermissionGranted,
                    requestPermission = requestSuperuserPermission,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var dontShowAgain by rememberSaveable { mutableStateOf(false) }
                Button(
                    onClick = requestAllPermissions,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.allow_all))
                }
                TextButton(
                    onClick = { skip(dontShowAgain) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.skip_for_now))
                }
                Row(
                    modifier = Modifier.clickable {
                        dontShowAgain = !dontShowAgain
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dont_show_again),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    icon: IconSource,
    isGranted: Boolean,
    requestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        enabled = !isGranted,
        colors = CardDefaults.elevatedCardColors(),
        onClick = requestPermission,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        ListItem(
            modifier = Modifier.padding(8.dp),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            headlineContent = {
                Text(title)
            },
            supportingContent = {
                Text(description)
            },
            trailingContent = {
                Switch(
                    checked = isGranted,
                    enabled = !isGranted,
                    onCheckedChange = null,
                )
            },
            leadingContent = {
                when (icon) {
                    is IconSource.Vector -> Icon(
                        imageVector = icon.imageVector,
                        contentDescription = null,
                    )
                    is IconSource.Drawable -> Icon(
                        painter = icon.painter,
                        contentDescription = null,
                    )
                }
            }
        )
    }
}

@Composable
private fun TopLayout(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            imageVector = Icons.Outlined.Checklist,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.please_grant_the_following_permissions),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Preview
@Composable
private fun ContentPreview() {
    Content(
        state = SetupPermissionScreenState(),
        requestCallLogPermissions = {},
        requestSmsPermission = {},
        requestContactsPermission = {},
        requestNotificationPermission = {},
        requestAllPermissions = {},
        requestSuperuserPermission = {},
        onAllPermissionsGranted = {},
        skip = {},
        goToNextScreen = {},
    )
}