package com.example.vitalisapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.Toast;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FavoritePresetService extends WidgetServiceCustom<PresetItem>
{
	
	FavoritePresetService()
	{
		super();
		Toast.makeText(getApplicationContext(), "Favorite Preset Service Intencied", Toast.LENGTH_LONG);
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
		String presetsJson = prefs.getString("presets", null);
		
		PresetItem[] presets;
		if (presetsJson == null)
			presets = new PresetItem[0];
		else
			presets = new Gson().fromJson(presetsJson, PresetItem[].class);
		
		list = Arrays.asList(presets);
		
		remoteViewCustomInterface = new GetRemoteViewCustom<PresetItem>()
		{
			@Override
			public RemoteViews getViewAt(int position, List<PresetItem> list, Context context, int id)
			{
				RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.preset_favorite_item);
				views.setTextViewText(R.id.preset_name, list.get(position).name);
				System.out.println("Get View");
				return views;
			}
			
			@Override
			public RemoteViews getLoadingViews(Context context, int id)
			{
				return null;
			}
		};
	}
}
