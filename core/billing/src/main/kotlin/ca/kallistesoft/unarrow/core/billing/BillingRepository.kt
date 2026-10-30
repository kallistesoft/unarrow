package ca.kallistesoft.unarrow.core.billing

import android.app.Activity
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The purchase rules on top of Play ([gateway]):
 * - a purchase is granted once Play says it's paid, never while pending; consumables are granted only when their
 *   consumption succeeds, so a hint pack is never counted twice;
 * - "Remove ads" is acknowledged (or Play refunds it after 3 days) and restored on [refresh]; when Play no longer
 *   lists it (refund), ads come back.
 *
 * Every purchase update goes through one collector started in [scope], including pending purchases that complete
 * while the app is open; [buy] waits for the one that ends its flow.
 */
internal class BillingRepository(
    private val gateway: BillingGateway,
    private val progress: ProgressRepository,
    private val currentActivity: () -> Activity?,
    scope: CoroutineScope,
) : Purchases {
    private val priceState = MutableStateFlow<Map<Product, String>>(emptyMap())
    override val prices: StateFlow<Map<Product, String>> = priceState.asStateFlow()

    private var products: Map<Product, StoreProduct> = emptyMap()
    private val outcomes = MutableSharedFlow<PurchaseOutcome>(extraBufferCapacity = 1)
    private val processing = Mutex()

    init {
        scope.launch {
            gateway.updates.collect { update -> outcomes.emit(handle(update)) }
        }
    }

    override suspend fun refresh() {
        if (!gateway.connect()) return
        loadProducts()
        val owned = gateway.queryPurchases() ?: return
        processing.withLock { owned.forEach { grant(it) } }
        val removeAds = owned.any { it.purchased && Product.REMOVE_ADS.id in it.productIds }
        progress.setRemoveAdsOwned(removeAds)
    }

    override suspend fun buy(product: Product): PurchaseOutcome {
        if (!gateway.connect()) return PurchaseOutcome.UNAVAILABLE
        if (product !in products) loadProducts()
        val details = products[product] ?: return PurchaseOutcome.UNAVAILABLE
        val activity = currentActivity() ?: return PurchaseOutcome.UNAVAILABLE
        return coroutineScope {
            // Listening before the sheet opens, so its result can't be missed.
            val next = async(start = CoroutineStart.UNDISPATCHED) { outcomes.first() }
            when (gateway.launch(activity, details)) {
                GatewayResult.OK -> next.await()
                GatewayResult.CANCELED -> PurchaseOutcome.CANCELED.also { next.cancel() }
                GatewayResult.ALREADY_OWNED -> {
                    next.cancel()
                    refresh()
                    PurchaseOutcome.ALREADY_OWNED
                }
                GatewayResult.ERROR -> PurchaseOutcome.UNAVAILABLE.also { next.cancel() }
            }
        }
    }

    private suspend fun loadProducts() {
        val found = gateway.queryProducts(Product.entries.map { it.id }) ?: return
        products = found.mapNotNull { p -> Product.byId(p.id)?.let { it to p } }.toMap()
        priceState.value = products.mapValues { it.value.price }
    }

    private suspend fun handle(update: PurchaseUpdate): PurchaseOutcome = when (update.result) {
        GatewayResult.OK -> processing.withLock {
            val results = update.purchases.map { grant(it) }
            when {
                PurchaseOutcome.PURCHASED in results -> PurchaseOutcome.PURCHASED
                PurchaseOutcome.PENDING in results -> PurchaseOutcome.PENDING
                else -> PurchaseOutcome.UNAVAILABLE
            }
        }
        GatewayResult.CANCELED -> PurchaseOutcome.CANCELED
        GatewayResult.ALREADY_OWNED -> {
            refresh()
            PurchaseOutcome.ALREADY_OWNED
        }
        GatewayResult.ERROR -> PurchaseOutcome.UNAVAILABLE
    }

    /** Grants [purchase] if it's paid: consumes a hint pack (then adds hints), acknowledges "Remove ads". */
    private suspend fun grant(purchase: OwnedPurchase): PurchaseOutcome {
        if (purchase.pending) return PurchaseOutcome.PENDING
        if (!purchase.purchased) return PurchaseOutcome.UNAVAILABLE
        for (product in purchase.productIds.mapNotNull(Product::byId)) {
            if (product.consumable) {
                if (!gateway.consume(purchase.token)) return PurchaseOutcome.UNAVAILABLE
                progress.addHints(HINTS_PER_PACK)
            } else {
                if (!purchase.acknowledged && !gateway.acknowledge(purchase.token)) return PurchaseOutcome.UNAVAILABLE
                progress.setRemoveAdsOwned(true)
            }
        }
        return PurchaseOutcome.PURCHASED
    }
}
