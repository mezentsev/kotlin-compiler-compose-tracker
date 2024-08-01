package pro.mezentsev.tracker.showcase.di.component

import android.content.Context
import pro.mezentsev.tracker.showcase.annotation.UiContextShowCase
import javax.inject.Inject

data class ShowCaseViewDependencies @Inject constructor(
    @UiContextShowCase @get:UiContextShowCase
    val context: Context
)