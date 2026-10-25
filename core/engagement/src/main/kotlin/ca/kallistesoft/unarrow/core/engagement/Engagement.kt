package ca.kallistesoft.unarrow.core.engagement

/** Play's in-app rating card, asked at good moments only. Tests use a fake (`FakeEngagement` in `:core:testing`). */
fun interface ReviewPrompt {
    /** Called right after a solved puzzle; asks Play for its rating card if the rules allow it now. */
    suspend fun afterWin()
}

/** Android's share sheet. */
fun interface Sharer {
    /** Shares [text] (it already ends with the Play Store link); [chooserTitle] heads the sheet. */
    fun share(text: String, chooserTitle: String)
}

/** The Play Store page of the release build, the link every shared result carries. */
const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=ca.kallistesoft.unarrow"

/** No rating card before this many solved puzzles: the player has to like the game first. */
const val REVIEW_MIN_WINS = 3

/** At most one rating card in this many days (Play also limits it on its side). */
const val REVIEW_SPACING_DAYS = 30L

/** The rating card is due after a win once [wins] reach [REVIEW_MIN_WINS] and none was asked in 30 days. */
fun reviewDue(wins: Int, lastPromptDay: Long?, today: Long): Boolean =
    wins >= REVIEW_MIN_WINS && (lastPromptDay == null || today - lastPromptDay >= REVIEW_SPACING_DAYS)
