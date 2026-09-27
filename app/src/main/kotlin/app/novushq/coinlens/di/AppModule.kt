package app.novushq.coinlens.di

import app.novushq.coinlens.common.DefaultDispatcherProvider
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.common.Logger
import app.novushq.coinlens.platform.AndroidLogger
import org.koin.dsl.module

/**
 * Root Koin graph. Feature modules contribute their own `module { }` and are
 * appended here - never by mutating another feature's module.
 */
val coreModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<Logger> { AndroidLogger() }
}

val appModule = listOf(coreModule)
