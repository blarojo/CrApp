package com.crapp.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** System entry point for [CrAppWidget] -- registered in AndroidManifest.xml, backs docs/backlog.md spec 15. */
class CrAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CrAppWidget()
}
