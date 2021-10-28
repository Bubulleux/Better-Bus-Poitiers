package com.bubulle.better_bus_poitiers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;


public class FavoriteWidget extends AppWidgetProvider
{
	
	
	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId)
	{
		
		//Create Intent
		Intent serviceIntent = new Intent(context, FavoritePresetService.class);
		serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
		
		Intent clickIntent = new Intent(context, MainActivity.class);
		PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.favorite_widget);
		views.setRemoteAdapter(R.id.list_view, serviceIntent);
		views.setEmptyView(R.id.list_view, R.id.example_widget_empty_view);
		views.setPendingIntentTemplate(R.id.list_view, clickPendingIntent);
		
		
		
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{
		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds)
		{
			updateAppWidget(context, appWidgetManager, appWidgetId);
		}
	}
	
	
	
	@Override
	public void onEnabled(Context context)
	{
		// Enter relevant functionality for when the first widget is created
	}

	@Override
	public void onDisabled(Context context)
	{
		// Enter relevant functionality for when the last widget is disabled
	}
}