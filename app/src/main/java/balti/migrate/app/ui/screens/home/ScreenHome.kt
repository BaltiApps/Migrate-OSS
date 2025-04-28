package balti.migrate.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import balti.migrate.R

@Composable
fun ScreenHome(
    onBackupSelected: () -> Unit,
    onRestoreSelected: () -> Unit
) {

    var shouldShowAboutDialog by remember { mutableStateOf(false) }

    if (shouldShowAboutDialog) {
        AboutDialog(
            onDismissRequest = {
                shouldShowAboutDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(
                showAboutDialog = {
                    shouldShowAboutDialog = true
                }
            )
        }
    ) { values ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(values)
                .padding(dimensionResource(R.dimen.screen_padding)),
            verticalArrangement = Arrangement
                .spacedBy(
                    dimensionResource(R.dimen.column_item_padding),
                    Alignment.CenterVertically
                ),
        ) {
            ButtonBackup(
                onClick = onBackupSelected,
            )
            ButtonRestore(
                onClick = onRestoreSelected,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    showAboutDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {},
        actions = {
            IconButton(
                onClick = showAboutDialog
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.about),
                )
            }
        },
        modifier = modifier,
    )
}

@Preview
@Composable
fun ScreenHomePreview() {
    ScreenHome({}, {})
}

@Composable
fun ButtonBackup(
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(stringResource(R.string.backup))
    }
}

@Composable
fun ButtonRestore(
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(stringResource(R.string.restore))
    }
}