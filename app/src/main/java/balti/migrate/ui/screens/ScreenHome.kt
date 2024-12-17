package balti.migrate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import balti.migrate.R
import balti.migrate.RouteBackup
import balti.migrate.RouteRestore

@Composable
fun ScreenHome(
    navController: NavController,
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
            ButtonBackup(navController)
            ButtonRestore(navController)
        }
    }
}

@Preview
@Composable
fun ScreenHomePreview() {
    val navController = rememberNavController()
    ScreenHome(navController)
}

@Composable
fun ButtonBackup(
    navController: NavController,
) {
    Button (
        modifier = Modifier.fillMaxWidth(),
        onClick = { navController.navigate(RouteBackup) }
    ) {
        Text(stringResource(R.string.backup))
    }
}

@Composable
fun ButtonRestore(
    navController: NavController,
) {
    OutlinedButton (
        modifier = Modifier.fillMaxWidth(),
        onClick = { navController.navigate(RouteRestore) }
    ) {
        Text(stringResource(R.string.restore))
    }
}