package com.novushq.coinlens.di

import com.novushq.coinlens.common.DefaultDispatcherProvider
import com.novushq.coinlens.common.DispatcherProvider
import com.novushq.coinlens.common.Logger
import com.novushq.coinlens.platform.AndroidLogger
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
