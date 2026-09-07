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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.crapp.CrAppApplication
import com.crapp.MainActivity
import kotlinx.coroutines.flow.first

/**
 * Home screen widget (docs/backlog.md spec 15): today's bowel-movement/food/energy
 * counts at a glance, plus one-tap "+BM"/"+Food" buttons -- so the two most frequent
 * actions don't require opening the app first. Tapping the summary chip itself (not
 * either button) opens the app on its normal Home screen. Built on Jetpack Glance
 * (not classic `RemoteViews`) to stay in the same Compose idiom as the rest of the
 * app.
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
            energyEntries = app.energyRepository.allEntries.first(),
            walkEntries = app.walkRepository.allEntries.first()
        )
        provideContent {
            GlanceTheme {
                CrAppWidgetContent(counts)
            }
        }
    }
}

/**
 * A compact 2x2 layout (see `res/xml/crapp_widget_info.xml`): a small rounded
 * "today" chip up top, and the two quick-add buttons side by side underneath.
 * Stacking them (an earlier version of this layout) took more vertical room than
 * some launchers actually grant a 2x2 slot -- side by side with short labels
 * ("+BM"/"+Food" rather than "Add BM"/"Add Food") keeps the whole widget within
 * one real 2x2 footprint rather than only the chip and one button fitting. The
 * whole stack is centered within the widget's real allocated area rather than
 * pinned to the top, since a launcher's actual 2x2 slot is often a bit taller
 * than this content needs.
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
            SummaryChip(counts, onClick = actionStartActivity(openAppIntent(context)))
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WidgetButton(
                    label = "+BM",
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = actionStartActivity(logBowelMovementIntent(context))
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                WidgetButton(
                    label = "+Food",
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = actionStartActivity(logFoodIntent(context))
                )
            }
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
 *
 * Tapping the chip itself (as opposed to either quick-add button) just opens the
 * app on its normal Home screen -- a plain launch, no deep-link extra -- so there's
 * a way into the full dashboard straight from the widget without adding a third
 * button.
 */
@Composable
private fun SummaryChip(counts: WidgetTodayCounts, onClick: androidx.glance.action.Action) {
    Column(
        modifier = GlanceModifier
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(16.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick),
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

/** A pill-shaped quick-add button, one of two side by side sharing the widget's width evenly. */
@Composable
private fun WidgetButton(label: String, modifier: GlanceModifier, onClick: androidx.glance.action.Action) {
    Text(
        text = label,
        modifier = modifier
            .background(GlanceTheme.colors.primary)
            .cornerRadius(20.dp)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .clickable(onClick),
        style = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = GlanceTheme.colors.onPrimary,
            textAlign = TextAlign.Center
        )
    )
}

/** A plain launch of the app, no deep-link extra -- lands on Home like tapping the launcher icon would. */
private fun openAppIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
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
