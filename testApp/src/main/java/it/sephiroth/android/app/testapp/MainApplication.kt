package it.sephiroth.android.app.testapp

import android.app.Application
import timber.log.Timber


/**
 * RxJavaExtensions
 *
 * @author Alessandro Crugnola on 15.12.21 - 12:43
 */
class MainApplication : Application() {
    override fun onCreate() {
        Timber.plant(Timber.DebugTree())
        super.onCreate()
    }
}
