package balti.migrate.restore.di

import balti.migrate.restore.ui.ScreenRestoreViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val restoreDiModule = module {

    viewModel<ScreenRestoreViewModel> {
        ScreenRestoreViewModel()
    }

}