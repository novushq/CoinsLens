package app.novushq.coinlens.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.novushq.coinlens.designsystem.R
import app.novushq.coinlens.designsystem.theme.CoinTextStyles
import app.novushq.coinlens.designsystem.theme.Elevations
import app.novushq.coinlens.designsystem.theme.Sizes
import app.novushq.coinlens.designsystem.theme.Spacing

/** The one accent action per screen (scan, primary CTA). Shows a spinner and blocks taps while [loading]. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = Sizes.buttonHeight),
        shape = CircleShape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = Elevations.button,
            pressedElevation = Elevations.buttonPressed,
        ),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        ButtonLabel(text = text, loading = loading, icon = icon, spinnerColor = MaterialTheme.colorScheme.onPrimary)
    }
}

/** Second action on a screen (share, choose photo): tonal so the one accent stays the accent. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = Sizes.buttonHeight),
        shape = CircleShape,
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        ButtonLabel(text = text, loading = loading, icon = icon, spinnerColor = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/** Third action on a screen (restore, manage, scan another): outlined, quiet. */
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = Sizes.buttonHeight),
        shape = CircleShape,
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        ButtonLabel(text = text, loading = loading, icon = icon, spinnerColor = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ButtonLabel(text: String, loading: Boolean, icon: ImageVector?, spinnerColor: androidx.compose.ui.graphics.Color) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(Sizes.spinner),
            strokeWidth = Sizes.spinnerStroke,
            color = spinnerColor,
        )
        Row(Modifier.padding(start = Spacing.sm)) { Text(text, style = MaterialTheme.typography.labelLarge) }
    } else {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Row(Modifier.padding(start = Spacing.sm)) { Text(text, style = MaterialTheme.typography.labelLarge) }
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Standard navigation icon for top app bars on pushed screens. */
@Composable
fun BackIconButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onBack, modifier = modifier) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.ds_back))
    }
}

/** Standard dismiss icon for top app bars on terminal screens (fresh result, dialogs-as-screens). */
@Composable
fun CloseIconButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClose, modifier = modifier) {
        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.ds_close))
    }
}

/** Section title with a brass marker dot and an optional trailing text action ("See all"). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.minTouch),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            Modifier
                .size(Spacing.sm)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Museum-label metadata row, e.g. "UNITED STATES · 1955 · PHILADELPHIA". Blank parts are skipped. */
@Composable
fun MetadataRow(parts: List<String>, modifier: Modifier = Modifier) {
    Text(
        text = parts.filter { it.isNotBlank() }.joinToString(" · ") { it.uppercase() },
        style = CoinTextStyles.metadata,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Valuation disclaimer shown wherever a value appears. */
@Composable
fun DisclaimerText(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.ds_disclaimer),
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Spacing.lg),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
