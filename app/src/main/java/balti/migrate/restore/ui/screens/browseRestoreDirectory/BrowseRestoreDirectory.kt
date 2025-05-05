package balti.migrate.restore.ui.screens.browseRestoreDirectory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import balti.migrate.common.ui.components.LoadingDialog
import baltiapps.migrate.domain.common.model.GenericFile
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BrowseRestoreDirectory(
    navigateUp: () -> Unit,
    onBackupSelected: () -> Unit,
    viewModel: BrowseRestoreDirectoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Content(
        state = state,
        onExportDirectorySelected = {
            viewModel.onAction(
                BrowseRestoreDirectoryActions.OnExportDirectorySelected(it) {
                    onBackupSelected()
                }
            )
        },
        onExportDirectoryOpen = {
            viewModel.onAction(BrowseRestoreDirectoryActions.OnExportDirectoryOpen(it))
        },
        onReloadExportDirectory = {
            viewModel.onAction(BrowseRestoreDirectoryActions.OnReloadExportDirectory)
        },
        onExportDirectoryUp = {
            viewModel.onAction(BrowseRestoreDirectoryActions.OnExportDirectoryUp)
        },
        navigateUp = navigateUp,
    )
    
    if (state.isImporting) {
        LoadingDialog(
            text = stringResource(R.string.importing_directory),
            progress = null,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Content(
    state: BrowseRestoreDirectoryState,
    onExportDirectorySelected: (GenericFile) -> Unit,
    onReloadExportDirectory: () -> Unit,
    onExportDirectoryOpen: (GenericFile) -> Unit,
    onExportDirectoryUp: () -> Unit,
    navigateUp: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.select_directory))
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigateUp
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = state.isLoading,
                onRefresh = onReloadExportDirectory
            ) {
                LazyColumn {
                    if (state.isBackAllowed) {
                        item {
                            DirectoryItem(
                                directory = object : GenericFile {
                                    override val path: String = ""
                                    override val name: String = ".."
                                },
                                onDirectoryClick = onExportDirectoryUp
                            )
                        }
                    }
                    items(state.exportDirectoriesToShow) {
                        DirectoryItem(
                            directory = it,
                            onDirectoryClick = {
                                if (it.isValidBackupDirectory) {
                                    onExportDirectorySelected(it)
                                } else {
                                    onExportDirectoryOpen(it)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DirectoryItem(
    directory: GenericFile,
    onDirectoryClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable {
                onDirectoryClick()
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (directory.isValidBackupDirectory) {
            MaterialTheme.colorScheme.primary
        } else MaterialTheme.colorScheme.onSurface
        Icon(
            imageVector = if (directory.isValidBackupDirectory) {
                Icons.Default.Save
            } else Icons.Default.Folder,
            contentDescription = null,
            tint = color,
        )
        Text(
            text = directory.name,
            modifier = Modifier.weight(1F),
            color = color,
        )
    }
}