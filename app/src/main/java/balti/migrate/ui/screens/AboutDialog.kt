package balti.migrate.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import balti.migrate.BuildConfig
import balti.migrate.R
import baltiapps.migrate.domain.PRIVACY_POLICY_URL
import baltiapps.migrate.domain.RELEASE_URL

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {

        val version = buildAnnotatedString {
            append(stringResource(R.string.app_version, BuildConfig.VERSION_NAME))
            append("\n")
            withStyle(
                style = SpanStyle(fontStyle = FontStyle.Italic)
            ) {
                append(BuildConfig.VERSION_CODENAME)
            }
        }

        val links = buildAnnotatedString {
            withLink(LinkAnnotation.Url(url = RELEASE_URL)) {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    )
                ) {
                    append(stringResource(R.string.release_information))
                }
            }
            append("\n")
            withLink(LinkAnnotation.Url(url = PRIVACY_POLICY_URL)) {
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    )
                ) {
                    append(stringResource(R.string.privacy_policy))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                ,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.notification_icon_00),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(R.string.app_name),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
                Text(
                    text = version,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = links,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.copyright),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(android.R.string.ok),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        onDismissRequest()
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun AboutDialogPreview() {
    AboutDialog(
        onDismissRequest = {}
    )
}