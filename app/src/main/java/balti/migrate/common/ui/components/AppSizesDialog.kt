package balti.migrate.common.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import balti.migrate.R
import baltiapps.migrate.domain.common.model.AppSizeInfo
import baltiapps.migrate.domain.common.utils.StringUtils.getHumanReadableSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSizesDialog(
    appSizeInfos: List<AppSizeInfo>,
    onDismiss: () -> Unit,
    getAppName: (packageName: String) -> String,
    modifier: Modifier = Modifier,
) {
    val sorted = remember(appSizeInfos) {
        appSizeInfos.sortedByDescending { it.bytesTotal }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.app_sizes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                ) {
                    items(sorted) { appSizeInfo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = getAppName(appSizeInfo.packageName),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = getHumanReadableSize(appSizeInfo.bytesTotal),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                HorizontalDivider()
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}
