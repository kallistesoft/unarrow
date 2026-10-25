package ca.kallistesoft.unarrow.core.update

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ForceUpdateTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `only builds below the minimum are blocked`() {
        assertFalse(isUpdateRequired(currentVersionCode = 5, minVersionCode = null))
        assertFalse(isUpdateRequired(currentVersionCode = 5, minVersionCode = 5))
        assertFalse(isUpdateRequired(currentVersionCode = 5, minVersionCode = 0))
        assertTrue(isUpdateRequired(currentVersionCode = 5, minVersionCode = 6))
    }

    @Test
    fun `the last fetched minimum is kept when the network fails`() = runTest {
        var remote: Long? = 3
        val repo = repository(this, currentVersionCode = 2) { remote }

        assertFalse("never blocked before the rules are known", repo.updateRequired.first())
        repo.refresh()
        assertTrue(repo.updateRequired.first())

        remote = null // offline
        repo.refresh()
        assertTrue("still blocked offline", repo.updateRequired.first())

        remote = 1 // minimum lowered again
        repo.refresh()
        assertFalse(repo.updateRequired.first())
    }

    @Test
    fun `the remote parameter of this build type is read`() = runTest {
        var asked: String? = null
        val repo = ForceUpdateRepository(
            store = PreferenceDataStoreFactory.create(scope = backgroundScope) { tmp.root.resolve("asked.preferences_pb") },
            source = { parameter -> asked = parameter; 0 },
            settings = ForceUpdateSettings("min_version_code_debug", currentVersionCode = 2, storePackage = "ca.example"),
        )
        repo.refresh()
        assertEquals("min_version_code_debug", asked)
        assertFalse(repo.updateRequired.first())
    }

    private fun repository(scope: TestScope, currentVersionCode: Long, fetch: () -> Long?) =
        ForceUpdateRepository(
            store = PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
                tmp.root.resolve("force_update.preferences_pb")
            },
            source = { fetch() },
            settings = ForceUpdateSettings("min_version_code", currentVersionCode, "ca.example"),
        )
}
