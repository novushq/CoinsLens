package app.novushq.coinlens

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.novushq.coinlens.ai.AiInfoProvider
import app.novushq.coinlens.ai.AiMode
import app.novushq.coinlens.ai.FakeIdentifyEngine
import app.novushq.coinlens.data.LocalEntitlementRepository
import app.novushq.coinlens.data.ScanDraft
import app.novushq.coinlens.di.appModules
import app.novushq.coinlens.domain.AddToCollectionUseCase
import app.novushq.coinlens.domain.CollectionRepository
import app.novushq.coinlens.domain.CompleteOnboardingUseCase
import app.novushq.coinlens.domain.ComputeCollectionValueUseCase
import app.novushq.coinlens.domain.EntitlementRepository
import app.novushq.coinlens.domain.GrantBonusScanUseCase
import app.novushq.coinlens.domain.IdentifyCoinUseCase
import app.novushq.coinlens.domain.ImageStore
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.domain.PreferencesRepository
import app.novushq.coinlens.domain.ScanRepository
import app.novushq.coinlens.identify.IdentifyEngine
import app.novushq.coinlens.navigation.AppNavigator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.verify.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class KoinGraphTest {

    @After
    fun tearDown() = stopKoin()

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `every module verifies in both ai modes`() {
        val extraTypes = listOf(
            Context::class,
            Application::class,
            SavedStateHandle::class,
            Function0::class,
            Boolean::class,
            Long::class,
            PreferencesRepository::class,
            AiMode::class,
            List::class,
            String::class,
            Int::class,
        )
        AiMode.entries.forEach { mode ->
            module { includes(appModules(mode, isDebug = true)) }.verify(extraTypes = extraTypes)
        }
    }

    @Test
    fun `fake graph resolves every shared contract`() {
        val koin = startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(appModules(AiMode.FAKE, isDebug = true))
        }.koin

        assertTrue(koin.get<IdentifyEngine>() is FakeIdentifyEngine)
        assertEquals(AiMode.FAKE, koin.get<AiInfoProvider>().current.mode)
        assertTrue(koin.get<EntitlementRepository>() === koin.get<LocalEntitlementRepository>())
        assertTrue(koin.get<ScanDraft>() === koin.get<ScanDraft>())
        assertTrue(koin.get<AppNavigator>() === koin.get<AppNavigator>())
        koin.get<MainViewModel>()
        koin.get<ScanRepository>()
        koin.get<CollectionRepository>()
        koin.get<PreferencesRepository>()
        koin.get<ImageStore>()
        koin.get<ObserveScanAllowanceUseCase>()
        koin.get<IdentifyCoinUseCase>()
        koin.get<GrantBonusScanUseCase>()
        koin.get<ComputeCollectionValueUseCase>()
        koin.get<AddToCollectionUseCase>()
        koin.get<CompleteOnboardingUseCase>()
    }

    @Test
    fun `ai mode parsing defaults to fake`() {
        assertEquals(AiMode.FIREBASE, aiModeOf("firebase"))
        assertEquals(AiMode.FAKE, aiModeOf("fake"))
        assertEquals(AiMode.FAKE, aiModeOf(""))
    }
}
