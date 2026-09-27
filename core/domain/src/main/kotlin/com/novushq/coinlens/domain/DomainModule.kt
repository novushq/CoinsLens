package com.novushq.coinlens.domain

import org.koin.dsl.module

/** Pure-Kotlin bindings. The data layer supplies the repository implementations. */
val domainModule = module {
    factory { ObserveScanAllowanceUseCase(get(), get()) }
    factory { IdentifyCoinUseCase(get(), get(), get(), get(), get(), get()) }
    factory { GrantBonusScanUseCase(get()) }
    factory { ComputeCollectionValueUseCase() }
    factory { AddToCollectionUseCase(get()) }
    factory { CompleteOnboardingUseCase(get()) }
    factory { CoinIdentificationSpec() }
}
