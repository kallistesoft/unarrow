package ca.kallistesoft.unarrow.core.billing

import android.app.Activity
import ca.kallistesoft.unarrow.core.data.GameProgress
import ca.kallistesoft.unarrow.core.testing.FakeProgressRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingRepositoryTest {
    private val activity = mockk<Activity>()
    private val gateway = FakeGateway()
    private val progress = FakeProgressRepository()
    private var onScreen: Activity? = activity

    private fun TestScope.repository() = BillingRepository(gateway, progress, { onScreen }, backgroundScope).also {
        runCurrent()
    }

    @Test
    fun `refresh loads the prices and restores remove ads`() = runTest {
        gateway.owned = listOf(purchase("remove_ads", acknowledged = true))
        val billing = repository()
        billing.refresh()
        assertEquals(mapOf(Product.REMOVE_ADS to "$3.99", Product.HINTS_10 to "$0.99"), billing.prices.value)
        assertTrue(progress.state.value.removeAdsOwned)
        assertTrue("already acknowledged", gateway.acknowledged.isEmpty())
    }

    @Test
    fun `a refunded remove ads brings the ads back`() = runTest {
        progress.state.value = GameProgress(removeAdsOwned = true)
        repository().refresh()
        assertFalse(progress.state.value.removeAdsOwned)
    }

    @Test
    fun `an unacknowledged purchase found on refresh is acknowledged, an unconsumed pack is granted`() = runTest {
        gateway.owned = listOf(purchase("remove_ads", token = "a"), purchase("hints_10", token = "h"))
        repository().refresh()
        assertEquals(listOf("a"), gateway.acknowledged)
        assertEquals(listOf("h"), gateway.consumed)
        assertEquals(GameProgress.STARTING_HINTS + HINTS_PER_PACK, progress.state.value.hints)
    }

    @Test
    fun `nothing changes when Play can't be reached`() = runTest {
        gateway.connects = false
        progress.state.value = GameProgress(removeAdsOwned = true)
        val billing = repository()
        billing.refresh()
        assertTrue(progress.state.value.removeAdsOwned)
        assertEquals(PurchaseOutcome.UNAVAILABLE, billing.buy(Product.REMOVE_ADS))

        gateway.connects = true
        gateway.purchasesQueryFails = true
        billing.refresh()
        assertTrue("kept when the query fails", progress.state.value.removeAdsOwned)
    }

    @Test
    fun `buying remove ads acknowledges it and turns ads off`() = runTest {
        val billing = repository()
        val outcome = async { billing.buy(Product.REMOVE_ADS) }
        runCurrent()
        assertEquals(listOf("remove_ads"), gateway.launched)
        gateway.finish(GatewayResult.OK, purchase("remove_ads", token = "t"))
        assertEquals(PurchaseOutcome.PURCHASED, outcome.await())
        assertEquals(listOf("t"), gateway.acknowledged)
        assertTrue(progress.state.value.removeAdsOwned)
    }

    @Test
    fun `a hint pack is consumed then granted, once`() = runTest {
        val billing = repository()
        val outcome = async { billing.buy(Product.HINTS_10) }
        runCurrent()
        gateway.finish(GatewayResult.OK, purchase("hints_10", token = "p"))
        assertEquals(PurchaseOutcome.PURCHASED, outcome.await())
        assertEquals(GameProgress.STARTING_HINTS + HINTS_PER_PACK, progress.state.value.hints)

        gateway.consumeWorks = false // Play reports it again but it's already consumed: nothing more is added.
        gateway.finish(GatewayResult.OK, purchase("hints_10", token = "p"))
        runCurrent()
        assertEquals(GameProgress.STARTING_HINTS + HINTS_PER_PACK, progress.state.value.hints)
    }

    @Test
    fun `a pending payment grants nothing until Play confirms it`() = runTest {
        val billing = repository()
        val outcome = async { billing.buy(Product.REMOVE_ADS) }
        runCurrent()
        gateway.finish(GatewayResult.OK, purchase("remove_ads", pending = true))
        assertEquals(PurchaseOutcome.PENDING, outcome.await())
        assertFalse(progress.state.value.removeAdsOwned)

        gateway.finish(GatewayResult.OK, purchase("remove_ads")) // paid later, app still open
        runCurrent()
        assertTrue(progress.state.value.removeAdsOwned)
    }

    @Test
    fun `canceling the sheet or an error grants nothing`() = runTest {
        val billing = repository()
        val canceled = async { billing.buy(Product.HINTS_10) }
        runCurrent()
        gateway.finish(GatewayResult.CANCELED)
        assertEquals(PurchaseOutcome.CANCELED, canceled.await())

        val failed = async { billing.buy(Product.HINTS_10) }
        runCurrent()
        gateway.finish(GatewayResult.ERROR)
        assertEquals(PurchaseOutcome.UNAVAILABLE, failed.await())

        val unknown = async { billing.buy(Product.HINTS_10) }
        runCurrent()
        gateway.finish(GatewayResult.OK, purchase("something_else", purchased = false))
        assertEquals(PurchaseOutcome.UNAVAILABLE, unknown.await())
        assertEquals(GameProgress.STARTING_HINTS, progress.state.value.hints)
    }

    @Test
    fun `the sheet can fail to open`() = runTest {
        val billing = repository()
        gateway.launchResult = GatewayResult.CANCELED
        assertEquals(PurchaseOutcome.CANCELED, billing.buy(Product.REMOVE_ADS))
        gateway.launchResult = GatewayResult.ERROR
        assertEquals(PurchaseOutcome.UNAVAILABLE, billing.buy(Product.REMOVE_ADS))
    }

    @Test
    fun `remove ads already owned is restored`() = runTest {
        val billing = repository()
        gateway.owned = listOf(purchase("remove_ads", acknowledged = true))
        gateway.launchResult = GatewayResult.ALREADY_OWNED
        assertEquals(PurchaseOutcome.ALREADY_OWNED, billing.buy(Product.REMOVE_ADS))
        assertTrue(progress.state.value.removeAdsOwned)

        progress.state.value = GameProgress()
        gateway.launchResult = GatewayResult.OK
        val outcome = async { billing.buy(Product.REMOVE_ADS) }
        runCurrent()
        gateway.finish(GatewayResult.ALREADY_OWNED)
        assertEquals(PurchaseOutcome.ALREADY_OWNED, outcome.await())
        assertTrue(progress.state.value.removeAdsOwned)
    }

    @Test
    fun `no purchase without the product in Play Console or without a screen`() = runTest {
        gateway.products = listOf(StoreProduct("hints_10", "$0.99", Unit))
        val billing = repository()
        assertEquals(PurchaseOutcome.UNAVAILABLE, billing.buy(Product.REMOVE_ADS))
        onScreen = null
        assertEquals(PurchaseOutcome.UNAVAILABLE, billing.buy(Product.HINTS_10))
        gateway.productsQueryFails = true
        billing.refresh()
        assertEquals(mapOf(Product.HINTS_10 to "$0.99"), billing.prices.value)
        assertTrue(gateway.launched.isEmpty())
    }

    @Test
    fun `a failed acknowledgement leaves remove ads to the next refresh`() = runTest {
        gateway.acknowledgeWorks = false
        val billing = repository()
        val outcome = async { billing.buy(Product.REMOVE_ADS) }
        runCurrent()
        gateway.finish(GatewayResult.OK, purchase("remove_ads"))
        assertEquals(PurchaseOutcome.UNAVAILABLE, outcome.await())
        assertFalse(progress.state.value.removeAdsOwned)
    }

    @Test
    fun `products are known by their Play ids`() {
        assertEquals(Product.HINTS_10, Product.byId("hints_10"))
        assertEquals(null, Product.byId("nope"))
    }

    private fun purchase(
        id: String,
        token: String = "token-$id",
        purchased: Boolean = true,
        pending: Boolean = false,
        acknowledged: Boolean = false,
    ) = OwnedPurchase(listOf(id), token, purchased && !pending, pending, acknowledged)

    private class FakeGateway : BillingGateway {
        var connects = true
        var products = listOf(StoreProduct("remove_ads", "$3.99", Unit), StoreProduct("hints_10", "$0.99", Unit))
        var productsQueryFails = false
        var owned = emptyList<OwnedPurchase>()
        var purchasesQueryFails = false
        var launchResult = GatewayResult.OK
        var acknowledgeWorks = true
        var consumeWorks = true
        val launched = mutableListOf<String>()
        val acknowledged = mutableListOf<String>()
        val consumed = mutableListOf<String>()
        override val updates = MutableSharedFlow<PurchaseUpdate>(extraBufferCapacity = 8)

        fun finish(result: GatewayResult, vararg purchases: OwnedPurchase) {
            updates.tryEmit(PurchaseUpdate(result, purchases.toList()))
        }

        override suspend fun connect() = connects

        override suspend fun queryProducts(ids: List<String>) = if (productsQueryFails) null else products

        override suspend fun queryPurchases() = if (purchasesQueryFails) null else owned

        override fun launch(activity: Activity, product: StoreProduct): GatewayResult {
            if (launchResult == GatewayResult.OK) launched += product.id
            return launchResult
        }

        override suspend fun acknowledge(token: String): Boolean {
            if (acknowledgeWorks) acknowledged += token
            return acknowledgeWorks
        }

        override suspend fun consume(token: String): Boolean {
            if (consumeWorks) consumed += token
            return consumeWorks
        }
    }
}
