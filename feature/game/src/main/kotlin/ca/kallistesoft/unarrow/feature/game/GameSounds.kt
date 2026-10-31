package ca.kallistesoft.unarrow.feature.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ca.kallistesoft.unarrow.feature.game.R as GameR

enum class Sound { EXIT, BLOCKED, WIN }

/** The game's three short sounds (res/raw), played through the game audio stream so they follow its volume. */
internal class GameSounds(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = mapOf(
        Sound.EXIT to pool.load(context, GameR.raw.sound_exit, 1),
        Sound.BLOCKED to pool.load(context, GameR.raw.sound_blocked, 1),
        Sound.WIN to pool.load(context, GameR.raw.sound_win, 1),
    )

    /** Plays [sound]; the stream id, 0 when it couldn't play (not loaded yet). */
    fun play(sound: Sound): Int = pool.play(ids.getValue(sound), 1f, 1f, 1, 0, 1f)

    fun release() = pool.release()

    private companion object {
        const val MAX_STREAMS = 4
    }
}

/** A pop when an arrow leaves, a bump when it's blocked, a chime when the puzzle is solved; nothing when off. */
@Composable
internal fun Sounds(session: GameSession, enabled: Boolean) {
    val context = LocalContext.current
    val sounds = remember(context) { GameSounds(context) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    LaunchedEffect(session.board.arrows.size) {
        if (enabled && session.exits.isNotEmpty() && session.status == GameStatus.PLAYING) sounds.play(Sound.EXIT)
    }
    LaunchedEffect(session.blocked?.serial) {
        if (enabled && session.blocked != null) sounds.play(Sound.BLOCKED)
    }
    LaunchedEffect(session.status) {
        if (enabled && session.status == GameStatus.WON) sounds.play(Sound.WIN)
    }
}
