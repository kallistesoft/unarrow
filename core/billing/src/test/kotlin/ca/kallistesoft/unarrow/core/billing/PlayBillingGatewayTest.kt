package ca.kallistesoft.unarrow.core.billing

import android.app.Activity
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The SDK adapter: each Play Billing callback ends the matching suspend call. The client is mocked. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK])
class PlayBillingGatewayTest {
    private val client = mockk<BillingClient>(relaxed = true)
    private val listener = slot<PurchasesUpdatedListener>()
    private val gateway = PlayBillingGateway { listener.captured = it; client }

    private fun result(code: Int) = BillingResult.newBuilder().setResponseCode(code).build()

    private fun purchase(state: Int, acknowledged: Boolean = false) = mockk<Purchase> {
        every { products } returns listOf("remove_ads")
        every { purchaseToken } returns "token"
        every { purchaseState } returns state
        every { isAcknowledged } returns acknowledged
    }

    @Test
    fun `connecting ends with the setup result, or at once when already connected`() = runTest {
        val state = slot<BillingClientStateListener>()
        every { client.isReady } returns false
        every { client.startConnection(capture(state)) } returns Unit

        val ok = async { gateway.connect() }
        runCurrent()
        state.captured.onBillingSetupFinished(result(BillingResponseCode.OK))
        assertTrue(ok.await())

        val unavailable = async { gateway.connect() }
        runCurrent()
        state.captured.onBillingSetupFinished(result(BillingResponseCode.BILLING_UNAVAILABLE))
        assertFalse(unavailable.await())

        val lost = async { gateway.connect() }
        runCurrent()
        state.captured.onBillingServiceDisconnected()
        state.captured.onBillingSetupFinished(result(BillingResponseCode.OK)) // late: ignored
        assertFalse(lost.await())

        every { client.isReady } returns true
        assertTrue(gateway.connect())
    }

    @Test
    fun `products come with their price, and none on an error`() = runTest {
        val callback = slot<ProductDetailsResponseListener>()
        every { client.queryProductDetailsAsync(any(), capture(callback)) } returns Unit
        val offer = mockk<ProductDetails.OneTimePurchaseOfferDetails> { every { formattedPrice } returns "$3.99" }
        val details = mockk<ProductDetails> {
            every { productId } returns "remove_ads"
            every { oneTimePurchaseOfferDetails } returns offer
        }
        val noOffer = mockk<ProductDetails> { every { oneTimePurchaseOfferDetails } returns null }
        val found = mockk<QueryProductDetailsResult> { every { productDetailsList } returns listOf(details, noOffer) }

        val ok = async { gateway.queryProducts(listOf("remove_ads", "hints_10")) }
        runCurrent()
        callback.captured.onProductDetailsResponse(result(BillingResponseCode.OK), found)
        assertEquals(listOf(StoreProduct("remove_ads", "$3.99", details)), ok.await())

        val failed = async { gateway.queryProducts(listOf("remove_ads")) }
        runCurrent()
        callback.captured.onProductDetailsResponse(result(BillingResponseCode.SERVICE_UNAVAILABLE), found)
        assertNull(failed.await())
    }

    @Test
    fun `owned purchases are read, and none on an error`() = runTest {
        val callback = slot<PurchasesResponseListener>()
        every { client.queryPurchasesAsync(any<com.android.billingclient.api.QueryPurchasesParams>(), capture(callback)) } returns Unit

        val ok = async { gateway.queryPurchases() }
        runCurrent()
        callback.captured.onQueryPurchasesResponse(
            result(BillingResponseCode.OK),
            listOf(purchase(Purchase.PurchaseState.PURCHASED, acknowledged = true)),
        )
        assertEquals(listOf(OwnedPurchase(listOf("remove_ads"), "token", true, false, true)), ok.await())

        val failed = async { gateway.queryPurchases() }
        runCurrent()
        callback.captured.onQueryPurchasesResponse(result(BillingResponseCode.ERROR), emptyList())
        assertNull(failed.await())
    }

    @Test
    fun `the purchase sheet opens and its end arrives as an update`() = runTest {
        val activity = mockk<Activity>()
        val details = mockk<ProductDetails>(relaxed = true)
        every { client.launchBillingFlow(activity, any()) } returns result(BillingResponseCode.OK)
        assertEquals(GatewayResult.OK, gateway.launch(activity, StoreProduct("remove_ads", "$3.99", details)))

        val update = async { gateway.updates.first() }
        runCurrent()
        listener.captured.onPurchasesUpdated(
            result(BillingResponseCode.OK),
            listOf(purchase(Purchase.PurchaseState.PENDING)),
        )
        assertEquals(
            PurchaseUpdate(GatewayResult.OK, listOf(OwnedPurchase(listOf("remove_ads"), "token", false, true, false))),
            update.await(),
        )

        val canceled = async { gateway.updates.first() }
        runCurrent()
        listener.captured.onPurchasesUpdated(result(BillingResponseCode.USER_CANCELED), null)
        assertEquals(PurchaseUpdate(GatewayResult.CANCELED, emptyList()), canceled.await())
    }

    @Test
    fun `acknowledging and consuming report success`() = runTest {
        val ack = slot<AcknowledgePurchaseResponseListener>()
        every { client.acknowledgePurchase(any(), capture(ack)) } returns Unit
        val consume = slot<ConsumeResponseListener>()
        every { client.consumeAsync(any(), capture(consume)) } returns Unit

        val acknowledged = async { gateway.acknowledge("token") }
        runCurrent()
        ack.captured.onAcknowledgePurchaseResponse(result(BillingResponseCode.OK))
        assertTrue(acknowledged.await())

        val consumed = async { gateway.consume("token") }
        runCurrent()
        consume.captured.onConsumeResponse(result(BillingResponseCode.ITEM_NOT_OWNED), "token")
        assertFalse(consumed.await())
    }

    @Test
    fun `response codes map to results`() {
        assertEquals(GatewayResult.OK, result(BillingResponseCode.OK).toGatewayResult())
        assertEquals(GatewayResult.CANCELED, result(BillingResponseCode.USER_CANCELED).toGatewayResult())
        assertEquals(GatewayResult.ALREADY_OWNED, result(BillingResponseCode.ITEM_ALREADY_OWNED).toGatewayResult())
        assertEquals(GatewayResult.ERROR, result(BillingResponseCode.DEVELOPER_ERROR).toGatewayResult())
    }
}
