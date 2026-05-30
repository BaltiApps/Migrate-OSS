package balti.migrate.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import balti.migrate.R
import balti.migrate.app.ui.screens.appSettings.AppSettings
import balti.migrate.app.ui.screens.home.ScreenHome
import balti.migrate.app.ui.screens.purchase.PurchaseScreen
import baltiapps.migrate.domain.common.sources.Preferences

private enum class MainScreenTab { Home, Settings, Purchase }

@Composable
fun MainScreenNavContainer(
    onBackupSelected: () -> Unit,
    onRestoreSelected: () -> Unit,
    updateUiState: (darkMode: Preferences.DarkMode, followSystemColors: Boolean) -> Unit,
) {
    var currentTab by remember { mutableStateOf(MainScreenTab.Home) }

    BackHandler(enabled = currentTab != MainScreenTab.Home) {
        currentTab = MainScreenTab.Home
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == MainScreenTab.Home,
                    onClick = { currentTab = MainScreenTab.Home },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainScreenTab.Home) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = stringResource(R.string.home),
                        )
                    },
                    label = { Text(stringResource(R.string.home)) },
                )
                NavigationBarItem(
                    selected = currentTab == MainScreenTab.Settings,
                    onClick = { currentTab = MainScreenTab.Settings },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainScreenTab.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    },
                    label = { Text(stringResource(R.string.settings)) },
                )
                NavigationBarItem(
                    selected = currentTab == MainScreenTab.Purchase,
                    onClick = { currentTab = MainScreenTab.Purchase },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_gift),
                            contentDescription = stringResource(R.string.purchase),
                        )
                    },
                    label = { Text(stringResource(R.string.support)) },
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when (currentTab) {
                MainScreenTab.Home -> ScreenHome(
                    onBackupSelected = onBackupSelected,
                    onRestoreSelected = onRestoreSelected,
                )
                MainScreenTab.Settings -> AppSettings(
                    updateUiState = updateUiState,
                )
                MainScreenTab.Purchase -> PurchaseScreen()
            }
        }
    }
}
