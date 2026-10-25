package ca.kallistesoft.unarrow.core.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ProgressRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private var day = 20_800L

    @Test
    fun `solving a level opens the next one and counts a win`() = runTest {
        val repo = repository()
        repo.completeLevel(1, stars = 2)
        repo.completeLevel(2, stars = 2)
        val p = repo.progress.first()
        assertEquals(3, p.level)
        assertEquals(2, p.wins)
    }

    @Test
    fun `replaying an old level opens nothing new`() = runTest {
        val repo = repository()
        repo.completeLevel(4, stars = 2)
        repo.completeLevel(2, stars = 2)
        assertEquals(5, repo.progress.first().level)
    }

    @Test
    fun `the daily streak grows day after day and starts over after a missed day`() = runTest {
        val repo = repository()
        repo.completeDaily(DailyResult(stars = 2, seconds = 90))
        day++
        repo.completeDaily(DailyResult(stars = 2, seconds = 90))
        assertEquals(2, repo.progress.first().dailyStreak)
        day += 2
        repo.completeDaily(DailyResult(stars = 2, seconds = 90))
        val p = repo.progress.first()
        assertEquals(1, p.dailyStreak)
        assertEquals(2, p.bestDailyStreak)
        assertEquals(3, p.wins)
    }

    @Test
    fun `solving the daily twice the same day counts once`() = runTest {
        val repo = repository()
        repo.completeDaily(DailyResult(stars = 2, seconds = 90))
        repo.completeDaily(DailyResult(stars = 2, seconds = 90))
        val p = repo.progress.first()
        assertEquals(1, p.dailyStreak)
        assertEquals(1, p.wins)
        assertTrue(p.dailyDone(day))
    }

    @Test
    fun `the theme follows the system until one is picked`() = runTest {
        val repo = repository()
        assertEquals(AppTheme.SYSTEM, repo.progress.first().theme)
        repo.setTheme(AppTheme.DARK)
        assertEquals(AppTheme.DARK, repo.progress.first().theme)
    }

    @Test
    fun `tips are remembered once shown`() = runTest {
        val repo = repository()
        repo.markTipSeen("LOCKS")
        repo.markTipSeen("LOCKS")
        assertEquals(setOf("LOCKS"), repo.progress.first().tipsSeen)
    }

    @Test
    fun `each level keeps its best stars`() = runTest {
        val repo = repository()
        repo.completeLevel(3, stars = 2)
        repo.completeLevel(3, stars = 1)
        repo.completeLevel(4, stars = 3)
        assertEquals(mapOf(3 to 2, 4 to 3), repo.progress.first().levelStars)
        repo.completeLevel(3, stars = 3)
        assertEquals(3, repo.progress.first().levelStars[3])
    }

    @Test
    fun `the best daily is the most stars, then the fastest, even replayed the same day`() = runTest {
        val repo = repository()
        repo.completeDaily(DailyResult(stars = 2, seconds = 60))
        day++
        repo.completeDaily(DailyResult(stars = 3, seconds = 200))
        assertEquals(DailyResult(3, 200), repo.progress.first().bestDaily)
        repo.completeDaily(DailyResult(stars = 3, seconds = 150))
        val p = repo.progress.first()
        assertEquals(DailyResult(3, 150), p.bestDaily)
        assertEquals("the same day counts once", 2, p.wins)
        day++
        repo.completeDaily(DailyResult(stars = 2, seconds = 10))
        assertEquals(DailyResult(3, 150), repo.progress.first().bestDaily)
        assertFalse(DailyResult(3, 150).beats(DailyResult(3, 150)))
    }

    @Test
    fun `the streak shows until the day after the last daily`() {
        val p = GameProgress(dailyStreak = 5, lastDailyDay = 100)
        assertEquals(5, p.currentStreak(100))
        assertEquals(5, p.currentStreak(101))
        assertEquals(0, p.currentStreak(102))
        assertEquals(0, GameProgress().currentStreak(100))
        assertFalse(p.dailyDone(101))
    }

    @Test
    fun `hints are spent until none are left, and can be added`() = runTest {
        val repo = repository()
        repeat(GameProgress.STARTING_HINTS) { assertTrue(repo.useHint()) }
        assertFalse(repo.useHint())
        repo.addHints(10)
        assertEquals(10, repo.progress.first().hints)
    }

    @Test
    fun `settings, the review prompt and the purchase are saved`() = runTest {
        val repo = repository()
        repo.setSound(false)
        repo.setHaptics(false)
        repo.markReviewPrompted()
        repo.setRemoveAdsOwned(true)
        val p = repo.progress.first()
        assertFalse(p.soundOn)
        assertFalse(p.hapticsOn)
        assertEquals(day, p.lastReviewPromptDay)
        assertTrue(p.removeAdsOwned)
    }

    @Test
    fun `progress survives a write and read, unknown keys ignored`() = runTest {
        val out = ByteArrayOutputStream()
        val progress = GameProgress(level = 12, hints = 7, levelStars = mapOf(3 to 2), bestDaily = DailyResult(3, 42))
        GameProgressSerializer.writeTo(progress, out)
        assertEquals(progress, GameProgressSerializer.readFrom(ByteArrayInputStream(out.toByteArray())))
        val withExtra = """{"level":3,"future":true}""".byteInputStream()
        assertEquals(3, GameProgressSerializer.readFrom(withExtra).level)
    }

    @Test(expected = CorruptionException::class)
    fun `an unreadable file is reported as corrupt`() = runTest {
        GameProgressSerializer.readFrom("not json".byteInputStream())
    }

    @Test
    fun `the system clock gives today's date`() {
        assertEquals(java.time.LocalDate.now().toEpochDay(), Today.System())
    }

    private fun TestScope.repository() = DataStoreProgressRepository(
        store = DataStoreFactory.create(GameProgressSerializer, scope = backgroundScope) {
            tmp.root.resolve("game_progress.json")
        },
        today = { day },
    )
}
