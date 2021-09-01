package com.example.vitalisapp;

import android.content.Context;

import java.io.*;

public class FileHelper
{
	public static void Save(String fileName, String fileContent, Context context)
	{
		FileOutputStream fos = null;

		try
		{
			fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			fos.write(fileContent.getBytes());
			fos.close();
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static String Load(String file, Context context)
	{

		FileInputStream fis = null;

		try
		{
			fis = context.openFileInput(file);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader buffer = new BufferedReader(isr);
			StringBuilder builder = new StringBuilder();
			String text;
			while ((text = buffer.readLine()) != null)
			{
				builder.append(text).append("\n");
			}

			fis.close();

			return builder.toString();
		} catch (FileNotFoundException e)
		{
			return null;
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
