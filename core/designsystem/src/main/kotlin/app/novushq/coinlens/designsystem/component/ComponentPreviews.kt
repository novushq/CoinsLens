package app.novushq.coinlens.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.theme.CoinLensTheme
import app.novushq.coinlens.designsystem.theme.CoinTextStyles
import app.novushq.coinlens.designsystem.theme.Sizes
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.designsystem.theme.ThemePreviews
import app.novushq.coinlens.model.Confidence
import app.novushq.coinlens.model.ValueRange
import app.novushq.coinlens.model.usd

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    CoinLensTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) { content() }
        }
    }
}

@ThemePreviews
@Composable
private fun MuseumLabelPreview() = PreviewSurface {
    CoinFrame(obverse = null, contentDescription = "1955 Lincoln cent", size = Sizes.coinFrameMedium)
    Text("1955 Lincoln Wheat Cent", style = MaterialTheme.typography.headlineMedium)
    MetadataRow(listOf("United States", "1955", "Philadelphia"))
    MoneyText(usd(1250.0), style = CoinTextStyles.moneyDisplay)
}

@ThemePreviews
@Composable
private fun ValueRangeBarPreview() = PreviewSurface {
    ValueRangeBar(
        circulated = ValueRange(usd(0.1), usd(0.5)),
        uncirculated = ValueRange(usd(2.0), usd(12.0)),
        confidence = Confidence.MEDIUM,
    )
}

@ThemePreviews
@Composable
private fun PillsAndChipsPreview() = PreviewSurface {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ConfidencePill(Confidence.HIGH)
        ConfidencePill(Confidence.MEDIUM)
        ConfidencePill(Confidence.LOW)
        PossibleHintChip(name = "Doubled die obverse", onClick = {})
        PossibleHintChip(name = "NEW PENCE mule", onClick = {})
    }
}

@ThemePreviews
@Composable
private fun ButtonsPreview() = PreviewSurface {
    PrimaryButton(text = "Scan a coin", onClick = {}, icon = Icons.Outlined.PhotoCamera)
    PrimaryButton(text = "Identifying…", onClick = {}, loading = true)
    SecondaryButton(text = "Share result", onClick = {}, icon = Icons.Outlined.PhotoCamera)
    TertiaryButton(text = "Scan another", onClick = {})
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        BackIconButton(onBack = {})
        CloseIconButton(onClose = {})
    }
    SectionHeader(title = "Recent scans", actionLabel = "See all", onAction = {})
    DisclaimerText()
}

@ThemePreviews
@Composable
private fun SurfacesPreview() = PreviewSurface {
    HeroCard {
        Text("Scan a coin", style = MaterialTheme.typography.headlineMedium)
        Text("Even light, full coin in frame.", style = MaterialTheme.typography.bodyMedium)
        PrimaryButton(text = "Open camera", onClick = {})
    }
    BrassDivider()
    SelectableCard(selected = true, onSelect = {}) {
        Text("Annual plan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(Spacing.lg))
    }
    SelectableCard(selected = false, onSelect = {}) {
        Text("Weekly plan", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(Spacing.lg))
    }
    ShutterButton(onClick = {})
}

@ThemePreviews
@Composable
private fun StatesPreview() = PreviewSurface {
    Column(Modifier.height(220.dp)) { UiStateSurface<Unit>(UiState.Loading) {} }
    Column(Modifier.height(220.dp)) { UiStateSurface<Unit>(UiState.Empty("No coins yet")) {} }
    Column(Modifier.height(260.dp)) { UiStateSurface<Unit>(UiState.Error(AppError.Network()), onRetry = {}) {} }
}
