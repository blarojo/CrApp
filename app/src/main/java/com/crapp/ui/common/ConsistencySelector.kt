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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun ConsistencySelector(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(7) { index ->
                val score = index + 1
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
