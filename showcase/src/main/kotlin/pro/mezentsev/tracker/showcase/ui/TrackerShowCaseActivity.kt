@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package pro.mezentsev.tracker.showcase.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import pro.mezentsev.tracker.showcase.di.component.DaggerShowCaseActivityComponent

@Suppress("MagicNumber")
class TrackerShowCaseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DaggerShowCaseActivityComponent
            .factory()
            .create(this)
            .inject(this)

        setContent {
            MaterialTheme {
                SimpleLayout()
            }
        }
    }
}
