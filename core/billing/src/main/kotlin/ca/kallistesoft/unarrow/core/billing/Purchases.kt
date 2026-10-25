package ca.kallistesoft.unarrow.core.billing

import kotlinx.coroutines.flow.StateFlow

/** What the game sells, as set up in Play Console (one-time products). */
enum class Product(val id: String, val consumable: Boolean) {
    /** No more interstitials. Bought once, kept forever (restored on every device). */
    REMOVE_ADS("remove_ads", consumable = false),

    /** [HINTS_PER_PACK] more hints; can be bought again. */
    HINTS_10("hints_10", consumable = true),
    ;

    companion object {
        fun byId(id: String): Product? = entries.firstOrNull { it.id == id }
    }
}

const val HINTS_PER_PACK = 10

enum class PurchaseOutcome {
    /** Paid and granted. */
    PURCHASED,

    /** Paid later (cash, bank transfer): granted once Play confirms it, even on a later launch. */
    PENDING,
    CANCELED,

    /** "Remove ads" was already bought: it's restored instead. */
    ALREADY_OWNED,

    /** Play Billing can't be reached, the product isn't set up in Play Console yet, or no screen to show it on. */
    UNAVAILABLE,
}

/**
 * Purchases with Google Play. What's bought is granted in the game's progress: "Remove ads" sets
 * `removeAdsOwned`, a hint pack adds hints. Tests use a fake (`FakePurchases` in `:core:testing`).
 */
interface Purchases {
    /** Local prices ("$3.99") of the products Play knows; a product missing from it can't be bought now. */
    val prices: StateFlow<Map<Product, String>>

    /** Connects to Play, loads the prices and restores what's owned (a refunded "Remove ads" is taken back). */
    suspend fun refresh()

    /** Shows Play's purchase sheet over the current screen and returns once it's closed. */
    suspend fun buy(product: Product): PurchaseOutcome
}
