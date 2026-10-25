package ca.kallistesoft.unarrow.core.update.di

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import ca.kallistesoft.unarrow.core.update.MinVersionSource
import ca.kallistesoft.unarrow.core.update.RemoteConfigMinVersionSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK])
class UpdateModuleTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `the cached rules live in the force_update preferences file`() = runBlocking {
        val store = UpdateModule.store(context)
        val key = longPreferencesKey("min_version_code")
        assertNull(store.data.first()[key])
        store.edit { it[key] = 7 }
        assertEquals(7L, store.data.first()[key])
        assertTrue(context.preferencesDataStoreFile("force_update").exists())
    }

    @Test
    fun `the minimum version comes from Remote Config`() = runBlocking {
        var asked: String? = null
        val source = RemoteConfigMinVersionSource { key -> asked = key; 12L }
        assertEquals(12L, source.fetch("min_version_code_debug"))
        assertEquals("min_version_code_debug", asked)

        // Dagger implements the @Binds; this stands in for it to check what the interface is bound to.
        val module = object : UpdateModule() {
            override fun minVersionSource(impl: RemoteConfigMinVersionSource): MinVersionSource = impl
        }
        assertSame(source, module.minVersionSource(source))
    }
}
