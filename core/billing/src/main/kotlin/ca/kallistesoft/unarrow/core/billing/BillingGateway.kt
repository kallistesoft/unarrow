package ca.kallistesoft.unarrow.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/** A product as Play describes it; [details] is the SDK's object, handed back to launch the purchase. */
internal data class StoreProduct(val id: String, val price: String, val details: Any)

/** A purchase as Play reports it. */
internal data class OwnedPurchase(
    val productIds: List<String>,
    val token: String,
    val purchased: Boolean,
    val pending: Boolean,
    val acknowledged: Boolean,
)

/** The end of a purchase flow (Play's PurchasesUpdatedListener). */
internal data class PurchaseUpdate(val result: GatewayResult, val purchases: List<OwnedPurchase>)

internal enum class GatewayResult { OK, CANCELED, ALREADY_OWNED, ERROR }

/** Play Billing, as [BillingRepository] needs it. Every call returns null or false when Play can't be reached. */
internal interface BillingGateway {
    /** Results of purchase flows, including pending purchases completing later. */
    val updates: Flow<PurchaseUpdate>

    suspend fun connect(): Boolean

    suspend fun queryProducts(ids: List<String>): List<StoreProduct>?

    suspend fun queryPurchases(): List<OwnedPurchase>?

    /** Opens the purchase sheet; the outcome arrives on [updates] unless this returns something other than OK. */
    fun launch(activity: Activity, product: StoreProduct): GatewayResult

    suspend fun acknowledge(token: String): Boolean

    suspend fun consume(token: String): Boolean
}
