package app.novushq.coinlens.feature.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.data.ScanDraft
import app.novushq.coinlens.designsystem.component.CoinFrame
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.theme.Sizes
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.domain.ObserveScanAllowanceUseCase
import app.novushq.coinlens.model.ScanAllowance
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.PaywallSource
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.dsl.module
import java.io.File
import java.io.IOException

val captureModule = module {}

fun NavGraphBuilder.captureGraph(navigator: AppNavigator) {
    composable<Route.Capture> {
        CaptureScreen(navigator)
    }
}

@Composable
private fun CaptureScreen(navigator: AppNavigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val draft: ScanDraft = koinInject()
    val dispatchers: DispatcherProvider = koinInject()
    val observeAllowance: ObserveScanAllowanceUseCase = koinInject()
    val allowanceFlow = remember(observeAllowance) { observeAllowance() }
    val allowance by allowanceFlow.collectAsStateWithLifecycle(initialValue = ScanAllowance())
    val draftState by draft.state.collectAsStateWithLifecycle()
    var reverseSide by rememberSaveable { mutableStateOf(false) }
    var frontUri by rememberSaveable { mutableStateOf<String?>(null) }
    var reverseUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    fun savePhoto(uri: Uri) {
        scope.launch {
            busy = true
            failed = false
            try {
                val bytes = withContext(dispatchers.io) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes == null || bytes.isEmpty()) {
                    failed = true
                } else if (reverseSide) {
                    draft.setReverse(bytes)
                    reverseUri = uri.toString()
                } else {
                    draft.setObverse(bytes)
                    frontUri = uri.toString()
                }
            } catch (_: IOException) {
                failed = true
            } catch (_: SecurityException) {
                failed = true
            } finally {
                busy = false
            }
        }
    }

    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) savePhoto(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCameraUri
        if (saved && uri != null) savePhoto(uri)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.capture_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(if (reverseSide) R.string.capture_reverse_prompt else R.string.capture_front_prompt),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CoinFrame(
                obverse = if (reverseSide) reverseUri else frontUri,
                reverse = if (reverseSide) null else reverseUri,
                contentDescription = stringResource(if (reverseSide) R.string.capture_reverse_photo else R.string.capture_front_photo),
                size = Sizes.coinFrameLarge,
            )
            if (failed) {
                Text(
                    text = stringResource(R.string.capture_photo_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            PrimaryButton(
                text = stringResource(if (reverseSide) R.string.capture_choose_reverse else R.string.capture_choose_front),
                onClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth(),
                loading = busy,
            )
            OutlinedButton(
                onClick = {
                    val output = File.createTempFile("coinlens-", ".jpg", context.cacheDir)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        output,
                    )
                    pendingCameraUri = uri
                    camera.launch(uri)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            ) {
                Text(stringResource(R.string.capture_camera))
            }
            if (frontUri != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedButton(onClick = { reverseSide = !reverseSide }, enabled = !busy) {
                        Text(stringResource(if (reverseSide) R.string.capture_front_side else R.string.capture_add_reverse))
                    }
                }
            }
            PrimaryButton(
                text = stringResource(R.string.capture_identify),
                onClick = {
                    if (allowance.canScan) {
                        navigator.navigate(Route.Result())
                    } else {
                        navigator.navigate(Route.Paywall(PaywallSource.QUOTA))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = draftState.isReady && !busy,
            )
            Text(
                text = stringResource(R.string.capture_photo_guidance),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
