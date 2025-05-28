package balti.migrate.restore.ui.screens.restoreSummary

import android.app.role.RoleManager
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.BuildConfig
import balti.migrate.R
import balti.migrate.common.ui.components.ButtonStatus
import balti.migrate.common.ui.components.LoadingDialog
import balti.migrate.common.ui.components.NextFab
import balti.migrate.common.utils.PermissionUtils
import balti.migrate.restore.ui.screens.restoreSummary.components.DelegatedRestore
import balti.migrate.restore.ui.screens.restoreSummary.components.SimpleYesNoDialog
import balti.migrate.restore.ui.screens.restoreSummary.components.SpecialPermissions
import balti.migrate.restore.ui.screens.restoreSummary.components.StandardRestore
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RestoreSummary(
    navigateUp: () -> Unit,
    startRestoreServiceAndGoToNextScreen: () -> Unit,
    viewModel: RestoreSummaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val roleManager = remember {
        context.getSystemService(RoleManager::class.java)
    }
    
    Content(
        state = { state },
        onStartRestore = {
            viewModel.onAction(RestoreSummaryAction.StartRestore(startRestoreServiceAndGoToNextScreen))
        },
        contactExportProgressDialog = {
            LoadingDialog(
                text = stringResource(R.string.exporting_percentage, (state.contactsExportProgress.percentage * 100).toInt()),
                progress = state.contactsExportProgress,
            )
        },
        showDialogContactImport = {
            SimpleYesNoDialog(
                titleText = stringResource(R.string.contacts_delegate_title),
                dialogText = stringResource(R.string.contacts_delegate_description),
                onProceed = {
                    viewModel.onAction(RestoreSummaryAction.OnUserProceedContactImport)
                },
                onSkip = {
                    viewModel.onAction(RestoreSummaryAction.SkipContacts)
                },
                icon = Icons.Outlined.Contacts,
            )
        },
        openContactsImport = PermissionUtils.requestSpecialPermission(
            intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = FileProvider.getUriForFile(
                    context.applicationContext,
                    BuildConfig.contentProviderAuthority,
                    viewModel.vcfFile.file,
                )
                setDataAndType(uri, "text/x-vcard")
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/x-vcard"))
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            },
            onResult = {
                viewModel.onAction(RestoreSummaryAction.OnContactImported)
            }
        ),
        showDialogDefaultSmsSet = {
            SimpleYesNoDialog(
                titleText = stringResource(R.string.default_sms_app_title),
                dialogText = stringResource(R.string.default_sms_app_description),
                onProceed = {
                    viewModel.onAction(RestoreSummaryAction.OnUserProceedSetDefaultSmsApp)
                },
                onSkip = {
                    viewModel.onAction(RestoreSummaryAction.SkipSms)
                },
                icon = Icons.Outlined.Sms,
            )
        },
        requestDefaultSmsApp = PermissionUtils.requestSpecialPermission(
            intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS),
            onResult = {
                viewModel.onAction(RestoreSummaryAction.OnDefaultSmsAppSet)
            }
        ),
        navigateUp = navigateUp,
    )
}

@Composable
private fun Content(
    state: () -> RestoreSummaryState,
    onStartRestore: () -> Unit,
    contactExportProgressDialog: @Composable () -> Unit,
    showDialogContactImport: @Composable () -> Unit,
    openContactsImport: () -> Unit,
    showDialogDefaultSmsSet: @Composable () -> Unit,
    requestDefaultSmsApp: () -> Unit,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state().contactSummaryState) {
        RestoreSummaryItemState.PROCESSING -> contactExportProgressDialog()
        RestoreSummaryItemState.REQUEST_USER_INPUT -> showDialogContactImport()
        RestoreSummaryItemState.ON_USER_INPUT_POSITIVE -> openContactsImport()
        else -> {}
    }
    when (state().smsSummaryState) {
        RestoreSummaryItemState.REQUEST_USER_INPUT -> showDialogDefaultSmsSet()
        RestoreSummaryItemState.ON_USER_INPUT_POSITIVE -> requestDefaultSmsApp()
        else -> {}
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopBar(navigateUp = navigateUp)
        },
        bottomBar = {
            BottomBar(
                onStartRestore = onStartRestore,
            )
        },
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            DelegatedRestore(
                state = state(),
                modifier = Modifier.fillMaxWidth(),
            )
            SpecialPermissions(
                state = state(),
                modifier = Modifier.fillMaxWidth(),
            )
            StandardRestore(
                state = state(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BottomBar(
    onStartRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier,
        actions = {},
        floatingActionButton = {
            NextFab(
                buttonStatus = ButtonStatus.Unspecified(
                    label = stringResource(R.string.start),
                    onPressed = onStartRestore
                )
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(text = stringResource(R.string.restore_summary))
        },
        navigationIcon = {
            IconButton(
                onClick = navigateUp,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.go_back),
                )
            }
        }
    )
}

@Preview
@Composable
private fun ContentPreview() {
//    Content(
//        state = {
//            RestoreSummaryState2(
//                countContacts = 5,
//                countCallLogs = 10,
//                countSms = 15,
//                contactSummaryState = RestoreSummaryItemState.DONE,
//                smsSummaryState = RestoreSummaryItemState.DONE,
//            )
//        },
//        onStartRestore = {},
//        navigateUp = {},
//    )
}