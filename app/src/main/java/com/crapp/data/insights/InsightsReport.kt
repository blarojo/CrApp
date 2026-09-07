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
    /**
     * A short caveat on data completeness (e.g. "Dog-walker-reported walks don't
     * include a consistency score, so walk movements are counted but excluded from
     * consistency correlations.") -- the source data is never complete, since not
     * everyone who walks the dog uses the app, so the skill is expected to say
     * plainly what it couldn't cover rather than silently ignoring the gap.
     */
    val dataCompleteness: String?,
    val insights: List<Insight>,
    val series: List<InsightSeries>
)

enum class InsightSeverity { INFO, NOTABLE }

/**
 * Which of the report's fixed sections an [Insight] belongs to, so the Insights
 * screen can group cards under headings instead of one flat list -- see
 * `.claude/skills/crapp-insights/SKILL.md` §3 for what each section covers.
 * [OTHER] is the fallback for a schema-1 file (predates sections) or an
 * unrecognized value, so older reports still render instead of failing to parse.
 */
enum class InsightSection { BOWEL_PROGRESSION, MOOD_FOOD_CORRELATION, OTHER }

data class Insight(
    val title: String,
    val detail: String,
    val severity: InsightSeverity,
    val section: InsightSection = InsightSection.OTHER
)

enum class SeriesKind { LINE, BAR }

data class InsightSeries(
    val label: String,
    val kind: SeriesKind,
    val unit: String?,
    val points: List<SeriesPoint>
)

data class SeriesPoint(val date: String, val value: Float)
