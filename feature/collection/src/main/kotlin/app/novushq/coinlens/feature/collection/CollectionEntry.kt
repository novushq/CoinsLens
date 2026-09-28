package app.novushq.coinlens.feature.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.novushq.coinlens.common.UiState
import app.novushq.coinlens.designsystem.component.DisclaimerText
import app.novushq.coinlens.designsystem.component.MoneyText
import app.novushq.coinlens.designsystem.component.MoneyRangeText
import app.novushq.coinlens.designsystem.component.PrimaryButton
import app.novushq.coinlens.designsystem.component.SectionHeader
import app.novushq.coinlens.designsystem.component.UiStateSurface
import app.novushq.coinlens.designsystem.theme.Spacing
import app.novushq.coinlens.model.CollectionItem
import app.novushq.coinlens.model.Folder
import app.novushq.coinlens.model.Grade
import app.novushq.coinlens.model.Money
import app.novushq.coinlens.model.ValueRange
import app.novushq.coinlens.model.ValuedItem
import app.novushq.coinlens.navigation.AppNavigator
import app.novushq.coinlens.navigation.Route
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.math.RoundingMode
import java.util.Locale

val collectionModule = module { viewModelOf(::CollectionViewModel) }

fun NavGraphBuilder.collectionGraph(navigator: AppNavigator) {
    composable<Route.Collection> { CollectionDestination(navigator) }
    composable<Route.Folder> { CollectionDestination(navigator) }
    composable<Route.Item> { CollectionDestination(navigator) }
}

private fun sanitizePriceInput(input: String): String {
    val normalized = input.filter { it in '0'..'9' || it == '.' || it == ',' }
    val decimal = normalized.indexOfAny(charArrayOf('.', ','))
    val whole = (if (decimal < 0) normalized else normalized.substring(0, decimal)).take(10)
    if (decimal < 0) return whole
    val fraction = normalized.substring(decimal + 1).filter(Char::isDigit).take(2)
    return "$whole.$fraction"
}

@Composable
private fun CollectionDestination(navigator: AppNavigator, viewModel: CollectionViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        UiStateSurface(state = state, onRetry = viewModel::retry, modifier = Modifier.padding(insets)) { data ->
            when {
                data.item != null -> ItemScreen(data.item, data.folders, viewModel, navigator)
                data.folder != null -> FolderScreen(data.folder.folder, data.folder.items, viewModel, navigator)
                else -> CollectionScreen(data, viewModel, navigator)
            }
        }
    }
}

@Composable
private fun CollectionScreen(data: CollectionScreenData, viewModel: CollectionViewModel, navigator: AppNavigator) {
    var newFolder by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item { Text(stringResource(R.string.collection_title), style = MaterialTheme.typography.headlineMedium) }
        if (data.summary.itemCount == 0) item {
            PrimaryButton(
                text = stringResource(R.string.collection_first_scan),
                onClick = { navigator.navigate(Route.Capture) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Card {
                Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(stringResource(R.string.collection_estimated_value), style = MaterialTheme.typography.labelLarge)
                    MoneyText(data.summary.totalMid, style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold))
                    MoneyRangeText(ValueRange(data.summary.totalLow, data.summary.totalHigh))
                    Text(stringResource(R.string.collection_items, data.summary.itemCount), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.collection_cost_basis), style = MaterialTheme.typography.labelLarge)
                    MoneyText(data.summary.totalCost, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(stringResource(R.string.collection_estimate_vs_cost), style = MaterialTheme.typography.bodyMedium)
                        MoneyText(Money(data.summary.totalMid.cents - data.summary.totalCost.cents), style = MaterialTheme.typography.bodyMedium)
                    }
                    DisclaimerText()
                }
            }
        }
        item { SectionHeader(stringResource(R.string.collection_folders)) }
        if (data.folders.isEmpty()) item { Text(stringResource(R.string.collection_no_folders), style = MaterialTheme.typography.bodyMedium) }
        items(data.folders, key = { it.id }) { folder ->
            FolderRow(folder) { navigator.navigate(Route.Folder(folder.id)) }
        }
        item {
            OutlinedButton(onClick = { newFolder = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.collection_create_folder))
            }
        }
        val unsorted = data.items.filter { it.item.folderId == null }
        if (unsorted.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.collection_unsorted)) }
            items(unsorted, key = { it.item.id }) { value -> ItemRow(value) { navigator.navigate(Route.Item(value.item.id)) } }
        }
        item { TextButton(onClick = navigator::back, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.collection_back)) } }
    }
    if (newFolder) AlertDialog(
        onDismissRequest = { newFolder = false },
        title = { Text(stringResource(R.string.collection_create_folder)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.collection_folder_name)) }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { viewModel.createFolder(name); name = ""; newFolder = false }, enabled = name.isNotBlank()) { Text(stringResource(R.string.collection_create)) }
        },
        dismissButton = { TextButton(onClick = { newFolder = false }) { Text(stringResource(R.string.collection_cancel)) } },
    )
}

@Composable
private fun FolderScreen(folder: Folder, items: List<ValuedItem>, viewModel: CollectionViewModel, navigator: AppNavigator) {
    var sort by remember { mutableStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(folder.name) }
    val sorted = when (sort) {
        1 -> items.sortedBy { it.scan.createdAt }
        2 -> items.sortedBy { it.scan.identification.name.lowercase() }
        else -> items.sortedByDescending { it.range.midpoint.cents }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        item { Text(folder.name, style = MaterialTheme.typography.headlineMedium) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            TextButton(onClick = { editName = true }) { Text(stringResource(R.string.collection_rename)) }
            TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.collection_delete)) }
            TextButton(onClick = { sort = (sort + 1) % 3 }) { Text(stringResource(listOf(R.string.collection_sort_value, R.string.collection_sort_date, R.string.collection_sort_name)[sort])) }
        } }
        item { SectionHeader(stringResource(R.string.collection_items, folder.itemCount)) }
        if (sorted.isEmpty()) item { Text(stringResource(R.string.collection_empty_folder), style = MaterialTheme.typography.bodyMedium) }
        items(sorted, key = { it.item.id }) { value -> ItemRow(value) { navigator.navigate(Route.Item(value.item.id)) } }
        item { TextButton(onClick = navigator::back, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.collection_back)) } }
    }
    if (editName) AlertDialog(
        onDismissRequest = { editName = false }, title = { Text(stringResource(R.string.collection_rename)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text(stringResource(R.string.collection_folder_name)) }) },
        confirmButton = { TextButton(onClick = { viewModel.renameFolder(folder.id, name); editName = false }, enabled = name.isNotBlank()) { Text(stringResource(R.string.collection_save)) } },
        dismissButton = { TextButton(onClick = { editName = false }) { Text(stringResource(R.string.collection_cancel)) } },
    )
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.collection_delete_folder_title, folder.name)) },
        text = { Text(stringResource(R.string.collection_delete_folder_body, folder.itemCount)) },
        confirmButton = { TextButton(onClick = { viewModel.deleteFolder(folder.id); navigator.back() }) { Text(stringResource(R.string.collection_delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.collection_cancel)) } },
    )
}

@Composable
private fun ItemScreen(value: ValuedItem, folders: List<Folder>, viewModel: CollectionViewModel, navigator: AppNavigator) {
    val current = value.item
    var grade by remember(current.id) { mutableStateOf(current.grade ?: Grade.GOOD) }
    var notes by remember(current.id) { mutableStateOf(current.gradeNotes) }
 var price by remember(current.id) { mutableStateOf(current.purchasePrice?.let { "%.2f".format(Locale.US, it.cents / 100.0) }.orEmpty()) }
    var quantity by remember(current.id) { mutableStateOf(current.quantity.toString()) }
    var folderId by remember(current.id) { mutableStateOf(current.folderId) }
    var gradeMenu by remember { mutableStateOf(false) }
    var folderMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        item { Text(value.scan.identification.name, style = MaterialTheme.typography.headlineMedium) }
        item { Text(listOf(value.scan.identification.country, value.scan.identification.year?.toString() ?: value.scan.identification.yearText).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodyMedium) }
        item { TextButton(onClick = { navigator.navigate(Route.Result(value.scan.id)) }) { Text(stringResource(R.string.collection_open_scan)) } }
        item { SectionHeader(stringResource(R.string.collection_condition)) }
        item {
            Column {
                OutlinedButton(onClick = { gradeMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.collection_grade, grade.name.replace('_', ' '))) }
                DropdownMenu(expanded = gradeMenu, onDismissRequest = { gradeMenu = false }) {
                    Grade.entries.forEach { option -> DropdownMenuItem(text = { Text(option.name.replace('_', ' ')) }, onClick = { grade = option; gradeMenu = false }) }
                }
            }
        }
        item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.collection_grade_notes)) }, modifier = Modifier.fillMaxWidth()) }
 item { OutlinedTextField(value = price, onValueChange = { price = sanitizePriceInput(it) }, label = { Text(stringResource(R.string.collection_purchase_price)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item { OutlinedTextField(value = quantity, onValueChange = { quantity = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(R.string.collection_quantity)) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item {
            Column {
                OutlinedButton(onClick = { folderMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(folders.firstOrNull { it.id == folderId }?.name ?: stringResource(R.string.collection_unsorted))
                }
                DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.collection_unsorted)) }, onClick = { folderId = null; folderMenu = false })
                    folders.forEach { folder -> DropdownMenuItem(text = { Text(folder.name) }, onClick = { folderId = folder.id; folderMenu = false }) }
                }
            }
        }
        item { Text(stringResource(R.string.collection_item_value), style = MaterialTheme.typography.labelLarge) }
        item { MoneyText(value.range.midpoint, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif)) }
        item {
            PrimaryButton(
                text = stringResource(R.string.collection_save),
                onClick = {
 val cents = runCatching {
     price.toBigDecimalOrNull()?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.longValueExact()
 }.getOrNull()
                    viewModel.updateItem(current.copy(grade = grade, gradeNotes = notes, purchasePrice = cents?.let(::Money), quantity = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1, folderId = folderId))
                }, modifier = Modifier.fillMaxWidth(),
            )
        }
        item { TextButton(onClick = { navigator.navigate(Route.Share(value.scan.id)) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.collection_share)) } }
        item { TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.collection_delete)) } }
        item { TextButton(onClick = navigator::back, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.collection_back)) } }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.collection_delete_item_title)) },
        text = { Text(stringResource(R.string.collection_delete_item_body)) },
        confirmButton = { TextButton(onClick = { viewModel.deleteItem(current.id); navigator.back() }) { Text(stringResource(R.string.collection_delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.collection_cancel)) } },
    )
}

@Composable
private fun FolderRow(folder: Folder, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(folder.name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.collection_items, folder.itemCount), style = MaterialTheme.typography.bodySmall)
            }
            MoneyText(folder.totalValue, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ItemRow(value: ValuedItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.weight(1f)) {
                Text(value.scan.identification.name, style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(value.scan.identification.year?.toString() ?: value.scan.identification.yearText, value.item.grade?.name?.replace('_', ' ')).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
            MoneyText(value.range.midpoint, style = MaterialTheme.typography.titleMedium)
        }
    }
}
