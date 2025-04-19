package balti.migrate.restore.ui.screens.restoreSummary.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import balti.migrate.R
import balti.migrate.restore.ui.screens.restoreSummary.RestoreSummaryState

@Composable
fun SpecialPermissions(
    state: RestoreSummaryState,
    modifier: Modifier = Modifier,
) {
    if (state.countSms <= 0) return
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.special_permissions_required),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
        }
        Spacer(Modifier.size(8.dp))
        SummaryItem(
            headline = stringResource(R.string.set_as_default_sms_app),
            icon = Icons.Outlined.Sms,
            userActionState = state.smsUserActionState,
        )
    }
}