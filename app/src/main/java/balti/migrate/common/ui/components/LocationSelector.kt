package balti.migrate.common.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import balti.migrate.R

@Composable
fun LocationSelector(
    isFallback: Boolean,
    locationLabel: String,
    isLocationAccessible: Boolean,
    onSelectClicked: () -> Unit,
    modifier: Modifier = Modifier,
    fallbackWarning: String? = null,
) {
    val isWarning = isFallback || !isLocationAccessible

    val cardColor = if (isWarning) {
        CardDefaults.cardColors()
    } else CardDefaults.outlinedCardColors()

    val cardBorder = if (!isWarning) {
        CardDefaults.outlinedCardBorder()
    } else null

    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        colors = cardColor,
        border = cardBorder,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isWarning) {
                    Icon(
                        painter = painterResource(R.drawable.outline_warning_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                    )
                }
                Text(
                    modifier = Modifier.weight(1F),
                    text = when {
                        isFallback -> fallbackWarning ?: stringResource(R.string.using_fallback_location)
                        !isLocationAccessible -> stringResource(R.string.location_not_accessible)
                        else -> stringResource(R.string.backup_location_label, locationLabel)
                    },
                    fontWeight = if (isWarning) {
                        FontWeight.Bold
                    } else FontWeight.Normal
                )
                if (isWarning) {
                    Button(onClick = onSelectClicked) {
                        Text(stringResource(R.string.setup))
                    }
                } else {
                    OutlinedButton(onClick = onSelectClicked) {
                        Text(stringResource(R.string.change))
                    }
                }
            }
            if (isFallback && isLocationAccessible) {
                Text(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    ),
                    text = stringResource(R.string.backup_location_label, locationLabel),
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

@Preview
@Composable
private fun LocationSelectorPreview() {
    Surface {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LocationSelector(
                isFallback = true,
                locationLabel = "Download/Migrate",
                isLocationAccessible = true,
                onSelectClicked = {},
            )
            LocationSelector(
                isFallback = false,
                locationLabel = "Migrate",
                isLocationAccessible = false,
                onSelectClicked = {},
            )
            LocationSelector(
                isFallback = true,
                locationLabel = "Migrate",
                isLocationAccessible = true,
                onSelectClicked = {},
                fallbackWarning = stringResource(R.string.location_not_set_warning),
            )
            LocationSelector(
                isFallback = false,
                locationLabel = "Migrate",
                isLocationAccessible = true,
                onSelectClicked = {},
            )
        }
    }
}