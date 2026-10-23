package app.novushq.coinlens.feature.capture

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.novushq.coinlens.common.DispatcherProvider
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.component.CoinFrame
import app.novushq.coinlens.designsystem.component.EmptyState
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.component.SecondaryButton
import app.novushq.coinlens.designsystem.component.ShutterButton
import app.novushq.coinlens.designsystem.component.TertiaryButton
import app.novushq.coinlens.designsystem.component.UiStateSurface
import app.novushq.coinlens.designsystem.theme.CoinLensTheme
import app.novushq.coinlens.designsystem.theme.Radii
import app.novushq.coinlens.designsystem.theme.Sizes
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.designsystem.theme.ThemePreviews
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.PaywallSource
import app.novushq.coinlens.navigation.Route
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File
import java.util.concurrent.atomic.AtomicLong

@Composable
fun CaptureRoute(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CaptureEvent.NavigatePaywall ->
                    navigator.navigate(Route.Paywall(PaywallSource.QUOTA))
                CaptureEvent.NavigateResult ->
                    navigator.navigate(Route.Result(scanId = null))
            }
        }
    }
    UiStateSurface(
        state = state,
        modifier = modifier,
        onRetry = viewModel::retry,
        empty = { message ->
            EmptyState(
                message = message,
                action = {
                    SecondaryButton(
                        text = stringResource(R.string.capture_view_plans),
                        onClick = { navigator.navigate(Route.Paywall(PaywallSource.QUOTA)) },
                    )
                },
            )
        },
    ) { content ->
        if (content.quotaExhausted) {
            EmptyState(
                message = stringResource(R.string.capture_quota_empty),
                action = {
                    SecondaryButton(
                        text = stringResource(R.string.capture_view_plans),
                        onClick = { navigator.navigate(Route.Paywall(PaywallSource.QUOTA)) },
                    )
                },
            )
        } else {
            CaptureScreen(
                content = content,
                onPhotoSaved = viewModel::onPhotoSaved,
                onPhotoError = viewModel::onPhotoError,
                onQuality = viewModel::onQuality,
                onRetake = viewModel::onRetake,
                onSwitchSide = viewModel::onSwitchSide,
                onSkipBack = viewModel::onSkipBack,
                onDone = viewModel::onDone,
            )
        }
    }
}

@Composable
private fun CaptureScreen(
    content: CaptureContent,
    onPhotoSaved: (ByteArray) -> Unit,
    onPhotoError: () -> Unit,
    onQuality: (Boolean, Boolean) -> Unit,
    onRetake: () -> Unit,
    onSwitchSide: () -> Unit,
    onSkipBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dispatchers: DispatcherProvider = koinInject()
    var busy by rememberSaveable { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val activity = context as? Activity
    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) {
            showRationale = false
            permanentlyDenied = false
        } else {
            val canAskAgain = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
            showRationale = canAskAgain
            permanentlyDenied = !canAskAgain
        }
    }

    fun saveGalleryPhoto(uri: Uri) {
        scope.launch {
            busy = true
            try {
                val bytes = withContext(dispatchers.io) {
                    val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                        when (ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL,
                        )) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270
                            else -> 0
                        }
                    } ?: 0
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    } ?: return@withContext null
                    cropAndCompress(applyExifRotation(bitmap, orientation))
                }
                if (bytes == null || bytes.isEmpty()) onPhotoError() else onPhotoSaved(bytes)
            } catch (_: Exception) {
                onPhotoError()
            } finally {
                busy = false
            }
        }
    }

    val gallery = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) saveGalleryPhoto(uri)
    }

    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }

    fun takePhoto() {
        val output = File.createTempFile("coinlens-", ".jpg", context.cacheDir)
        busy = true
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(output).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    scope.launch {
                        try {
                            val bytes = withContext(dispatchers.io) {
                                val bitmap = BitmapFactory.decodeFile(output.absolutePath)
                                    ?: return@withContext null
                                cropAndCompress(bitmap)
                            }
                            output.delete()
                            if (bytes == null || bytes.isEmpty()) onPhotoError()
                            else onPhotoSaved(bytes)
                        } catch (_: Exception) {
                            onPhotoError()
                        } finally {
                            busy = false
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    output.delete()
                    busy = false
                    onPhotoError()
                }
            },
        )
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Column(
            modifier = modifier
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
                text = stringResource(
                    if (content.step == CaptureStep.FRONT) {
                        R.string.capture_front_prompt
                    } else {
                        R.string.capture_reverse_prompt
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when {
                !hasPermission || showRationale || permanentlyDenied -> PermissionBlock(
                    permanentlyDenied = permanentlyDenied,
                    onGrant = {
                        if (permanentlyDenied) {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                )

                else -> {
                    CameraPreview(
                        imageCapture = imageCapture,
                        onQuality = onQuality,
                        sideLabel = stringResource(
                            if (content.step == CaptureStep.FRONT) {
                                R.string.capture_front_side_label
                            } else {
                                R.string.capture_back_side_label
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (content.glareWarning) {
                        WarningBanner(text = stringResource(R.string.capture_glare_warning))
                    }
                    if (content.blurWarning) {
                        WarningBanner(text = stringResource(R.string.capture_blur_warning))
                    }
                    content.currentBytes?.let { bytes ->
                        CoinFrame(
                            obverse = bytes,
                            contentDescription = stringResource(R.string.capture_preview_photo),
                            size = Sizes.coinFrameMedium,
                        )
                    }
                    ShutterButton(
                        onClick = ::takePhoto,
                        enabled = !busy,
                        loading = busy,
                        contentDescription = stringResource(R.string.capture_camera),
                    )
                    SecondaryButton(
                        text = stringResource(
                            if (content.step == CaptureStep.FRONT) {
                                R.string.capture_choose_front
                            } else {
                                R.string.capture_choose_reverse
                            },
                        ),
                        onClick = { gallery.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth(),
                        loading = busy,
                        icon = Icons.Outlined.PhotoLibrary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        if (content.currentBytes != null) {
                            TertiaryButton(
                                text = stringResource(R.string.capture_retake),
                                onClick = onRetake,
                                enabled = !busy,
                            )
                        }
                        if (content.step == CaptureStep.FRONT && content.frontBytes != null) {
                            TertiaryButton(
                                text = stringResource(R.string.capture_add_reverse),
                                onClick = onSwitchSide,
                                enabled = !busy,
                            )
                        }
                        if (content.step == CaptureStep.BACK) {
                            TertiaryButton(
                                text = stringResource(R.string.capture_skip_back),
                                onClick = onSkipBack,
                                enabled = !busy,
                            )
                        }
                    }
                    PrimaryButton(
                        text = stringResource(R.string.capture_identify),
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = content.canFinish && !busy,
                    )
                }
            }
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

@Composable
private fun PermissionBlock(
    permanentlyDenied: Boolean,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                if (permanentlyDenied) {
                    R.string.capture_permission_blocked
                } else {
                    R.string.capture_permission_rationale
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        SecondaryButton(
            text = stringResource(
                if (permanentlyDenied) R.string.capture_open_settings else R.string.capture_grant,
            ),
            onClick = onGrant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WarningBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radii.medium),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Live preview with the 72%-of-short-edge circular guide. A throttled
 * [ImageAnalysis] analyzer samples the Y plane (stride 4) and reports
 * advisory glare/blur flags; capture binds Preview + [imageCapture] + analysis
 * to the lifecycle and unbinds on dispose.
 */
@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    onQuality: (Boolean, Boolean) -> Unit,
    sideLabel: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val dispatchers: DispatcherProvider = koinInject()
    val scope = rememberCoroutineScope()
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    val preview = remember {
        Preview.Builder().build().also { preview ->
            preview.setSurfaceProvider(ContextCompat.getMainExecutor(context)) { request ->
                surfaceRequest = request
            }
        }
    }
    val analysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { ia ->
                ia.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                    GuideQualityAnalyzer(onQuality),
                )
            }
    }
    DisposableEffect(lifecycle, preview, imageCapture, analysis) {
        var provider: ProcessCameraProvider? = null
        val job = scope.launch {
            val obtained = withContext(dispatchers.io) {
                ProcessCameraProvider.getInstance(context).get()
            }
            provider = obtained
            try {
                obtained.unbindAll()
                obtained.bindToLifecycle(
                    lifecycle,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                    analysis,
                )
            } catch (_: Exception) {
                // Camera unavailable on this device; gallery import still works.
            }
        }
        onDispose {
            job.cancel()
            try {
                provider?.unbindAll()
            } catch (_: Exception) {
                // Already torn down; nothing to release.
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val request = surfaceRequest
        if (request != null) {
            androidx.camera.compose.CameraXViewfinder(
                surfaceRequest = request,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(Radii.large)),
            )
        }
        val diameter = minOf(maxWidth, maxHeight) * GUIDE_DIAMETER_FRACTION
        val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)
        val ring = MaterialTheme.colorScheme.inverseOnSurface
        Canvas(modifier = Modifier.matchParentSize()) {
            if (request != null) {
                val radius = (diameter.toPx()) / 2f
                drawContext.canvas.saveLayer(null, null)
                drawRect(color = scrim)
                drawCircle(
                    color = Color.Transparent,
                    radius = radius,
                    blendMode = BlendMode.Clear,
                )
                drawContext.canvas.restore()
                drawCircle(color = ring, radius = radius, style = Stroke(width = Sizes.coinRing.toPx()))
            }
        }
        Text(
            text = sideLabel,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.md),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            textAlign = TextAlign.Center,
        )
    }
}

/** Samples the Y plane every ~300ms and maps it onto the pure quality math. */
private class GuideQualityAnalyzer(
    private val onQuality: (Boolean, Boolean) -> Unit,
) : ImageAnalysis.Analyzer {
    private val lastRun = AtomicLong(0)

    override fun analyze(image: ImageProxy) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastRun.get() < 300) return
            lastRun.set(now)
            val stride = 4
            val width = image.width / stride
            val height = image.height / stride
            if (width < 8 || height < 8) return
            val plane = image.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val luma = IntArray(width * height)
            val row = ByteArray(rowStride)
            for (y in 0 until height) {
                buffer.position((y * stride) * rowStride)
                buffer.get(row, 0, minOf(rowStride, row.size))
                for (x in 0 until width) {
                    luma[y * width + x] = row[x * stride].toInt() and 0xFF
                }
            }
            val cx = width / 2.0
            val cy = height / 2.0
            val radius = 0.36 * minOf(width, height)
            val glare = isGlare(luma, width, height, cx, cy, radius)
            val blurry = isBlurry(laplacianVariance(luma, width, height, cx, cy, radius))
            onQuality(glare, blurry)
        } catch (_: Exception) {
            // Advisory path only; a bad frame must never break preview.
        } finally {
            image.close()
        }
    }
}

@ThemePreviews
@Composable
private fun CaptureLoadingPreview() {
    CoinLensTheme {
        UiStateSurface<Nothing>(state = UiState.Loading, content = {})
    }
}

@ThemePreviews
@Composable
private fun CaptureQuotaPreview() {
    CoinLensTheme {
        EmptyState(
            message = "No scans left this month.",
            action = { SecondaryButton(text = "View plans", onClick = {}) },
        )
    }
}

@ThemePreviews
@Composable
private fun CaptureErrorPreview() {
    CoinLensTheme {
        UiStateSurface<Nothing>(
            state = UiState.Error(
                app.novushq.coinlens.common.AppError.Unknown("Could not read that photo."),
            ),
            onRetry = {},
            content = {},
        )
    }
}

@ThemePreviews
@Composable
private fun CapturePermissionPreview() {
    CoinLensTheme {
        PermissionBlock(permanentlyDenied = false, onGrant = {})
    }
}

@ThemePreviews
@Composable
private fun CaptureWarningsPreview() {
    CoinLensTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            WarningBanner(text = "Glare detected — tilt the coin or move away from the light.")
            WarningBanner(text = "Photo looks blurry — hold steady and tap to focus.")
        }
    }
}
