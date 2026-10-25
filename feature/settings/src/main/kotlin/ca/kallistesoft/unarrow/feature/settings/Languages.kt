package ca.kallistesoft.unarrow.feature.settings

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import ca.kallistesoft.unarrow.core.ui.R

/** The languages the game ships in; [tag] is empty for the system's own. */
enum class AppLanguage(val tag: String, @StringRes val label: Int) {
    SYSTEM("", R.string.settings_language_system),
    ENGLISH("en", R.string.language_en),
    FRENCH_CANADA("fr-CA", R.string.language_fr_ca),
    FRENCH_FRANCE("fr-FR", R.string.language_fr_fr),
    ;

    companion object {
        /** The language chosen in the app, or [SYSTEM]. */
        fun current(): AppLanguage = fromTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())

        /** The language of AppCompat's language tags ("fr-CA", "en-US,fr"); [SYSTEM] when none of ours. */
        fun fromTags(tags: String): AppLanguage {
            val first = tags.substringBefore(',')
            if (first.isEmpty()) return SYSTEM
            return entries.firstOrNull { it != SYSTEM && it.tag.equals(first, ignoreCase = true) }
                ?: when (first.substringBefore('-').lowercase()) {
                    "en" -> ENGLISH
                    "fr" -> FRENCH_FRANCE
                    else -> SYSTEM
                }
        }
    }
}

/** Switches the whole app (the activity is recreated in the new language). */
fun AppLanguage.select() {
    AppCompatDelegate.setApplicationLocales(
        if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
    )
}
