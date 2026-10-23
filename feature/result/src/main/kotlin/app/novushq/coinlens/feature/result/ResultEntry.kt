package app.novushq.coinlens.feature.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.common.AppError
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.component.BackIconButton
import app.novushq.coinlens.designsystem.component.BrassDivider
import app.novushq.coinlens.designsystem.component.CloseIconButton
import app.novushq.coinlens.designsystem.component.CoinFrame
import app.novushq.coinlens.designsystem.component.DisclaimerText
import app.novushq.coinlens.designsystem.component.MetadataRow
import app.novushq.coinlens.designsystem.component.PossibleHintChip
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.component.SecondaryButton
import app.novushq.coinlens.designsystem.component.SectionHeader
import app.novushq.coinlens.designsystem.component.SkeletonList
import app.novushq.coinlens.designsystem.component.TertiaryButton
import app.novushq.coinlens.designsystem.component.UiStateSurface
import app.novushq.coinlens.designsystem.component.ValueRangeBar
import app.novushq.coinlens.designsystem.theme.CoinLensTheme
import app.novushq.coinlens.designsystem.theme.Sizes
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.designsystem.theme.Stroke
import app.novushq.coinlens.model.CoinIdentification
import app.novushq.coinlens.model.Confidence
import app.novushq.coinlens.model.Grade
import app.novushq.coinlens.model.ItemKind
import app.novushq.coinlens.model.Money
import app.novushq.coinlens.model.Rarity
import app.novushq.coinlens.model.ScanRecord
import app.novushq.coinlens.model.ValueEstimate
import app.novushq.coinlens.model.ValueRange
import app.novushq.coinlens.model.VarietyHint
import app.novushq.coinlens.model.usd
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import java.time.Instant
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val resultModule = module {
    viewModelOf(::ResultViewModel)
}

fun NavGraphBuilder.resultGraph(navigator: AppNavigator) {
    composable<Route.Result> {
        val viewModel: ResultViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        ResultContent(
            state = state,
            isFresh = viewModel.isFresh,
            onRetry = viewModel::retry,
            onOpenAddSheet = viewModel::openAddSheet,
            onCloseAddSheet = viewModel::closeAddSheet,
            onSelectFolder = viewModel::selectFolder,
            onNewFolderName = viewModel::setNewFolderName,
            onCreateFolder = viewModel::createFolder,
            onSelectGrade = viewModel::setGrade,
            onPrice = viewModel::setPrice,
            onNotes = viewModel::setNotes,
            onConfirmAdd = viewModel::confirmAdd,
            onExpandHint = viewModel::expandHint,
            onDismissHint = viewModel::dismissHint,
            onShare = { id -> navigator.navigate(Route.Share(id)) },
            onScanAnother = { navigator.navigate(Route.Capture) },
            onBack = navigator::back,
            onCloseToHome = { navigator.navigate(Route.Home, popUpTo = Route.Capture, inclusive = true) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultContent(
    state: UiState<ResultData>,
    isFresh: Boolean,
    onRetry: () -> Unit,
    onOpenAddSheet: () -> Unit,
    onCloseAddSheet: () -> Unit,
    onSelectFolder: (String?) -> Unit,
    onNewFolderName: (String) -> Unit,
    onCreateFolder: () -> Unit,
    onSelectGrade: (Grade?) -> Unit,
    onPrice: (String) -> Unit,
    onNotes: (String) -> Unit,
    onConfirmAdd: () -> Unit,
    onExpandHint: (VarietyHint) -> Unit,
    onDismissHint: () -> Unit,
    onShare: (String) -> Unit,
    onScanAnother: () -> Unit,
    onBack: () -> Unit,
    onCloseToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val addedMessage = stringResource(R.string.result_added_message)
    val added = (state as? UiState.Success)?.data?.addedToCabinet == true
    LaunchedEffect(added) {
        if (added) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            snackbar.showSnackbar(addedMessage)
        }
    }
    val revealed = state is UiState.Success
    LaunchedEffect(revealed) {
        if (revealed) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    val data = (state as? UiState.Success)?.data

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    if (isFresh) CloseIconButton(onCloseToHome) else BackIconButton(onBack)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        UiStateSurface(
            state = state,
            onRetry = onRetry,
            modifier = Modifier.padding(insets),
            loading = { ResultLoadingSkeleton() },
        ) { result ->
            val coin = result.record.identification
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Spacing.xl,
                    vertical = Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    ) {
                        CoinFrame(
                            obverse = result.record.obversePath,
                            reverse = result.record.reversePath,
                            contentDescription = if (coin.recognized) coin.name else stringResource(R.string.result_unknown_photo),
                            size = Sizes.coinFrameLarge,
                        )
                        if (coin.recognized) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(Stroke.hairline, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.lg),
                                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                                ) {
                                    Text(
                                        text = coin.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .semantics { heading() },
                                        style = MaterialTheme.typography.displaySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    MetadataRow(
                                        listOf(coin.country, coin.year?.toString() ?: coin.yearText, coin.mintMark ?: coin.mint.orEmpty()),
                                    )
                                    RarityChip(rarity = coin.rarity)
                                    BrassDivider()
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.result_unrecognized),
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Text(coin.description, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (coin.recognized) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            SectionHeader(title = stringResource(R.string.result_value))
                            ValueRangeBar(
                                circulated = coin.value.circulated,
                                uncirculated = coin.value.uncirculated,
                                confidence = coin.value.confidence,
                            )
                            DisclaimerText()
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            SectionHeader(title = stringResource(R.string.result_details))
                            val details = buildList {
                                add(stringResource(R.string.result_denomination) to coin.denomination)
                                add(stringResource(R.string.result_composition) to coin.composition.orEmpty())
                                coin.weightGrams?.let { add(stringResource(R.string.result_weight) to stringResource(R.string.result_grams, it)) }
                                coin.diameterMm?.let { add(stringResource(R.string.result_diameter) to stringResource(R.string.result_millimeters, it)) }
                            }.filter { it.second.isNotBlank() }
                            details.forEachIndexed { index, (label, value) ->
                                if (index > 0) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = Stroke.hairline)
                                }
                                DetailRow(label, value)
                            }
                            if (coin.description.isNotBlank()) {
                                Text(coin.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    if (coin.hints.isNotEmpty()) {
                        item {
                            HintChips(
                                hints = coin.hints,
                                onExpand = onExpandHint,
                            )
                        }
                    }
                    if (coin.alternatives.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                SectionHeader(title = stringResource(R.string.result_alternatives))
                                coin.alternatives.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            PrimaryButton(
                                text = stringResource(if (result.addedToCabinet) R.string.result_added else R.string.result_add),
                                onClick = onOpenAddSheet,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !result.addedToCabinet,
                                icon = Icons.Outlined.Add,
                            )
                            SecondaryButton(
                                text = stringResource(R.string.result_share),
                                onClick = { onShare(result.record.id) },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Outlined.Share,
                            )
                            TertiaryButton(
                                text = stringResource(R.string.result_scan_another),
                                onClick = onScanAnother,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    item {
                        PrimaryButton(
                            text = stringResource(R.string.result_try_again),
                            onClick = onScanAnother,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    data?.expandedHint?.let { hint ->
        HintDetailSheet(hint = hint, onDismiss = onDismissHint)
    }
    data?.sheet?.let { sheet ->
        AddToCollectionSheet(
            sheet = sheet,
            onDismiss = onCloseAddSheet,
            onSelectFolder = onSelectFolder,
            onNewFolderName = onNewFolderName,
            onCreateFolder = onCreateFolder,
            onSelectGrade = onSelectGrade,
            onPrice = onPrice,
            onNotes = onNotes,
            onConfirm = onConfirmAdd,
        )
    }
}

/** Skeleton of the result card with rotating identification hints. */
@Composable
private fun ResultLoadingSkeleton(modifier: Modifier = Modifier) {
    val hints = listOf(
        stringResource(R.string.result_hint_date),
        stringResource(R.string.result_hint_mint),
        stringResource(R.string.result_hint_value),
    )
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            index = (index + 1) % hints.size
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            hints[index],
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SkeletonList(rows = 3)
    }
}

@Composable
private fun RarityChip(rarity: Rarity, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(Stroke.hairline, MaterialTheme.colorScheme.tertiary),
    ) {
        Text(
            text = rarityLabel(rarity),
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun rarityLabel(rarity: Rarity): String = stringResource(
    when (rarity) {
        Rarity.COMMON -> R.string.result_rarity_common
        Rarity.SCARCE -> R.string.result_rarity_scarce
        Rarity.RARE -> R.string.result_rarity_rare
        Rarity.VERY_RARE -> R.string.result_rarity_very_rare
        Rarity.UNKNOWN -> R.string.result_rarity_unknown
    },
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HintChips(
    hints: List<VarietyHint>,
    onExpand: (VarietyHint) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(title = stringResource(R.string.result_hints))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            hints.forEach { hint ->
                PossibleHintChip(
                    name = hint.name,
                    onClick = { onExpand(hint) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HintDetailSheet(
    hint: VarietyHint,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(R.string.result_hint_details),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.result_possible_hint, hint.name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (hint.description.isNotBlank()) {
                Text(hint.description, style = MaterialTheme.typography.bodyMedium)
            }
            if (hint.whereToLook.isNotBlank()) {
                Text(
                    stringResource(R.string.result_where_to_look, hint.whereToLook),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToCollectionSheet(
    sheet: AddSheetState,
    onDismiss: () -> Unit,
    onSelectFolder: (String?) -> Unit,
    onNewFolderName: (String) -> Unit,
    onCreateFolder: () -> Unit,
    onSelectGrade: (Grade?) -> Unit,
    onPrice: (String) -> Unit,
    onNotes: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        var folderExpanded by remember(sheet.folders) { mutableStateOf(false) }
        var gradeExpanded by remember { mutableStateOf(false) }
        val selectedFolder = sheet.folders.firstOrNull { it.id == sheet.folderId }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(R.string.result_add_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            ExposedDropdownMenuBox(
                expanded = folderExpanded,
                onExpandedChange = { folderExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedFolder?.name ?: stringResource(R.string.result_no_folder),
                    onValueChange = {},
                    readOnly = true,
                    enabled = !sheet.loadingFolders,
                    label = { Text(stringResource(R.string.result_folder_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(folderExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = folderExpanded,
                    onDismissRequest = { folderExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.result_no_folder)) },
                        onClick = { onSelectFolder(null); folderExpanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    sheet.folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder.name) },
                            onClick = { onSelectFolder(folder.id); folderExpanded = false },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = sheet.newFolderName,
                    onValueChange = onNewFolderName,
                    label = { Text(stringResource(R.string.result_new_folder_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = stringResource(R.string.result_new_folder_create),
                    onClick = onCreateFolder,
                    enabled = sheet.newFolderName.isNotBlank() && !sheet.saving,
                )
            }
            ExposedDropdownMenuBox(
                expanded = gradeExpanded,
                onExpandedChange = { gradeExpanded = it },
            ) {
                OutlinedTextField(
                    value = sheet.grade?.name?.replace('_', ' ') ?: stringResource(R.string.result_no_grade),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.result_grade_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(gradeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = gradeExpanded,
                    onDismissRequest = { gradeExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.result_no_grade)) },
                        onClick = { onSelectGrade(null); gradeExpanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    Grade.entries.forEach { grade ->
                        DropdownMenuItem(
                            text = { Text(grade.name.replace('_', ' ')) },
                            onClick = { onSelectGrade(grade); gradeExpanded = false },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = sheet.priceText,
                onValueChange = onPrice,
                label = { Text(stringResource(R.string.result_price_label)) },
                placeholder = { Text(stringResource(R.string.result_price_hint)) },
                singleLine = true,
                isError = sheet.priceError,
                supportingText = if (sheet.priceError) {
                    { Text(stringResource(R.string.result_price_error)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = sheet.notes,
                onValueChange = onNotes,
                label = { Text(stringResource(R.string.result_notes_label)) },
                placeholder = { Text(stringResource(R.string.result_notes_hint)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            sheet.error?.let {
                Text(
                    stringResource(R.string.result_add_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            PrimaryButton(
                text = stringResource(R.string.result_save),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                loading = sheet.saving,
                icon = Icons.Outlined.Add,
            )
            TertiaryButton(
                text = stringResource(R.string.result_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    if (value.isNotBlank()) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun previewRecord(recognized: Boolean): ScanRecord = ScanRecord(
    id = "scan-1",
    createdAt = Instant.parse("2026-09-27T10:00:00Z"),
    obversePath = "",
    reversePath = null,
    identification = if (recognized) {
        CoinIdentification(
            kind = ItemKind.COIN,
            name = "Lincoln cent",
            country = "United States",
            denomination = "1 cent",
            year = 1969,
            mintMark = "S",
            composition = "Copper",
            weightGrams = 3.11,
            diameterMm = 19.0,
            rarity = Rarity.RARE,
            description = "Wheat reverse penny from San Francisco.",
            value = ValueEstimate(
                circulated = ValueRange(usd(1.0), usd(3.0)),
                uncirculated = ValueRange(Money(1000, Money.USD), Money(2000, Money.USD)),
                confidence = Confidence.MEDIUM,
            ),
            hints = listOf(
                VarietyHint(
                    name = "doubled die obverse",
                    description = "Doubling shows on the date and lettering.",
                    whereToLook = "the date and LIBERTY",
                ),
            ),
            alternatives = listOf("1969 Philadelphia cent"),
        )
    } else {
        CoinIdentification.unrecognized("The photo looks like a bottle cap, not a coin.")
    },
)

@Composable
private fun PreviewContent(state: UiState<ResultData>, isFresh: Boolean = false) {
    CoinLensTheme {
        ResultContent(
            state = state,
            isFresh = isFresh,
            onRetry = {},
            onOpenAddSheet = {},
            onCloseAddSheet = {},
            onSelectFolder = {},
            onNewFolderName = {},
            onCreateFolder = {},
            onSelectGrade = {},
            onPrice = {},
            onNotes = {},
            onConfirmAdd = {},
            onExpandHint = {},
            onDismissHint = {},
            onShare = {},
            onScanAnother = {},
            onBack = {},
            onCloseToHome = {},
        )
    }
}

@Preview
@Composable
private fun ResultLoadingPreview() {
    PreviewContent(state = UiState.Loading, isFresh = true)
}

@Preview
@Composable
private fun ResultSuccessPreview() {
    PreviewContent(state = UiState.Success(ResultData(previewRecord(recognized = true))))
}

@Preview
@Composable
private fun ResultUnrecognizedPreview() {
    PreviewContent(state = UiState.Success(ResultData(previewRecord(recognized = false))))
}

@Preview
@Composable
private fun ResultErrorPreview() {
    PreviewContent(state = UiState.Error(AppError.Network()))
}
