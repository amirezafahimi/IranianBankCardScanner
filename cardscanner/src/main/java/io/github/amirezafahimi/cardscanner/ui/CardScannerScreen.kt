package io.github.amirezafahimi.cardscanner.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.amirezafahimi.cardscanner.R
import io.github.amirezafahimi.cardscanner.analyzer.CardAnalyzer
import io.github.amirezafahimi.cardscanner.model.CardScannerConfig
import io.github.amirezafahimi.cardscanner.model.ScannedCard
import io.github.amirezafahimi.cardscanner.utils.CardScannerCameraPermissionStore
import io.github.amirezafahimi.cardscanner.utils.resolveCameraPermissionDialogState
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Full screen bank card scanner.
 *
 * Drop it on a route or in a dialog and it takes care of everything: it asks for the camera
 * permission (and sends the user to app settings once they picked "Don't ask again"), binds a
 * CameraX preview, runs OCR through [CardAnalyzer], and calls [onCardDetected] with the first card
 * whose number passes the Luhn check and matches one of the configured IINs.
 *
 * ```kotlin
 * CardScannerScreen(
 *     onCardDetected = { card -> viewModel.onCardScanned(card) },
 *     onCloseClick = { navController.popBackStack() }
 * )
 * ```
 *
 * The camera is released, the torch turned off and the ML Kit recognizer closed when the composable
 * leaves composition, so no cleanup is needed on the caller's side.
 *
 * @param onCardDetected called on the main thread with the scanned card. The scanner keeps running
 *   until it is removed from composition, so dismiss it from this callback.
 * @param onCloseClick called when the user dismisses the scanner, including when they decline the
 *   camera permission.
 * @param config scan tuning, see [CardScannerConfig].
 * @param onScannerOpened called once per open, before the camera starts. Hook your analytics here.
 * @param overlay the UI drawn over the camera preview. Defaults to [CardOverlay]; the lambda
 *   receives the torch state, a torch toggle and the close action.
 */
@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CardScannerScreen(
    onCardDetected: (ScannedCard) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: CardScannerConfig = CardScannerConfig(),
    onScannerOpened: () -> Unit = {},
    overlay: @Composable (
        isTorchOn: Boolean,
        onTorchToggle: () -> Unit,
        onCloseClick: () -> Unit
    ) -> Unit = { isTorchOn, onTorchToggle, onClose ->
        CardOverlay(
            isTorchOn = isTorchOn,
            onTorchToggle = onTorchToggle,
            onCloseClick = onClose
        )
    }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val currentOnScannerOpened by rememberUpdatedState(onScannerOpened)

    LaunchedEffect(Unit) { currentOnScannerOpened() }

    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val initialDialogState = remember { resolveCameraPermissionDialogState(context) }
    var showRequestDialog by remember { mutableStateOf(initialDialogState.showRequest) }
    var showSettingsDialog by remember { mutableStateOf(initialDialogState.showSettings) }
    var isAwaitingSettingsReturn by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasCameraPermission = true
            showRequestDialog = false
            showSettingsDialog = false
        } else {
            val activity = context.findActivity()
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false

            if (shouldShowRationale) {
                // Plain denial: the system will ask again next time, so just leave.
                onCloseClick()
            } else {
                // "Don't ask again": remember it so the settings dialog opens straight away.
                CardScannerCameraPermissionStore.markPermanentlyDenied(context)
                showSettingsDialog = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraControl?.enableTorch(false)
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            recognizer.close()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    hasCameraPermission = true
                    showRequestDialog = false
                    showSettingsDialog = false
                    isAwaitingSettingsReturn = false
                    CardScannerCameraPermissionStore.clearPermanentlyDenied(context)
                } else if (isAwaitingSettingsReturn) {
                    isAwaitingSettingsReturn = false
                    CardScannerCameraPermissionStore.clearPermanentlyDenied(context)
                    val dialogState = resolveCameraPermissionDialogState(context)
                    showRequestDialog = dialogState.showRequest
                    showSettingsDialog = dialogState.showSettings
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showRequestDialog) {
        CameraPermissionDialog(
            message = stringResource(R.string.card_scanner_camera_permission_message),
            confirmText = stringResource(R.string.card_scanner_confirm),
            onDismiss = {
                showRequestDialog = false
                onCloseClick()
            },
            onConfirm = {
                showRequestDialog = false
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    if (showSettingsDialog) {
        CameraPermissionDialog(
            message = stringResource(R.string.card_scanner_camera_permission_settings_message),
            confirmText = stringResource(R.string.card_scanner_go_to_settings),
            onDismiss = {
                showSettingsDialog = false
                onCloseClick()
            },
            onConfirm = {
                showSettingsDialog = false
                isAwaitingSettingsReturn = true
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        if (hasCameraPermission) {
            val previewView = remember { PreviewView(context) }

            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Tap to focus on the tapped point.
                            val point = previewView.meteringPointFactory
                                .createPoint(offset.x, offset.y)
                            cameraControl?.startFocusAndMetering(point.toFocusAction())
                        }
                    }
            )

            LaunchedEffect(isTorchOn) {
                cameraControl?.enableTorch(isTorchOn)
            }

            LaunchedEffect(Unit) {
                // Suspending await: if the composable leaves before the provider resolves, the
                // cancelled LaunchedEffect never reaches bindToLifecycle.
                val provider = ProcessCameraProvider.awaitInstance(context)
                cameraProvider = provider

                val resolutionSelector = ResolutionSelector.Builder()
                    .setAllowedResolutionMode(
                        ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
                    )
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            // 720p is the sweet spot between OCR quality and speed.
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER
                        )
                    )
                    .build()

                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                imageAnalysis.setAnalyzer(
                    cameraExecutor,
                    CardAnalyzer(
                        recognizer = recognizer,
                        scope = scope,
                        config = config,
                        onResult = onCardDetected
                    )
                )

                try {
                    provider.unbindAll()

                    val camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )

                    cameraControl = camera.cameraControl

                    val centerPoint = previewView.meteringPointFactory.createPoint(
                        previewView.width / 2f,
                        previewView.height / 2f
                    )
                    camera.cameraControl.startFocusAndMetering(centerPoint.toFocusAction())

                    // Embossed digits read much better with noise reduction and edge enhancement.
                    Camera2CameraControl.from(camera.cameraControl).captureRequestOptions =
                        CaptureRequestOptions.Builder()
                            .setCaptureRequestOption(
                                CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON
                            )
                            .setCaptureRequestOption(
                                CaptureRequest.NOISE_REDUCTION_MODE,
                                CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
                            )
                            .setCaptureRequestOption(
                                CaptureRequest.EDGE_MODE,
                                CaptureRequest.EDGE_MODE_HIGH_QUALITY
                            )
                            .build()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            overlay(
                isTorchOn,
                { isTorchOn = !isTorchOn },
                onCloseClick
            )
        }
    }
}

private fun MeteringPoint.toFocusAction(): FocusMeteringAction =
    FocusMeteringAction
        .Builder(this, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
        .setAutoCancelDuration(FOCUS_AUTO_CANCEL_SECONDS, TimeUnit.SECONDS)
        .build()

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

private const val FOCUS_AUTO_CANCEL_SECONDS = 3L
