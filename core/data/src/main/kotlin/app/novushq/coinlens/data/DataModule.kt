package app.novushq.coinlens.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import app.novushq.coinlens.data.db.CoinLensDatabase
import app.novushq.coinlens.domain.CollectionRepository
import app.novushq.coinlens.domain.EntitlementRepository
import app.novushq.coinlens.domain.ImageStore
import app.novushq.coinlens.domain.PreferencesRepository
import app.novushq.coinlens.domain.ScanRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

private val Context.coinLensPrefs: DataStore<Preferences> by preferencesDataStore(
    name = DataStorePreferencesRepository.FILE_NAME,
)

/**
 * Requires [DispatcherProvider][app.novushq.coinlens.common.DispatcherProvider] from the app module and
 * the domain module (for ComputeCollectionValueUseCase). Load feature modules after this one so
 * feature/paywall can override [EntitlementRepository].
 */
val dataModule = module {
    single { CoinLensDatabase.build(androidContext()) }
    single { get<CoinLensDatabase>().scanDao() }
    single { get<CoinLensDatabase>().folderDao() }
    single { get<CoinLensDatabase>().itemDao() }
    single<DataStore<Preferences>> { androidContext().coinLensPrefs }

    single<ImageStore> { FileImageStore(File(androidContext().filesDir, FileImageStore.DIR_NAME), get()) }
    single<PreferencesRepository> { DataStorePreferencesRepository(get()) }
    single<ScanRepository> { RoomScanRepository(get(), get(), get()) }
    single<CollectionRepository> { RoomCollectionRepository(get(), get(), get(), get()) }
    single { LocalEntitlementRepository() }
    single<EntitlementRepository> { get<LocalEntitlementRepository>() }
    single { ScanDraft() }
}
