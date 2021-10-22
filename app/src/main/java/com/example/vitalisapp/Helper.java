package com.example.vitalisapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.Inflater;

public class Helper
{
	
	public static final String PREF_NAME = "com.example.BetterVitalis_Pref";
	
	public static Date getDate(String rawDate)
	{
		// 2021-08-29T18:27:17+0200 -> 29/08/2021  18:27:17
		rawDate = rawDate.replace("T", ",");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");

		try
		{
			return formatter.parse(rawDate);
		} catch (ParseException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static <T> T loadPrefJson(String key, Class<T> tClass, Context context)
	{
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		String dataJson = prefs.getString(key, null);
		
		if (dataJson != null)
		{
			return new Gson().fromJson(dataJson, tClass);
		}
		return null;
	}
	
	public static <T> void savePrefJson(T data, String key, Context context)
	{
		String dataJson = new Gson().toJson(data);
		
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, dataJson);
		editor.apply();
	}
	
	public static void moreInfoStation(Context context, Station station)
	{
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		LayoutInflater inflater = LayoutInflater.from(context);
		
		LinearLayout alertDialogView = (LinearLayout) inflater.inflate(R.layout.station_info_dialog, null);
		alertDialogBuilder.setView(alertDialogView);
		
		AlertDialog alertDialog = alertDialogBuilder.create();
		
		((TextView) alertDialogView.findViewById(R.id.station_name)).setText(station.name);
		((Button) alertDialogView.findViewById(R.id.next_passage_btn)).setOnClickListener((View view) ->
		{
			Intent intent = new Intent(context, NextPassageActivity.class);
			intent.putExtra("Station", station);
			context.startActivity(intent);
			alertDialog.cancel();
		});
		
		((Button) alertDialogView.findViewById(R.id.fix_timetable_btn)).setOnClickListener((View view) ->
		{
			Intent intent = new Intent(context, ActivityFixTimeTable.class);
			intent.putExtra("Station", station);
			context.startActivity(intent);
			alertDialog.cancel();
		});
		
		((Button) alertDialogView.findViewById(R.id.see_in_map_btn)).setOnClickListener((View view) ->
		{
			Uri uri = Uri.parse(String.format("https://www.google.fr/maps/dir//%f,%f/", station.lat, station.lng));
			
			Intent intent = new Intent(context, ActivityFixTimeTable.class);
			intent.putExtra("Station", station);
			context.startActivity(intent);
			alertDialog.cancel();
		});
		
		((Button) alertDialogView.findViewById(R.id.close_btn)).setOnClickListener((View view) -> alertDialog.cancel());
	}
}
