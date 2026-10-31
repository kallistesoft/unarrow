package ca.kallistesoft.unarrow.core.billing.di

import android.content.Context
import ca.kallistesoft.unarrow.core.billing.BillingRepository
import ca.kallistesoft.unarrow.core.billing.PlayBillingGateway
import ca.kallistesoft.unarrow.core.billing.Purchases
import ca.kallistesoft.unarrow.core.common.CurrentActivity
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BillingModule {
    @Provides
    @Singleton
    fun purchases(
        @ApplicationContext context: Context,
        progress: ProgressRepository,
        currentActivity: CurrentActivity,
    ): Purchases = BillingRepository(
        gateway = PlayBillingGateway { listener ->
            BillingClient.newBuilder(context)
                .setListener(listener)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build()
        },
        progress = progress,
        currentActivity = currentActivity::get,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    )
}
