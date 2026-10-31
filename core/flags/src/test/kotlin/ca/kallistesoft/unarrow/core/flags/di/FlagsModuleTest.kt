package ca.kallistesoft.unarrow.core.flags.di

import android.content.Context
import ca.kallistesoft.unarrow.core.flags.DefaultFeatureFlags
import ca.kallistesoft.unarrow.core.flags.DefaultRemoteNumbers
import ca.kallistesoft.unarrow.core.flags.FeatureFlagSettings
import ca.kallistesoft.unarrow.core.flags.NoRemoteConfigValues
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.After
import org.junit.Assert.assertNotNull
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
class FlagsModuleTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val settings = FeatureFlagSettings(minimumFetchIntervalSeconds = 3600)

    @After
    fun tearDown() = FirebaseApp.clearInstancesForTest()

    @Test
    fun `without a google-services json, flags have their defaults and nothing is known remotely`() {
        val holder = FlagsModule.remoteConfig(context, settings)
        assertNull(holder.firebase)
        assertTrue(FlagsModule.featureFlags(holder) is DefaultFeatureFlags)
        assertTrue(FlagsModule.remoteNumbers(holder) is DefaultRemoteNumbers)
        assertSame(NoRemoteConfigValues, FlagsModule.remoteConfigValues(holder))
    }

    @Test
    fun `with Firebase, Remote Config provides both`() {
        FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApplicationId("1:1234567890:android:0123456789abcdef")
                .setApiKey("test-api-key")
                .setProjectId("unarrow-test")
                .build(),
        )
        val holder = FlagsModule.remoteConfig(context, settings)
        val firebase = holder.firebase
        assertNotNull(firebase)
        assertSame(firebase, FlagsModule.featureFlags(holder))
        assertSame(firebase, FlagsModule.remoteNumbers(holder))
        assertSame(firebase, FlagsModule.remoteConfigValues(holder))
    }
}
