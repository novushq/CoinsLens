package app.novushq.coinlens.feature.paywall

import android.app.Activity
import android.content.Context
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.AppResult
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.data.LocalEntitlementRepository
import app.novushq.coinlens.domain.EntitlementRepository
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.Period
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlin.coroutines.resume

enum class PlanType { WEEKLY, ANNUAL, LIFETIME }

data class BillingPlan(
    val type: PlanType,
    val localizedPrice: String? = null,
    val trialDays: Int? = null,
)

interface BillingGateway {
    val isDemo: Boolean
    suspend fun loadPlans(): AppResult<List<BillingPlan>>
    /** true = entitlement active, false = user cancelled checkout. */
    suspend fun purchase(activity: Activity, plan: PlanType): AppResult<Boolean>
}

class FakeBillingGateway(private val entitlements: LocalEntitlementRepository) : BillingGateway {
    override val isDemo = true

    override suspend fun loadPlans() = AppResult.Success(
        listOf(
            BillingPlan(PlanType.WEEKLY),
            BillingPlan(PlanType.ANNUAL, trialDays = 3),
            BillingPlan(PlanType.LIFETIME),
        ),
    )

    override suspend fun purchase(activity: Activity, plan: PlanType): AppResult<Boolean> {
        entitlements.setPro(true)
        return AppResult.Success(true)
    }
}

class RevenueCatSdk(context: Context, apiKey: String) {
    val purchases: Purchases

    init {
        if (!Purchases.isConfigured) {
            purchases = Purchases.configure(PurchasesConfiguration.Builder(context.applicationContext, apiKey).build())
        } else {
            purchases = Purchases.sharedInstance
        }
    }
}

class RevenueCatEntitlementRepository(sdk: RevenueCatSdk) : EntitlementRepository {
    private val purchases = sdk.purchases
    private val active = kotlinx.coroutines.flow.MutableStateFlow(false)
    override val isPro = active.asStateFlow()

    init {
        purchases.updatedCustomerInfoListener = UpdatedCustomerInfoListener { info -> active.value = info.entitlements.active.containsKey(PRO_ENTITLEMENT) }
        purchases.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo) {
                active.value = customerInfo.entitlements.active.containsKey(PRO_ENTITLEMENT)
            }
            override fun onError(error: com.revenuecat.purchases.PurchasesError) = Unit
        })
    }

    override suspend fun refresh(): AppResult<Boolean> = customerInfo(purchases::getCustomerInfo)

    override suspend fun restore(): AppResult<Boolean> = customerInfo(purchases::restorePurchases)

    private suspend fun customerInfo(request: (ReceiveCustomerInfoCallback) -> Unit): AppResult<Boolean> =
        suspendCancellableCoroutine { continuation ->
            request(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: com.revenuecat.purchases.CustomerInfo) {
                    val isPro = customerInfo.entitlements.active.containsKey(PRO_ENTITLEMENT)
                    active.value = isPro
                    if (continuation.isActive) continuation.resume(AppResult.Success(isPro))
                }
                override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                    if (continuation.isActive) continuation.resume(AppResult.Failure(AppError.Network(error.message)))
                }
            })
        }

    private companion object { const val PRO_ENTITLEMENT = "pro" }
}

class RevenueCatBillingGateway(private val sdk: RevenueCatSdk) : BillingGateway {
    override val isDemo = false
    private val products = mutableMapOf<PlanType, Package>()

    override suspend fun loadPlans(): AppResult<List<BillingPlan>> = suspendCancellableCoroutine { continuation ->
        sdk.purchases.getOfferings(object : ReceiveOfferingsCallback {
            override fun onReceived(offerings: Offerings) {
                val current = offerings.current
                val entries = listOf(
                    PlanType.WEEKLY to current?.weekly,
                    PlanType.ANNUAL to current?.annual,
                    PlanType.LIFETIME to current?.lifetime,
                )
                val available = entries.mapNotNull { (type, product) ->
                    product?.also { products[type] = it }?.let { pkg ->
                        val trialPeriod = pkg.product.subscriptionOptions?.freeTrial?.freePhase?.billingPeriod
                        BillingPlan(
                            type = type,
                            localizedPrice = pkg.product.price.formatted,
                            trialDays = trialPeriod?.takeIf { it.unit == Period.Unit.DAY }?.value,
                        )
                    }
                }
                if (continuation.isActive) {
                    continuation.resume(if (available.isEmpty()) AppResult.Failure(AppError.NotFound("No store plans configured")) else AppResult.Success(available))
                }
            }
            override fun onError(error: com.revenuecat.purchases.PurchasesError) {
                if (continuation.isActive) continuation.resume(AppResult.Failure(AppError.Network(error.message)))
            }
        })
    }

    override suspend fun purchase(activity: Activity, plan: PlanType): AppResult<Boolean> =
        suspendCancellableCoroutine { continuation ->
            val product = products[plan]
            if (product == null) {
                continuation.resume(AppResult.Failure(AppError.NotFound("Store plan not available")))
                return@suspendCancellableCoroutine
            }
            sdk.purchases.purchase(PurchaseParams.Builder(activity, product).build(), object : PurchaseCallback {
                override fun onCompleted(
                    storeTransaction: com.revenuecat.purchases.models.StoreTransaction,
                    customerInfo: com.revenuecat.purchases.CustomerInfo,
                ) {
                    if (continuation.isActive) {
                        val hasPro = customerInfo.entitlements.active.containsKey("pro")
                        continuation.resume(
                            if (hasPro) AppResult.Success(true)
                            else AppResult.Failure(AppError.NotFound("Purchase completed without the pro entitlement")),
                        )
                    }
                }
                override fun onError(error: com.revenuecat.purchases.PurchasesError, userCancelled: Boolean) {
                    if (continuation.isActive) {
                        val result = if (userCancelled) AppResult.Success(false) else AppResult.Failure(AppError.Network(error.message))
                        continuation.resume(result)
                    }
                }
            })
        }
}

interface RewardedAdGateway {
    val isDemo: Boolean
    suspend fun show(activity: Activity): AppResult<Boolean>
}

class FakeRewardedAdGateway : RewardedAdGateway {
    override val isDemo = true
    override suspend fun show(activity: Activity): AppResult<Boolean> {
        delay(900)
        return AppResult.Success(true)
    }
}

class AdMobRewardedAdGateway(
    context: Context,
    private val adUnitId: String,
    private val dispatchers: DispatcherProvider,
) : RewardedAdGateway {
    private val appContext = context.applicationContext
    override val isDemo = false

    override suspend fun show(activity: Activity): AppResult<Boolean> {
        return try {
            withContext(dispatchers.io) { com.google.android.gms.ads.MobileAds.initialize(appContext) }
            val loaded = withTimeout(60_000) {
                suspendCancellableCoroutine<AppResult<com.google.android.gms.ads.rewarded.RewardedAd>> { continuation ->
                    com.google.android.gms.ads.rewarded.RewardedAd.load(
                        appContext,
                        adUnitId,
                        com.google.android.gms.ads.AdRequest.Builder().build(),
                        object : com.google.android.gms.ads.rewarded.RewardedAdLoadCallback() {
                            override fun onAdLoaded(ad: com.google.android.gms.ads.rewarded.RewardedAd) {
                                if (continuation.isActive) continuation.resume(AppResult.Success(ad))
                            }

                            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                                if (continuation.isActive) continuation.resume(AppResult.Failure(AppError.Network(error.message)))
                            }
                        },
                    )
                }
            }
            when (loaded) {
                is AppResult.Failure -> loaded
                is AppResult.Success -> suspendCancellableCoroutine<AppResult<Boolean>> { continuation ->
                    loaded.data.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            if (continuation.isActive) continuation.resume(AppResult.Success(false))
                        }

                        override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                            if (continuation.isActive) continuation.resume(AppResult.Failure(AppError.Network(error.message)))
                        }
                    }
                    loaded.data.show(activity) {
                        if (continuation.isActive) continuation.resume(AppResult.Success(true))
                    }
                }
            }
        } catch (_: TimeoutCancellationException) {
            AppResult.Failure(AppError.Timeout())
        }
    }
}
