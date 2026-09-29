package app.novushq.coinlens.feature.share

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.core.content.FileProvider
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.component.BackIconButton
import app.novushq.coinlens.designsystem.component.CoinFrame
import app.novushq.coinlens.designsystem.component.ConfidencePill
import app.novushq.coinlens.designsystem.component.DisclaimerText
import app.novushq.coinlens.designsystem.component.MetadataRow
import app.novushq.coinlens.designsystem.component.MoneyRangeText
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.component.SecondaryButton
import app.novushq.coinlens.designsystem.component.UiStateSurface
import app.novushq.coinlens.designsystem.component.ValueRangeBar
import app.novushq.coinlens.designsystem.theme.Sizes
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

val shareModule = module {
    viewModelOf(::ShareViewModel)
}

fun NavGraphBuilder.shareGraph(navigator: AppNavigator) {
    composable<Route.Share> {
        val viewModel: ShareViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        ShareContent(state = state, navigator = navigator)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareContent(state: UiState<ScanRecord>, navigator: AppNavigator) {
    val context = LocalContext.current
    val dispatchers: DispatcherProvider = koinInject()
    val scope = rememberCoroutineScope()
    var darkCard by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    val palette = MaterialTheme.colorScheme
    val chooserTitle = stringResource(R.string.share_chooser)
    val sharePalette = SharePalette(
        background = (if (darkCard) palette.inverseSurface else palette.surface).toArgb(),
        surface = (if (darkCard) palette.surfaceContainerHighest else palette.surface).toArgb(),
        primary = palette.primary.toArgb(),
        text = (if (darkCard) palette.inverseOnSurface else palette.onSurface).toArgb(),
        secondary = palette.onSurfaceVariant.toArgb(),
        outline = palette.outlineVariant.toArgb(),
    )

    fun export(record: ScanRecord, saveToPhotos: Boolean, chooserTitle: String) {
        scope.launch {
            busy = true
            message = null
            try {
                val output = withContext(dispatchers.io) {
                    val image = ShareCardRenderer.render(context, record, sharePalette)
                    if (saveToPhotos) {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, "coinlens-${record.id}.png")
                            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CoinLens")
                        }
                        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                            ?: throw IOException("Could not create photo entry")
                        val saved = context.contentResolver.openOutputStream(uri)?.use {
                            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                        } == true
                        image.recycle()
                        if (!saved) throw IOException("Could not write photo")
                        null
                    } else {
                        val file = File(context.cacheDir, "share-${record.id}.png")
                        FileOutputStream(file).use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                        image.recycle()
                        file
                    }
                }
                if (saveToPhotos) {
                    message = R.string.share_saved
                } else if (output != null) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", output)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, chooserTitle))
                }
            } catch (_: IOException) {
                message = R.string.share_error
            } catch (_: SecurityException) {
                message = R.string.share_error
            } finally {
                busy = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_title)) },
                navigationIcon = { BackIconButton(navigator::back) },
            )
        },
    ) { insets ->
        UiStateSurface(state = state, modifier = Modifier.padding(insets)) { record ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                item {
                    SharePreview(record = record, dark = darkCard)
                }
                item {
                    val options = listOf(
                        stringResource(R.string.share_light),
                        stringResource(R.string.share_dark),
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        options.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = darkCard == (index == 1),
                                onClick = { darkCard = index == 1 },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            ) { Text(label) }
                        }
                    }
                }
                item {
                    PrimaryButton(
                        text = stringResource(if (busy) R.string.share_working else R.string.share_action),
                        onClick = { export(record, false, chooserTitle) },
                        modifier = Modifier.fillMaxWidth(),
                        loading = busy,
                        icon = Icons.Outlined.Share,
                    )
                }
                item {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        SecondaryButton(
                            text = stringResource(R.string.share_save),
                            onClick = { export(record, true, chooserTitle) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                            icon = Icons.Outlined.Download,
                        )
                    }
                }
                message?.let { item { Text(stringResource(it), color = MaterialTheme.colorScheme.primary) } }
                item { DisclaimerText() }
            }
        }
    }
}

@Composable
private fun SharePreview(record: ScanRecord, dark: Boolean) {
    val coin = record.identification
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (dark) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            CoinFrame(record.obversePath, coin.name, size = Sizes.coinFrameMedium)
            Text(
                text = if (coin.recognized) coin.name else stringResource(R.string.share_unrecognized),
                style = MaterialTheme.typography.headlineMedium,
                color = if (dark) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
            )
            MetadataRow(listOf(coin.country, coin.year?.toString() ?: coin.yearText, coin.mintMark.orEmpty()))
            ValueRangeBar(coin.value.circulated, coin.value.uncirculated, confidence = coin.value.confidence)
            ConfidencePill(coin.value.confidence)
            (coin.value.circulated ?: coin.value.uncirculated)?.let { MoneyRangeText(it) }
            Text(
                text = stringResource(R.string.share_estimate_footer),
                style = MaterialTheme.typography.bodySmall,
                color = if (dark) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
