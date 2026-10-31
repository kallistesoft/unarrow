package ca.kallistesoft.unarrow.core.testing

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Sets [content] and gives it a status bar [top] px high and a navigation bar [bottom] px high, as an edge-to-edge
 * window on a phone gets them (Robolectric windows have none), to check that screens keep clear of the system bars.
 */
fun ComposeContentTestRule.setContentWithSystemBars(top: Int, bottom: Int, content: @Composable () -> Unit) {
    lateinit var view: View
    setContent {
        view = LocalView.current
        content()
    }
    runOnIdle {
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, top, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, bottom))
            .build()
        ViewCompat.dispatchApplyWindowInsets(view, insets)
    }
    waitForIdle()
}
