package ca.kallistesoft.unarrow.core.common

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * The activity on screen, for full-screen ads, the privacy options form, purchases, sharing and the review prompt,
 * so ViewModels never hold an activity.
 * Weakly held, and forgotten once paused.
 */
class ResumedActivity : Application.ActivityLifecycleCallbacks, CurrentActivity {
    private var resumed: WeakReference<Activity>? = null

    override fun get(): Activity? = resumed?.get()

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
