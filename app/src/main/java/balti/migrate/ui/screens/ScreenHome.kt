package balti.migrate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Scaffold(
        modifier = Modifier.fillMaxSize()
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