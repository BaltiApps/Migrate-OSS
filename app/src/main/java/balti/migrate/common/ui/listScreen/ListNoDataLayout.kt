package balti.migrate.common.ui.listScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import balti.migrate.R

@Composable
fun ListNoDataLayout(
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Column(
        modifier = modifier.padding(8.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterVertically
        ),
    ) {
        val dim = 0.75f
        Image(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(36.dp).alpha(dim),
        )
        Text(
            text = title ?: stringResource(R.string.no_data_title_default),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.alpha(dim),
        )
        Text(
            text = stringResource(R.string.no_data_desc),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.alpha(dim),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ListNoDataPreview() {
    ListNoDataLayout()
}