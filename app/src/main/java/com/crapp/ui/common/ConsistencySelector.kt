package com.crapp.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

/**
 * The Purina Fecal Scoring Chart: the standard 7-point veterinary scale used to
 * describe dog stool consistency, from 1 (very hard/dry) to 7 (liquid, no texture).
 * Chosen over the human Bristol Stool Chart -- see docs/development-plan.md §2 --
 * but given the same picture-plus-text selection UX via [StoolShapeIcon].
 */
private val consistencyDescriptions = mapOf(
    1 to "Very hard, dry pellets",
    2 to "Firm but segmented",
    3 to "Firm, log-shaped, moist surface",
    4 to "Soft, log-shaped, loses form when picked up",
    5 to "Very soft, no distinct shape, pile form",
    6 to "Loose, mushy texture",
    7 to "Liquid, no texture"
)

/** Purina's own reference page for what each of the 7 scores looks like -- linked from the selector below. */
const val PURINA_FECAL_SCORING_CHART_URL = "https://vetcentre.purina.co.uk/news-articles/faecal-score-chart"

@Composable
fun ConsistencySelector(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = modifier.fillMaxWidth()) {
        TextButton(
            onClick = { uriHandler.openUri(PURINA_FECAL_SCORING_CHART_URL) },
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text("What do these scores mean? (Purina reference chart) ↗")
        }
        // Descending (7 -> 1): Mango's scores cluster at the high end, so leading
        // with them means no scrolling for the common case.
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(7) { index ->
                val score = 7 - index
                val selected = value == score
                Surface(
                    onClick = { onValueChange(score) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier.size(width = 64.dp, height = 78.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    ) {
                        StoolShapeIcon(
                            score = score,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(width = 36.dp, height = 20.dp)
                        )
                        Text(
                            score.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Text(
            text = consistencyDescriptions[value].orEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
