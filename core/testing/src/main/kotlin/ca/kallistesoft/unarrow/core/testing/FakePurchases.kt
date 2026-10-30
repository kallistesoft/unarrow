package ca.kallistesoft.unarrow.core.testing

import ca.kallistesoft.unarrow.core.billing.Product
import ca.kallistesoft.unarrow.core.billing.PurchaseOutcome
import ca.kallistesoft.unarrow.core.billing.Purchases
import kotlinx.coroutines.flow.MutableStateFlow

/** Every product priced; [buy] answers [outcome] and records what was bought. */
class FakePurchases(var outcome: PurchaseOutcome = PurchaseOutcome.PURCHASED) : Purchases {
    override val prices = MutableStateFlow(mapOf(Product.REMOVE_ADS to "$3.99", Product.HINTS_10 to "$0.99"))
    val bought = mutableListOf<Product>()
    var refreshes = 0

    override suspend fun refresh() {
        refreshes++
    }

    override suspend fun buy(product: Product): PurchaseOutcome {
        bought += product
        return outcome
    }
}
