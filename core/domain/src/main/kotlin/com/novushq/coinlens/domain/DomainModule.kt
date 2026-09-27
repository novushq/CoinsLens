package com.novushq.coinlens.domain

import org.koin.dsl.module

/** Pure-Kotlin bindings. `:core:data` and `:core:ai` supply repositories and the engine. */
val domainModule = module {
    single { CoinIdentificationSpec() }
    factory { ObserveScanAllowanceUseCase(get(), get()) }
    factory { IdentifyCoinUseCase(get(), get(), get(), get(), get(), get()) }
    factory { GrantBonusScanUseCase(get()) }
    factory { ComputeCollectionValueUseCase() }
    factory { AddToCollectionUseCase(get()) }
    factory { CompleteOnboardingUseCase(get()) }
}
