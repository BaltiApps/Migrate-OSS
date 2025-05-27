package balti.migrate.restore.ui.screens.restoreSummary2.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import balti.migrate.R
import balti.migrate.restore.ui.screens.restoreSummary2.RestoreSummaryState2


@Composable
fun DelegatedRestore2(
    state: RestoreSummaryState2,
    modifier: Modifier = Modifier,
) {
    if (state.countContacts <= 0) return
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
                text = stringResource(R.string.delegated_restore),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
        }
        Spacer(Modifier.size(8.dp))
        SummaryItem2(
            headlineStringRes = R.string.contacts_to_restore,
            count = state.countContacts,
            icon = Icons.Outlined.Contacts,
            state = state.contactSummaryState,
        )
    }
}