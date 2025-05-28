package balti.migrate.app.ui.screens.appSettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import balti.migrate.R
import baltiapps.migrate.domain.common.sources.Preferences
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppSettings(
    updateUiState: (darkMode: Preferences.DarkMode, followSystemColors: Boolean) -> Unit,
    viewModel: AppSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Content(
        state = state,
        onChangeDarkMode = {
            viewModel.onAction(AppSettingsAction.ChangeDarkMode(it, updateUiState))
        },
        onChangeSystemColors = {
            viewModel.onAction(AppSettingsAction.ChangeShouldFollowSystemColors(it, updateUiState))
        },
    )
}

@Composable
private fun Content(
    state: AppSettingsState,
    onChangeDarkMode: (Preferences.DarkMode) -> Unit,
    onChangeSystemColors: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val commonModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)

    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = stringResource(R.string.appearance),
                style = MaterialTheme.typography.labelLarge,
                modifier = commonModifier,
            )
            ListItem(
                headlineContent = {
                    Text(text = stringResource(R.string.dark_mode))
                },
                supportingContent = {
                    Text(text = stringResource(R.string.dark_mode_description))
                },
            )
            RadioOption(
                state = state,
                darkMode = Preferences.DarkMode.SYSTEM,
                label = stringResource(R.string.follow_system),
                onChangeDarkMode = onChangeDarkMode,
            )
            RadioOption(
                state = state,
                darkMode = Preferences.DarkMode.DARK,
                label = stringResource(R.string.dark),
                onChangeDarkMode = onChangeDarkMode,
            )
            RadioOption(
                state = state,
                darkMode = Preferences.DarkMode.LIGHT,
                label = stringResource(R.string.light),
                onChangeDarkMode = onChangeDarkMode,
            )
            HorizontalDivider(
                modifier = commonModifier,
            )
            ListItem(
                headlineContent = {
                    Text(text = stringResource(R.string.color_theme))
                },
                supportingContent = {
                    Text(text = stringResource(R.string.color_theme_desc))
                },
            )
            RadioOption(
                state = state,
                followSystemColors = true,
                label = stringResource(R.string.follow_system),
                onChangeFollowSystemColors = onChangeSystemColors,
            )
            RadioOption(
                state = state,
                followSystemColors = false,
                label = stringResource(R.string.migrate_colors),
                onChangeFollowSystemColors = onChangeSystemColors,
            )
            HorizontalDivider(
                modifier = commonModifier,
            )
        }
    }
}

@Composable
private fun RadioOption(
    state: AppSettingsState,
    darkMode: Preferences.DarkMode,
    label: String,
    onChangeDarkMode: (Preferences.DarkMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onChangeDarkMode(darkMode)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = state.darkMode == darkMode,
            onClick = null,
        )
    }
}

@Composable
private fun RadioOption(
    state: AppSettingsState,
    followSystemColors: Boolean,
    label: String,
    onChangeFollowSystemColors: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onChangeFollowSystemColors(followSystemColors)
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = state.shouldFollowSystemColors == followSystemColors,
            onClick = null,
        )
    }
}