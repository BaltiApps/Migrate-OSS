package balti.migrate.restore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import balti.migrate.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScreenRestore(
    navController: NavController,
) {
    val viewModel: ScreenRestoreViewModel = koinViewModel<ScreenRestoreViewModel>()

    val state by viewModel.state.collectAsState()

    Content(
        state = { state },
        goBack = navController::navigateUp
    )
}

@Composable
private fun Content(
    state: () -> ScreenRestoreState,
    goBack: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { values ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(values)
                .padding(dimensionResource(R.dimen.screen_padding)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            Box(modifier = Modifier.fillMaxWidth()) {
                Text(state().v.toString())
            }

            Button(
                onClick = goBack,
                enabled = state().v == 5
            ) {
                Text(
                    "Go back"
                )
            }
        }
    }
}