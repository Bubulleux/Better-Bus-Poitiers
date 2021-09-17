package com.example.vitalisapp;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FavoritePresetService extends RemoteViewsService
{
	
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new FavoritePresetWidgetFactory(getApplicationContext(), intent);
	}

	class FavoritePresetWidgetFactory implements RemoteViewsFactory {
		private Context context;
		private int appWidgetId;
		private List<PresetItem> list = new ArrayList<>();
		
		FavoritePresetWidgetFactory(Context context, Intent intent) {
			this.context = context;
			this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		
		@Override
		public void onCreate() {
		
		}
		@Override
		public void onDataSetChanged()
		{
			SharedPreferences prefs = getApplicationContext().getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
			String presetsJson = prefs.getString("presets", null);
			
			PresetItem[] presets;
			if (presetsJson == null)
				presets = new PresetItem[0];
			else
				presets = new Gson().fromJson(presetsJson, PresetItem[].class);
			
			
			list.clear();
			for (PresetItem presetItem : presets)
			{
				if (presetItem.isFavorite)
					list.add(presetItem);
			}
		}
		@Override
		public void onDestroy() {
			//close data source
		}
		@Override
		public int getCount() {
			return list.size();
		}
		
		@Override
		public RemoteViews getViewAt(int position) {
			
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.preset_favorite_item);
			views.setTextViewText(R.id.preset_name, list.get(position).name);
			
			Intent fillIntent = new Intent();
			fillIntent.putExtra("TimetablePreset", (PresetItem) list.get(position));
			views.setOnClickFillInIntent(R.id.preset_name, fillIntent);
			return views;
		}
		@Override
		public RemoteViews getLoadingView() {
			return null;
		}
		@Override
		public int getViewTypeCount() {
			return 1;
		}
		@Override
		public long getItemId(int position) {
			return position;
		}
		@Override
		public boolean hasStableIds() {
			return true;
		}
	}
}
