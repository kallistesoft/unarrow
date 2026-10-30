package ca.kallistesoft.unarrow.core.billing

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Play Billing Library 9. [createClient] builds the client around the purchases listener (tests pass a mock); the
 * client reconnects to Play by itself after a disconnection.
 */
internal class PlayBillingGateway(createClient: (PurchasesUpdatedListener) -> BillingClient) : BillingGateway {
    private val updateFlow = MutableSharedFlow<PurchaseUpdate>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val updates: Flow<PurchaseUpdate> = updateFlow

    private val client = createClient { result, purchases ->
        updateFlow.tryEmit(PurchaseUpdate(result.toGatewayResult(), purchases.orEmpty().map { it.toOwned() }))
    }

    override suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { done ->
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (done.isActive) done.resume(result.responseCode == BillingResponseCode.OK)
                    }

                    override fun onBillingServiceDisconnected() {
                        if (done.isActive) done.resume(false)
                    }
                },
            )
        }
    }

    override suspend fun queryProducts(ids: List<String>): List<StoreProduct>? = suspendCancellableCoroutine { done ->
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ids.map {
                    QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(ProductType.INAPP)
                        .build()
                },
            )
            .build()
        client.queryProductDetailsAsync(params) { result, found ->
            val products = if (result.responseCode == BillingResponseCode.OK) {
                found.productDetailsList.mapNotNull { it.toStoreProduct() }
            } else {
                null
            }
            done.resume(products)
        }
    }

    override suspend fun queryPurchases(): List<OwnedPurchase>? = suspendCancellableCoroutine { done ->
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            done.resume(if (result.responseCode == BillingResponseCode.OK) purchases.map { it.toOwned() } else null)
        }
    }

    override fun launch(activity: Activity, product: StoreProduct): GatewayResult {
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product.details as ProductDetails)
                        .build(),
                ),
            )
            .build()
        return client.launchBillingFlow(activity, params).toGatewayResult()
    }

    override suspend fun acknowledge(token: String): Boolean = suspendCancellableCoroutine { done ->
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()) {
            done.resume(it.responseCode == BillingResponseCode.OK)
        }
    }

    override suspend fun consume(token: String): Boolean = suspendCancellableCoroutine { done ->
        client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(token).build()) { result, _ ->
            done.resume(result.responseCode == BillingResponseCode.OK)
        }
    }
}

internal fun BillingResult.toGatewayResult(): GatewayResult = when (responseCode) {
    BillingResponseCode.OK -> GatewayResult.OK
    BillingResponseCode.USER_CANCELED -> GatewayResult.CANCELED
    BillingResponseCode.ITEM_ALREADY_OWNED -> GatewayResult.ALREADY_OWNED
    else -> GatewayResult.ERROR
}

internal fun Purchase.toOwned() = OwnedPurchase(
    productIds = products,
    token = purchaseToken,
    purchased = purchaseState == Purchase.PurchaseState.PURCHASED,
    pending = purchaseState == Purchase.PurchaseState.PENDING,
    acknowledged = isAcknowledged,
)

private fun ProductDetails.toStoreProduct(): StoreProduct? =
    oneTimePurchaseOfferDetails?.let { StoreProduct(productId, it.formattedPrice, this) }
