package pro.mezentsev.tracker.showcase

import android.app.Application
import pro.mezentsev.tracker.showcase.di.component.AppComponent
import pro.mezentsev.tracker.showcase.di.component.DaggerAppComponent

class TrackerShowCaseApp : Application() {
    override fun onCreate() {
        super.onCreate()

        appComponent = DaggerAppComponent
            .factory()
            .create(this)
    }

    companion object {
        lateinit var appComponent: AppComponent
    }
}
