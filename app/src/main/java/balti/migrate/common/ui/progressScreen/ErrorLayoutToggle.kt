package balti.migrate.common.ui.progressScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import balti.migrate.R

@Composable
fun ErrorLayoutToggle(
    isErrorOnly: Boolean,
    onToggleErrorOnly: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.show_only_errors))
        Switch(
            checked = isErrorOnly,
            onCheckedChange = onToggleErrorOnly
        )
    }
}

