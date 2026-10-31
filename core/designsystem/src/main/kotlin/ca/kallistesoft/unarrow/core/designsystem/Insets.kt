package ca.kallistesoft.unarrow.core.designsystem

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable

/**
 * What a screen's own top bar (a Row in a Scaffold's `topBar`, not a Material TopAppBar) keeps clear of: the status
 * bar and the display cutout, plus the side bars in landscape. The Scaffold's content padding then starts under the
 * bar, so the top inset is applied once.
 */
val WindowInsets.Companion.topBar: WindowInsets
    @Composable get() = safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
