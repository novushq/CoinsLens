package app.novushq.coinlens.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.novushq.coinlens.designsystem.R
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.model.Confidence

/** Confidence is never colour alone: colour + icon + word (HIGH primary, MEDIUM tertiary, LOW error). */
@Composable
fun ConfidencePill(confidence: Confidence, modifier: Modifier = Modifier) {
    val (color, icon, label) = confidenceVisuals(confidence)
    val text = stringResource(label)
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            .clearAndSetSemantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun confidenceVisuals(confidence: Confidence): Triple<Color, ImageVector, Int> {
    val scheme = MaterialTheme.colorScheme
    return when (confidence) {
        Confidence.HIGH -> Triple(scheme.primary, Icons.Outlined.Verified, R.string.ds_confidence_high)
        Confidence.MEDIUM -> Triple(scheme.tertiary, Icons.AutoMirrored.Outlined.HelpOutline, R.string.ds_confidence_medium)
        Confidence.LOW -> Triple(scheme.error, Icons.Outlined.WarningAmber, R.string.ds_confidence_low)
    }
}
