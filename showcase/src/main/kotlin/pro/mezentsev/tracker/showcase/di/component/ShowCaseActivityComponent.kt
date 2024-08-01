package pro.mezentsev.tracker.showcase.di.component

import android.content.Context
import pro.mezentsev.tracker.showcase.annotation.UiContextShowCase
import pro.mezentsev.tracker.showcase.ui.TrackerShowCaseActivity
import dagger.BindsInstance
import dagger.Component

@Component
interface ShowCaseActivityComponent {
  fun inject(trackerShowCaseActivity: TrackerShowCaseActivity)

  @Component.Factory
  interface Factory {
    fun create(@BindsInstance @UiContextShowCase context: Context): ShowCaseActivityComponent
  }
}