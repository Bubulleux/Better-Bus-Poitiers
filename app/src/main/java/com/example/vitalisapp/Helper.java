package com.example.vitalisapp;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
}
