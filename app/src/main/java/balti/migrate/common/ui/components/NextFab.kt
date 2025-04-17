package balti.migrate.common.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

interface ButtonStatus {
    val label: String
    val onPressed: () -> Unit

    class Success(
        override val label: String,
        override val onPressed: () -> Unit,
    ): ButtonStatus
    class Error(
        override val label: String,
        override val onPressed: () -> Unit,
    ): ButtonStatus
    class Loading(
        override val label: String,
        override val onPressed: () -> Unit,
    ): ButtonStatus
    class Unspecified(
        override val label: String,
        override val onPressed: () -> Unit,
    ): ButtonStatus
}

@Composable
fun NextFab(
    buttonStatus: ButtonStatus,
) {
    val onFabColor = when(buttonStatus) {
        is ButtonStatus.Loading -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f)
        is ButtonStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val fabBackgroundColor = when(buttonStatus) {
        is ButtonStatus.Error -> MaterialTheme.colorScheme.errorContainer
        else -> FloatingActionButtonDefaults.containerColor
    }
    // https://stackoverflow.com/a/74312669/10967630
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ExtendedFloatingActionButton(
            containerColor = fabBackgroundColor,
            onClick = {
                buttonStatus.onPressed()
            },
            icon = {
                when(buttonStatus) {
                    is ButtonStatus.Success -> {
                        Icon(
                            imageVector = Icons.Outlined.Done,
                            tint = onFabColor,
                            contentDescription = null,
                        )
                    }
                    is ButtonStatus.Error -> {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            tint = onFabColor,
                            contentDescription = null,
                        )
                    }
                    is ButtonStatus.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = onFabColor
                        )
                    }
                    is ButtonStatus.Unspecified -> {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            tint = onFabColor,
                            contentDescription = null
                        )
                    }
                }
            },
            text = {
                Text(
                    text = buttonStatus.label,
                    color = onFabColor,
                )
            },
        )
    }
}