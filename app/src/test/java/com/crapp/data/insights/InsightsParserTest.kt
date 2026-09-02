package com.crapp.data.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsParserTest {

    @Test
    fun parse_fullReport_readsSummaryInsightsAndSeries() {
        val json = """
            {
              "schemaVersion": 1,
              "generatedAt": "2026-09-02T21:00:00Z",
              "summary": "Overall stable.",
              "insights": [
                { "title": "Looser stools after chicken treats", "detail": "Seen 3 times.", "severity": "notable" },
                { "title": "No strong pattern with medication timing", "severity": "info" }
              ],
              "series": [
                {
                  "label": "Consistency over time",
                  "kind": "line",
                  "unit": "Purina score (1-7)",
                  "points": [ { "date": "2026-08-01", "value": 6 }, { "date": "2026-08-03", "value": 5 } ]
                },
                {
                  "label": "Movements per week",
                  "kind": "bar",
                  "points": [ { "date": "2026-W31", "value": 9 } ]
                }
              ]
            }
        """.trimIndent()

        val report = InsightsParser.parse(json)

        assertEquals("2026-09-02T21:00:00Z", report.generatedAt)
        assertEquals("Overall stable.", report.summary)

        assertEquals(2, report.insights.size)
        assertEquals("Looser stools after chicken treats", report.insights[0].title)
        assertEquals(InsightSeverity.NOTABLE, report.insights[0].severity)
        assertEquals("Seen 3 times.", report.insights[0].detail)
        assertEquals(InsightSeverity.INFO, report.insights[1].severity)
        assertEquals("", report.insights[1].detail) // detail is optional

        assertEquals(2, report.series.size)
        val consistencySeries = report.series[0]
        assertEquals(SeriesKind.LINE, consistencySeries.kind)
        assertEquals("Purina score (1-7)", consistencySeries.unit)
        assertEquals(2, consistencySeries.points.size)
        assertEquals("2026-08-01", consistencySeries.points[0].date)
        assertEquals(6f, consistencySeries.points[0].value)

        val frequencySeries = report.series[1]
        assertEquals(SeriesKind.BAR, frequencySeries.kind)
        assertEquals(null, frequencySeries.unit)
    }

    @Test
    fun parse_minimalReport_missingOptionalFieldsDefaultSensibly() {
        val json = """{ "schemaVersion": 1 }"""

        val report = InsightsParser.parse(json)

        assertEquals(null, report.generatedAt)
        assertEquals(null, report.summary)
        assertTrue(report.insights.isEmpty())
        assertTrue(report.series.isEmpty())
    }

    @Test
    fun parse_unknownSeverityOrKind_fallsBackToDefault() {
        val json = """
            {
              "schemaVersion": 1,
              "insights": [ { "title": "x", "severity": "urgent" } ],
              "series": [ { "label": "y", "kind": "pie", "points": [] } ]
            }
        """.trimIndent()

        val report = InsightsParser.parse(json)

        assertEquals(InsightSeverity.INFO, report.insights[0].severity)
        assertEquals(SeriesKind.LINE, report.series[0].kind)
    }

    @Test
    fun parse_missingSchemaVersion_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            InsightsParser.parse("""{"summary": "no marker"}""")
        }
    }

    @Test
    fun parse_notJson_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            InsightsParser.parse("definitely not json")
        }
    }
}
