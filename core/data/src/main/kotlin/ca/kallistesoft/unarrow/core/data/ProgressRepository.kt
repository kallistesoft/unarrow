package ca.kallistesoft.unarrow.core.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Today as a local calendar day number (days since the epoch); swapped out in tests. */
fun interface Today {
    operator fun invoke(): Long

    companion object {
        val System = Today { LocalDate.now().toEpochDay() }
    }
}

interface ProgressRepository {
    val progress: Flow<GameProgress>

    /**
     * [level] was solved for [stars]: the next one opens (replaying an older level opens nothing new) and the level
     * keeps its best stars.
     */
    suspend fun completeLevel(level: Int, stars: Int)

    /**
     * Today's daily puzzle was solved as [result]: the streak grows if yesterday's was too, else starts again at 1, and
     * the best daily result is kept.
     */
    suspend fun completeDaily(result: DailyResult)

    /** Spends a hint; false when there are none left. */
    suspend fun useHint(): Boolean

    suspend fun addHints(count: Int)

    suspend fun setSound(on: Boolean)

    suspend fun setHaptics(on: Boolean)

    suspend fun markReviewPrompted()

    suspend fun setRemoveAdsOwned(owned: Boolean)

    /** The tip [tip] was shown: it won't show again. */
    suspend fun markTipSeen(tip: String)
}

@Singleton
internal class DataStoreProgressRepository @Inject constructor(
    private val store: DataStore<GameProgress>,
    private val today: Today,
) : ProgressRepository {
    override val progress: Flow<GameProgress> = store.data

    override suspend fun completeLevel(level: Int, stars: Int) {
        store.updateData { it.withLevelSolved(level, stars) }
    }

    override suspend fun completeDaily(result: DailyResult) {
        val day = today()
        store.updateData { it.withDailySolved(day, result) }
    }

    override suspend fun useHint(): Boolean {
        if (progress.first().hints <= 0) return false
        var used = false
        store.updateData {
            used = it.hints > 0
            if (used) it.copy(hints = it.hints - 1) else it
        }
        return used
    }

    override suspend fun addHints(count: Int) {
        store.updateData { it.copy(hints = it.hints + count) }
    }

    override suspend fun setSound(on: Boolean) {
        store.updateData { it.copy(soundOn = on) }
    }

    override suspend fun setHaptics(on: Boolean) {
        store.updateData { it.copy(hapticsOn = on) }
    }

    override suspend fun markReviewPrompted() {
        val day = today()
        store.updateData { it.copy(lastReviewPromptDay = day) }
    }

    override suspend fun setRemoveAdsOwned(owned: Boolean) {
        store.updateData { it.copy(removeAdsOwned = owned) }
    }

    override suspend fun markTipSeen(tip: String) {
        store.updateData { it.copy(tipsSeen = it.tipsSeen + tip) }
    }
}

internal object GameProgressSerializer : Serializer<GameProgress> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue = GameProgress()

    override suspend fun readFrom(input: InputStream): GameProgress = try {
        json.decodeFromString(GameProgress.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Unreadable progress file", e)
    }

    override suspend fun writeTo(t: GameProgress, output: OutputStream) {
        output.write(json.encodeToString(GameProgress.serializer(), t).encodeToByteArray())
    }
}
