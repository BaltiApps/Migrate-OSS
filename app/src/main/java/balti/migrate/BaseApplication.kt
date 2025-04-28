package balti.migrate

import android.app.Application
import balti.migrate.app.di.appDiModule
import balti.migrate.backup.di.backupDiModule
import balti.migrate.common.di.commonDiModule
import balti.migrate.restore.di.restoreDiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class BaseApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        startKoin {
            androidContext(this@BaseApplication)
            modules(commonDiModule, backupDiModule, restoreDiModule, appDiModule)
        }
    }
}