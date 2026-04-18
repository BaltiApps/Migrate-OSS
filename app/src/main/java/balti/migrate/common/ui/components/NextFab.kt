package balti.migrate.common.ui.components

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

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
    class Disabled(
        override val label: String,
    ): ButtonStatus {
        override val onPressed: () -> Unit = {}
    }
    class Unspecified(
        override val label: String,
        override val onPressed: () -> Unit,
    ): ButtonStatus
}

private val NoOpInteractionSource = object : MutableInteractionSource {
    override val interactions: Flow<Interaction> = emptyFlow()
    override suspend fun emit(interaction: Interaction) {}
    override fun tryEmit(interaction: Interaction) = false
}

@Composable
fun NextFab(
    buttonStatus: ButtonStatus,
) {
    val onFabColor = when(buttonStatus) {
        is ButtonStatus.Loading,
        is ButtonStatus.Disabled ->
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f)
        is ButtonStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val fabBackgroundColor = when(buttonStatus) {
        is ButtonStatus.Error -> MaterialTheme.colorScheme.errorContainer
        is ButtonStatus.Disabled -> FloatingActionButtonDefaults.containerColor.copy(alpha = 0.38f)
        else -> FloatingActionButtonDefaults.containerColor
    }
    val interactionSource = if (buttonStatus is ButtonStatus.Disabled) {
        NoOpInteractionSource
    } else {
        remember { MutableInteractionSource() }
    }
    // https://stackoverflow.com/a/74312669/10967630
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ExtendedFloatingActionButton(
            containerColor = fabBackgroundColor,
            elevation = if (buttonStatus is ButtonStatus.Disabled) {
                FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
            } else {
                FloatingActionButtonDefaults.elevation()
            },
            interactionSource = interactionSource,
            onClick = { buttonStatus.onPressed() },
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
                    is ButtonStatus.Disabled,
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
