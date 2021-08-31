package com.example.vitalisapp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Helper
{

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
}
