package ca.kallistesoft.unarrow.core.engagement

import android.content.Context
import android.content.Intent

/** Android's share sheet, opened over the current screen (or as a new task from the application). */
internal class ShareSheet(private val context: Context, private val currentActivity: () -> android.app.Activity?) :
    Sharer {
    override fun share(text: String, chooserTitle: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        val chooser = Intent.createChooser(send, chooserTitle)
        val activity = currentActivity()
        if (activity != null) {
            activity.startActivity(chooser)
        } else {
            context.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
