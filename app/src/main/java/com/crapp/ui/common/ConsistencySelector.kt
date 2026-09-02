package com.crapp.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The Purina Fecal Scoring Chart: the standard 7-point veterinary scale used to
 * describe dog stool consistency, from 1 (very hard/dry) to 7 (liquid, no texture).
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
        LazyRow(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items(7) { index ->
                val score = index + 1
                FilterChip(
                    selected = value == score,
                    onClick = { onValueChange(score) },
                    label = { Text(score.toString()) }
                )
            }
        }
        Text(
            text = consistencyDescriptions[value].orEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
