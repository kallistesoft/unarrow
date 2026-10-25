package ca.kallistesoft.unarrow.core.designsystem

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Previews every component and screen in the two combinations that catch most problems: light in English and dark
 * in French (longer strings).
 */
@Preview(name = "Light · EN", locale = "en", showBackground = true)
@Preview(name = "Dark · FR", locale = "fr", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class UnarrowPreviews

/** Theme and background for previews and screenshot tests. */
@Composable
fun PreviewSurface(content: @Composable () -> Unit) {
    UnarrowTheme {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
