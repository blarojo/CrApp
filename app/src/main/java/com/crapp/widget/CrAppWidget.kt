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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
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

@Composable
private fun CrAppWidgetContent(counts: WidgetTodayCounts) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        Text(
            text = summaryText(counts),
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GlanceTheme.colors.onBackground
            )
        )
        Spacer(modifier = GlanceModifier.height(12.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            WidgetButton(
                label = "Add BM",
                modifier = GlanceModifier.defaultWeight(),
                onClick = actionStartActivity(logBowelMovementIntent(context))
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            WidgetButton(
                label = "Add Food",
                modifier = GlanceModifier.defaultWeight(),
                onClick = actionStartActivity(logFoodIntent(context))
            )
        }
    }
}

/**
 * "💩 6 · 🍗 1 · ⚡ 1" -- matches `HomeScreen`'s own "Today" hero card: the
 * bowel-movement count always shows, food/energy only when non-zero (same
 * "Also today: ..." convention), so a quiet day doesn't clutter the widget with
 * zeroes.
 */
private fun summaryText(counts: WidgetTodayCounts): String {
    val parts = buildList {
        add("💩 ${counts.bowelMovements}")
        if (counts.food > 0) add("🍗 ${counts.food}")
        if (counts.energy > 0) add("⚡ ${counts.energy}")
    }
    return parts.joinToString(" · ")
}

@Composable
private fun WidgetButton(label: String, modifier: GlanceModifier, onClick: androidx.glance.action.Action) {
    Text(
        text = label,
        modifier = modifier
            .background(GlanceTheme.colors.primary)
            .cornerRadius(8.dp)
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .clickable(onClick),
        style = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = GlanceTheme.colors.onPrimary,
            textAlign = androidx.glance.text.TextAlign.Center
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
