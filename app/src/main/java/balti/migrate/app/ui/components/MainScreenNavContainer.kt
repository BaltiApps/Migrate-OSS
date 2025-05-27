package balti.migrate.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import balti.migrate.app.ui.navigation.AppNavBarItem

@Composable
fun MainScreenNavContainer(
    appNavBarItems: List<AppNavBarItem>,
    currentNavRoute: AppNavBarItem,
    onBottomNavItemSelected: (AppNavBarItem) -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                appNavBarItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentNavRoute == item,
                        onClick = { onBottomNavItemSelected(item) },
                        icon = {
                            Icon(
                                imageVector = if (currentNavRoute == item) {
                                    item.filledIconVector
                                } else item.unfilledIconVector,
                                contentDescription = item.label,
                            )
                        },
                        label = {
                            Text(text = item.label)
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            content()
        }
    }
}