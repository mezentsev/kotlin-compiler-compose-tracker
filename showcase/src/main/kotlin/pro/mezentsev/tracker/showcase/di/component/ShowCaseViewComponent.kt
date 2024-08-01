package pro.mezentsev.tracker.showcase.di.component

import pro.mezentsev.tracker.showcase.di.scope.PerShowCase
import dagger.Component

@PerShowCase
@Component(dependencies = [ShowCaseViewDependencies::class])
interface ShowCaseViewComponent {

  @Component.Factory
  interface Factory {
    fun create(showCaseViewDependencies: ShowCaseViewDependencies): ShowCaseViewComponent
  }
}