package balti.migrate.common.ui.listScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import balti.migrate.R

@Composable
fun ListPermissionRequestLayout(
    description: String,
    onRequestPermission: () -> Unit,
    onSkip: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = description,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.size(16.dp))
        Button(
            onClick = onRequestPermission
        ) {
            Text(text = stringResource(R.string.request_permission))
        }
        if (onSkip == null) return@Column
        TextButton(
            onClick = onSkip
        ) {
            Text(stringResource(R.string.skip))
        }
    }
}

@Preview
@Composable
private fun RequestPreview() {
    ListPermissionRequestLayout(
        description = stringResource(R.string.contacts_backup_permission_description),
        onRequestPermission = {},
        onSkip = {}
    )
}