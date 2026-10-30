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
        repo.completeLevel(1, LevelResult(2, 30))
        repo.completeLevel(2, LevelResult(2, 30))
        val p = repo.progress.first()
        assertEquals(3, p.level)
        assertEquals(2, p.wins)
    }

    @Test
    fun `replaying an old level opens nothing new`() = runTest {
        val repo = repository()
        repo.completeLevel(4, LevelResult(2, 30))
        repo.completeLevel(2, LevelResult(2, 30))
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
    fun `total stars add up the best of each level`() {
        assertEquals(0, GameProgress().totalStars)
        assertEquals(7, GameProgress(levelResults = mapOf(1 to LevelResult(3), 2 to LevelResult(1), 5 to LevelResult(3, 9))).totalStars)
    }

    @Test
    fun `the best result is the most stars, then the fastest, a tie keeps the earlier`() {
        assertTrue(LevelResult(2, 90).beats(null))
        assertTrue(LevelResult(3, 200).beats(LevelResult(2, 10)))
        assertTrue(LevelResult(2, 50).beats(LevelResult(2, 60)))
        assertFalse("a tie keeps the earlier", LevelResult(2, 60).beats(LevelResult(2, 60)))
        assertTrue("a known time beats an unknown one", LevelResult(2, 60).beats(LevelResult(2)))
        assertFalse(LevelResult(2).beats(LevelResult(2, 60)))
        assertFalse(LevelResult(2).beats(LevelResult(2)))
    }

    @Test
    fun `replays only improve the best results, never the level, the last daily or the streak`() = runTest {
        val repo = repository()
        repo.completeLevel(5, LevelResult(2, 80))
        repo.replayLevel(5, LevelResult(1, 20))
        repo.replayLevel(3, LevelResult(3, 40))
        var p = repo.progress.first()
        assertEquals(6, p.level)
        assertEquals(mapOf(5 to LevelResult(2, 80), 3 to LevelResult(3, 40)), p.levelResults)

        repo.completeDaily(DailyResult(2, 100))
        repo.replayDaily(day - 3, DailyResult(3, 70))
        repo.replayDaily(day, DailyResult(3, 90))
        p = repo.progress.first()
        assertEquals(day, p.lastDailyDay)
        assertEquals(1 to 1, p.dailyStreak to p.bestDailyStreak)
        assertEquals(mapOf(day to DailyResult(3, 90), day - 3 to DailyResult(3, 70)), p.dailyResults)
        assertEquals(DailyResult(3, 70), p.bestDaily)
        assertEquals("today's result improved", DailyResult(3, 90), p.dailyResult(day))
        assertEquals(2, p.wins)
    }

    @Test
    fun `progress saved by earlier versions moves to the history`() = runTest {
        val old = """{"level":9,"levelStars":{"3":2,"8":3},"lastDailyDay":20800,"lastDailyResult":{"stars":3,"seconds":42},
            |"bestDaily":{"stars":3,"seconds":40}}""".trimMargin().byteInputStream()
        val p = GameProgressSerializer.readFrom(old)
        assertEquals(mapOf(3 to LevelResult(2), 8 to LevelResult(3)), p.levelResults)
        assertTrue(p.levelStars.isEmpty())
        assertEquals("only the last daily's day is known", mapOf(20_800L to DailyResult(3, 42)), p.dailyResults)
        assertEquals(p, p.migrated())
        assertEquals(GameProgress(), GameProgress().migrated())
        // A new result for a migrated level keeps the best: its stars, now with a time.
        assertEquals(LevelResult(2, 50), p.withLevelReplayed(3, LevelResult(2, 50)).levelResults[3])
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
        repo.completeLevel(3, LevelResult(2, 30))
        repo.completeLevel(3, LevelResult(1, 30))
        repo.completeLevel(4, LevelResult(3, 30))
        assertEquals(mapOf(3 to LevelResult(2, 30), 4 to LevelResult(3, 30)), repo.progress.first().levelResults)
        repo.completeLevel(3, LevelResult(3, 30))
        assertEquals(LevelResult(3, 30), repo.progress.first().levelResults[3])
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
        assertEquals("today's own result", DailyResult(2, 10), repo.progress.first().dailyResult(day))
        repo.completeDaily(DailyResult(stars = 1, seconds = 5))
        assertEquals("a worse replay keeps today's best", DailyResult(2, 10), repo.progress.first().dailyResult(day))
        assertEquals(null, repo.progress.first().dailyResult(day + 1))
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
        val progress = GameProgress(
            level = 12, hints = 7, levelResults = mapOf(3 to LevelResult(2)), bestDaily = DailyResult(3, 42),
            dailyResults = mapOf(20_800L to DailyResult(3, 42)),
        )
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
