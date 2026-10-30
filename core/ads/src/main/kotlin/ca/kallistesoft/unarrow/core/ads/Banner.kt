package ca.kallistesoft.unarrow.core.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * An anchored adaptive banner of [unitId], as wide as the space given. It has no height until an ad is loaded (offline
 * or no fill: nothing shows, no empty strip), and is destroyed when it leaves the screen. A new width (rotation) loads
 * a banner of the new size.
 */
@Composable
internal fun AnchoredBanner(unitId: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().testTag("banner")) {
        val context = LocalContext.current
        val width = maxWidth.value.toInt()
        val size = remember(width) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width) }
        key(size) {
            var loaded by remember { mutableStateOf(false) }
            AndroidView(
                factory = {
                    AdView(it).apply {
                        setAdSize(size)
                        adUnitId = unitId
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                loaded = true
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(if (loaded) size.height.dp else Dp.Hairline),
                onRelease = AdView::destroy,
            )
        }
    }
}
