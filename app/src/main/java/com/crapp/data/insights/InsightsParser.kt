package com.crapp.data.insights

import org.json.JSONObject

/**
 * Parses the JSON produced by the `crapp-insights` Claude skill into an
 * [InsightsReport]. See that skill's SKILL.md for the authoritative schema this
 * mirrors.
 */
object InsightsParser {
    /** @throws IllegalArgumentException if [json] doesn't match the expected schema. */
    fun parse(json: String): InsightsReport {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw IllegalArgumentException("Not a valid insights file (not valid JSON).", e)
        }
        if (!root.has("schemaVersion")) {
            throw IllegalArgumentException("Not a valid insights file (missing schemaVersion).")
        }

        val insights = root.optJSONArray("insights")?.let { array ->
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Insight(
                    title = o.getString("title"),
                    detail = o.optString("detail", ""),
                    severity = when (o.optString("severity", "info").lowercase()) {
                        "notable" -> InsightSeverity.NOTABLE
                        else -> InsightSeverity.INFO
                    }
                )
            }
        }.orEmpty()

        val series = root.optJSONArray("series")?.let { array ->
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val points = o.optJSONArray("points")?.let { pointsArray ->
                    (0 until pointsArray.length()).map { p ->
                        val point = pointsArray.getJSONObject(p)
                        SeriesPoint(date = point.getString("date"), value = point.getDouble("value").toFloat())
                    }
                }.orEmpty()
                InsightSeries(
                    label = o.getString("label"),
                    kind = when (o.optString("kind", "line").lowercase()) {
                        "bar" -> SeriesKind.BAR
                        else -> SeriesKind.LINE
                    },
                    unit = if (o.has("unit") && !o.isNull("unit")) o.optString("unit") else null,
                    points = points
                )
            }
        }.orEmpty()

        return InsightsReport(
            generatedAt = if (root.has("generatedAt") && !root.isNull("generatedAt")) root.optString("generatedAt") else null,
            summary = if (root.has("summary") && !root.isNull("summary")) root.optString("summary") else null,
            insights = insights,
            series = series
        )
    }
}
