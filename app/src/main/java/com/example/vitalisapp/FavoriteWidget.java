package com.example.vitalisapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.google.gson.Gson;

import java.util.List;
import java.util.zip.Inflater;


public class FavoriteWidget extends AppWidgetProvider
{
	
	
	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId)
	{

//		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.favorite_widget);
//		//Load Presets
//		SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
//		String presetsJson = prefs.getString("presets", null);
//
//		PresetItem[] presets;
//		if (presetsJson == null)
//			presets = new PresetItem[0];
//		else
//			presets = new Gson().fromJson(presetsJson, PresetItem[].class);
//
//
//		int presetFavoriteCount = 0;
//		LayoutInflater inflater = LayoutInflater.from(context);
//		for (PresetItem preset : presets)
//		{
//			if (preset.isFavorite)
//			{
//				presetFavoriteCount += 1;
//				RemoteViews layout = new RemoteViews(context.getPackageName(), R.layout.preset_favorite_item);
//				layout.setTextViewText(R.id.preset_name, preset.name);
//
//				Intent intent = new Intent(context, NextPassageActivity.class);
//				intent.putExtra("TimetablePreset", preset);
//
//				layout.setOnClickFillInIntent(R.id.preset_name, intent);
//
//				views.addView(R.id.root_layout, layout);
//			}
//		}
//		System.out.printf("Widget Updated Preset Find: %d\n", presetFavoriteCount);
		
		//Create Intent
		Intent serviceIntent = new Intent(context, FavoritePresetService.class);
		serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.favorite_widget);
		views.setRemoteAdapter(R.id.list_view, serviceIntent);
		
		
		
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