package ca.kallistesoft.unarrow.core.testing

import ca.kallistesoft.unarrow.core.engagement.ReviewPrompt
import ca.kallistesoft.unarrow.core.engagement.Sharer

/** Counts rating prompts and records shared texts. */
class FakeEngagement : ReviewPrompt, Sharer {
    var reviewAsks = 0
    val shared = mutableListOf<String>()

    override suspend fun afterWin() {
        reviewAsks++
    }

    override fun share(text: String, chooserTitle: String) {
        shared += text
    }
}
