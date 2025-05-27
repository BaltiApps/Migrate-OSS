package balti.migrate.app.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector

data class AppNavBarItem(
    val route: Any,
    val label: String,
    val filledIconVector: ImageVector,
    val unfilledIconVector: ImageVector,
)