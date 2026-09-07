package com.crapp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.crapp.CrAppApplication
import com.crapp.MainActivity
import kotlinx.coroutines.flow.first

/**
 * Home screen widget (docs/backlog.md spec 15): today's bowel-movement/food/energy
 * counts at a glance, plus one-tap "Add BM"/"Add Food" buttons -- so the two most
 * frequent actions don't require opening the app first. Built on Jetpack Glance (not
 * classic `RemoteViews`) to stay in the same Compose idiom as the rest of the app.
 *
 * Refreshed two ways (see [CrAppApplication.onCreate] and [WidgetRefreshWorker]):
 * near-instantly on any relevant data change (a reactive collector, same pattern as
 * this app's existing Wear OS today-count sync), and hourly as a fallback so the
 * summary still resets correctly across a local-midnight rollover even if the
 * widget sits untouched.
 */
class CrAppWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as CrAppApplication
        val counts = computeWidgetTodayCounts(
            movements = app.bowelMovementRepository.allMovements.first(),
            foodEntries = app.foodRepository.allFoodEntries.first(),
            energyEntries = app.energyRepository.allEntries.first()
        )
        provideContent {
            GlanceTheme {
                CrAppWidgetContent(counts)
            }
        }
    }
}

/**
 * A compact, roughly-square 2x2 layout (see `res/xml/crapp_widget_info.xml`):
 * a small rounded "today" chip up top, and the two quick-add buttons stacked full-
 * width underneath (not side by side -- easier to tap accurately at this size, and
 * reads better than two squeezed half-width buttons). The whole stack is centered
 * within the widget's real allocated area rather than pinned to the top, since a
 * launcher's actual 2x2 slot is usually taller than the content needs and
 * top-alignment left an awkward slab of empty space below the content.
 */
@Composable
private fun CrAppWidgetContent(counts: WidgetTodayCounts) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SummaryChip(counts)
            Spacer(modifier = GlanceModifier.height(10.dp))
            WidgetButton(
                icon = "💩",
                label = "Add BM",
                onClick = actionStartActivity(logBowelMovementIntent(context))
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            WidgetButton(
                icon = "🍗",
                label = "Add Food",
                onClick = actionStartActivity(logFoodIntent(context))
            )
        }
    }
}

/**
 * The "today" summary as a small rounded chip (matching `HomeScreen`'s own Today
 * card, which fills a `primaryContainer` card the same way) rather than plain text
 * floating on the widget background -- gives it real visual weight as the headline
 * number it is. Bowel movements always show; food/energy only when non-zero, same
 * "Also today: ..." convention as the app's own Today card, so a quiet day doesn't
 * clutter a widget this small with zeroes.
 */
@Composable
private fun SummaryChip(counts: WidgetTodayCounts) {
    Column(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(16.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "💩 ${counts.bowelMovements}",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        )
        extrasText(counts)?.let { extras ->
            Text(
                text = extras,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/** "🍗 3   ⚡ 1", or null if both are zero (nothing to add below the headline number). */
private fun extrasText(counts: WidgetTodayCounts): String? {
    val parts = buildList {
        if (counts.food > 0) add("🍗 ${counts.food}")
        if (counts.energy > 0) add("⚡ ${counts.energy}")
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("   ")
}

/** A full-width, pill-shaped quick-add button -- stacked, not side by side, so each stays easy to tap accurately at 2x2 size. */
@Composable
private fun WidgetButton(icon: String, label: String, onClick: androidx.glance.action.Action) {
    Text(
        text = "$icon  $label",
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.primary)
            .cornerRadius(20.dp)
            .padding(vertical = 10.dp, horizontal = 12.dp)
            .clickable(onClick),
        style = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = GlanceTheme.colors.onPrimary,
            textAlign = TextAlign.Center
        )
    )
}

/** Reuses [MainActivity.EXTRA_OPEN_LOG_BOWEL_MOVEMENT] -- same deep-link mechanism as a reminder notification tap, not a second path to the same destination. */
private fun logBowelMovementIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        putExtra(MainActivity.EXTRA_OPEN_LOG_BOWEL_MOVEMENT, true)
    }

private fun logFoodIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        putExtra(MainActivity.EXTRA_OPEN_LOG_FOOD, true)
    }
