package balti.migrate.common.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import balti.migrate.R

@Composable
fun ExternalDataCountBar(
    totalCount: Int,
    selectedCount: Int,
    isStaging: Boolean,
    areAllExternalDataSelected: Boolean,
    areAllExternalMediaSelected: Boolean,
    onAllExternalDataToggled: (Boolean) -> Unit,
    onAllExternalMediaToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shouldEnableExternalDataSelection: Boolean = true,
    shouldEnableExternalMediaSelection: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${stringResource(R.string.selected_items)} - $selectedCount/$totalCount",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.external_data_count_bar_legend),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.basicMarquee(),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CheckboxLabel("D")
            Checkbox(
                checked = areAllExternalDataSelected,
                onCheckedChange = onAllExternalDataToggled,
                enabled = !isStaging && shouldEnableExternalDataSelection,
                modifier = Modifier.removeTopPadding(),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CheckboxLabel("M")
            Checkbox(
                checked = areAllExternalMediaSelected,
                onCheckedChange = onAllExternalMediaToggled,
                enabled = !isStaging && shouldEnableExternalMediaSelection,
                modifier = Modifier.removeTopPadding(),
            )
        }
    }
}
