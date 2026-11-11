package io.github.amirezafahimi.cardscanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.amirezafahimi.cardscanner.R

/**
 * Default overlay drawn over the camera preview: a [ScannerFrame] cut-out, a close button,
 * instructions and a torch toggle.
 *
 * Pass your own composable to [CardScannerScreen]'s `overlay` parameter to replace it entirely.
 *
 * @param isTorchOn current torch state, drives which flashlight icon is shown.
 * @param onTorchToggle called when the torch button is tapped.
 * @param onCloseClick called when the close button is tapped.
 * @param title headline shown above the frame.
 * @param subtitle hint shown under [title].
 */
@Composable
fun CardOverlay(
    isTorchOn: Boolean,
    onTorchToggle: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.card_scanner_overlay_title),
    subtitle: String = stringResource(R.string.card_scanner_overlay_subtitle)
) {
    Box(modifier = modifier.fillMaxSize()) {
        ScannerFrame(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .focusable(false)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onCloseClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.card_scanner_close),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onTorchToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isTorchOn) {
                    Icons.Outlined.FlashlightOff
                } else {
                    Icons.Outlined.FlashlightOn
                },
                contentDescription = stringResource(R.string.card_scanner_toggle_torch),
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
