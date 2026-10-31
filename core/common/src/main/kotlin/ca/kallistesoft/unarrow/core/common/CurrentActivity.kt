package ca.kallistesoft.unarrow.core.common

import android.app.Activity

/** The activity on screen now, or null between screens and in the background. */
fun interface CurrentActivity {
    fun get(): Activity?
}
