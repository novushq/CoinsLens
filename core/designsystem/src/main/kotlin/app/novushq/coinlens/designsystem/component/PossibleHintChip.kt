package app.novushq.coinlens.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.novushq.coinlens.designsystem.R

/**
 * "Possible doubled die" — a variety worth checking. Always phrased as possible, never as a verdict.
 * [onClick] typically opens a sheet with the hint's description and where to look.
 */
@Composable
fun PossibleHintChip(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = onClick,
        label = { Text(stringResource(R.string.ds_possible_hint, hintPhrase(name))) },
        leadingIcon = {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
                tint = MaterialTheme.colorScheme.tertiary,
            )
        },
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    )
}

/** "Doubled die" → "doubled die"; acronyms and proper nouns ("NEW PENCE mule", "VDB") keep their case. */
internal fun hintPhrase(name: String): String {
    val trimmed = name.trim()
    return if (trimmed.length > 1 && trimmed[0].isUpperCase() && trimmed[1].isLowerCase()) {
        trimmed.replaceFirstChar { it.lowercaseChar() }
    } else {
        trimmed
    }
}
