package com.crapp.data.insights

/**
 * An analysis report generated *outside* the app (see the `crapp-insights` Claude
 * skill in `.claude/skills/`, which reads the app's own CSV export and writes one of
 * these) and uploaded back in via the Insights screen -- docs/development-plan.md
 * Phase 8's "CSV export -> Claude analysis skill -> in-app dashboard upload" idea,
 * promoted from future-features.md.
 */
data class InsightsReport(
    val generatedAt: String?,
    val summary: String?,
    val insights: List<Insight>,
    val series: List<InsightSeries>
)

enum class InsightSeverity { INFO, NOTABLE }

data class Insight(
    val title: String,
    val detail: String,
    val severity: InsightSeverity
)

enum class SeriesKind { LINE, BAR }

data class InsightSeries(
    val label: String,
    val kind: SeriesKind,
    val unit: String?,
    val points: List<SeriesPoint>
)

data class SeriesPoint(val date: String, val value: Float)
