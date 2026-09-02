package com.crapp.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

data class AddEntryOption(
    val label: String,
    val onClick: () -> Unit
)

/**
 * Quick-add entry point (docs/development-plan.md Phase 7): a single "+" FAB that
 * expands into one labeled mini-FAB per loggable entry type, replacing the three
 * separate "Log X" buttons that used to sit in the Dashboard body.
 */
@Composable
fun AddEntryFab(
    options: List<AddEntryOption>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 45f else 0f, label = "addFabRotation")

    Column(horizontalAlignment = Alignment.End, modifier = modifier) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    ExtendedFloatingActionButton(
                        onClick = {
                            expanded = false
                            option.onClick()
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(option.label)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        FloatingActionButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = if (expanded) "Close add menu" else "Add entry",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}
