package com.aegissoft.dolphyx.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aegissoft.dolphyx.DolphyRoutes
import com.aegissoft.dolphyx.MainActivity
import com.aegissoft.dolphyx.R

/**
 * Safe home-screen shortcut widget. It never starts a background radio by itself;
 * every button opens the app at a specific legitimate screen.
 */
class DolphyXQuickWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.dolphyx_widget)
            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
            views.setTextViewText(R.id.widget_subtitle, context.getString(R.string.widget_subtitle))
            views.setTextViewText(R.id.widget_ir, context.getString(R.string.widget_ir))
            views.setTextViewText(R.id.widget_nfc, context.getString(R.string.widget_nfc))
            views.setTextViewText(R.id.widget_open, context.getString(R.string.widget_open))
            views.setOnClickPendingIntent(R.id.widget_ir, activityIntent(context, DolphyRoutes.IR))
            views.setOnClickPendingIntent(R.id.widget_nfc, activityIntent(context, DolphyRoutes.NFC))
            views.setOnClickPendingIntent(R.id.widget_open, activityIntent(context, null))
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun activityIntent(context: Context, route: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (route != null) putExtra(EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context,
            route?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val EXTRA_ROUTE = "dolphyx_route"
    }
}
