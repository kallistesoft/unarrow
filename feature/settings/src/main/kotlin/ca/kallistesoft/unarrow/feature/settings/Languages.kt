package ca.kallistesoft.unarrow.feature.settings

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import ca.kallistesoft.unarrow.core.ui.R

/**
 * The languages the game offers: the system's own, English or French. French follows the phone's region: Canada gets
 * the Quebec wording (values-fr-rCA), everywhere else France's (values-fr), so nobody has to pick a variant.
 */
enum class AppLanguage(@StringRes val label: Int) {
    SYSTEM(R.string.settings_language_system),
    ENGLISH(R.string.language_en),
    FRENCH(R.string.language_fr),
    ;

    /** The language tag to apply; empty for the system's own. [region] is the phone's region ("CA", "FR", ""). */
    fun tag(region: String = systemRegion()): String = when (this) {
        SYSTEM -> ""
        ENGLISH -> "en"
        FRENCH -> if (region.equals("CA", ignoreCase = true)) "fr-CA" else "fr-FR"
    }

    companion object {
        /** The language chosen in the app, or [SYSTEM]. */
        fun current(): AppLanguage = fromTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())

        /**
         * The language of AppCompat's language tags ("en-US,fr"): any English is [ENGLISH], any French [FRENCH] (the
         * "fr-CA" and "fr-FR" choices of earlier versions too), anything else [SYSTEM].
         */
        fun fromTags(tags: String): AppLanguage = when (tags.substringBefore(',').substringBefore('-').lowercase()) {
            "en" -> ENGLISH
            "fr" -> FRENCH
            else -> SYSTEM
        }

        /** The phone's own region, which the app's language choice doesn't change. */
        fun systemRegion(): String = Resources.getSystem().configuration.locales[0].country
    }
}

/** Switches the whole app (the activity is recreated in the new language). */
fun AppLanguage.select() {
    val tag = tag()
    AppCompatDelegate.setApplicationLocales(
        if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
    )
}
