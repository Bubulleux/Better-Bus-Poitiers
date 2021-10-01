package com.example.vitalisapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

import com.google.gson.Gson;

public class ApiHelper implements Serializable
{
	private final OkHttpClient client = new OkHttpClient();
	public String token = null;
	Station[] stations;
	private Context context;
	
	public ApiHelper(Context context)
	{
		this.context = context;
		CheckConnection(context);
	}
	
	public void GetToken(CallbackToken callback)
	{
		Request request = new Request.Builder().url("https://www.vitalis-poitiers.fr/services/").build();
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(@NotNull Call call, @NotNull IOException e) {
				e.printStackTrace();
			}

			@Override
			public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException
			{
				String body = response.body().string();

				int tokenIndex = body.indexOf("token");
				int end_token = body.indexOf("',", tokenIndex);
				token = "Bearer " + body.substring(tokenIndex + 8, end_token);
				if (callback != null)
				{
					callback.onResponse(token);
				}
			}
		});
	}
	
	public boolean CheckConnection(Context context)
	{
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		boolean is_connected;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Network nw = connectivityManager.getActiveNetwork();
			if (nw == null) is_connected = false;
			
			NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
			
			is_connected =  actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
					actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
					actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
					actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
		}
		else {
			NetworkInfo nwInfo = connectivityManager.getActiveNetworkInfo();
			is_connected = nwInfo != null && nwInfo.isConnected();
		}
		
		if (!is_connected) {
			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
			alertBuilder.setMessage(R.string.connection_error);
			alertBuilder.setPositiveButton("Ok", null);
			alertBuilder.create().show();
		}
		return  is_connected;
	}


	public void GetAllStations(CallbackObject<Station[]> callback)
	{
		GetAllStations(callback, false);
	}

	public void GetAllStations(CallbackObject<Station[]> callback, boolean force)
	{
		if (stations != null && !force)
		{
			callback.onResponse(stations);
			return;
		}

		HttpUrl url = HttpUrl.parse("https://api.scoop.airweb.fr/stops/");
		Request request = new Request.Builder()
				.url(url)
				.header("Authorization", token)
				.build();


		client.newCall(request).enqueue(new CallBackHttp(context, this) {
			@Override
			public void onResponseOk(Response response, String body)
			{
				stations = new Gson().fromJson(body, Station[].class);

				callback.onResponse(stations);

			}
		});
	}

	public void GetStationLine(Station station, CallbackObject<Line[]> callback)
	{
		GetStationLine(station.name, callback);
	}

	public void GetStationLine(String stationName, CallbackObject<Line[]> callback)
	{
		HttpUrl url = Objects.requireNonNull(HttpUrl.parse("https://api.scoop.airweb.fr/gtfs/Line/getStationLines.json?networks=[1]"))
				.newBuilder()
				.addQueryParameter("station", stationName)
				.build();


		Request request = GetRequest(url);

		client.newCall(request).enqueue(new CallBackHttp(context, this) {
			@Override
			public void onResponseOk(Response response, String body)
			{
				Lines lines = new Gson().fromJson(body, Lines.class);
				callback.onResponse(lines.lines);
			}
		});
	}

	public void GetStationLineId(Station station, Line line,  CallbackObject<StationLineInfo> callback)
	{
		HttpUrl url = Objects.requireNonNull(HttpUrl.parse("https://api.scoop.airweb.fr/gtfs/Station/getBoardingIDs.json?networks=[1]"))
				.newBuilder()
				.addQueryParameter("line", line.name)
				.addQueryParameter("station", station.name)
				.build();


		Request request = GetRequest(url);

		client.newCall(request).enqueue(new CallBackHttp(context, this) {
			@Override
			public void onResponseOk(Response response, String body)
			{
				StationLineInfo info = new Gson().fromJson(body, StationLineInfo.class);
				station.id = info.stop_id;

				for (int i = 0; i < info.boarding_ids.aller.length; i++)
					GetStationByName(line.direction.aller[i]).id = info.boarding_ids.aller[i];

				for (int i = 0; i < info.boarding_ids.retour.length; i++)
					GetStationByName(line.direction.retour[i]).id = info.boarding_ids.retour[i];

				if (callback != null)
					callback.onResponse(info);
			}
		});
	}

//	public void GetTimeTable(Station station, CallbackObject<Line[]> callback)
//	{
//		HttpUrl url = Objects.requireNonNull(HttpUrl.parse("https://api.scoop.airweb.fr/gtfs/Line/getStationLines.json?networks=[1]"))
//				.newBuilder()
//				.addQueryParameter("station", station.name)
//				.build();
//
//
//		Request request = GetRequest(url);
//
//		client.newCall(request).enqueue(new CallBackHttp() {
//			@Override
//			public void onResponseOk(Response response, String body)
//			{
//				Line[] lines = new Gson().fromJson(body, Line[].class);
//				callback.onResponse(lines);
//			}
//		});
//	}

	public void GetNextPassage(Station station, Line line, CallbackObject<NextPassages> callback)
	{
		if (station.id == "")
		{
			System.out.println("Error station id is undefined");
		}
		HttpUrl.Builder url = Objects.requireNonNull(HttpUrl.parse("https://api.scoop.airweb.fr/gtfs/SIRI/getSIRIWithErrors.json?networks=[1]"))
				.newBuilder()
				.addQueryParameter("max", "50")
				.addQueryParameter("stopPoint", station.id);
		if (line != null)
		{
			url.addQueryParameter("line", line.name);
		}

		Request request = GetRequest(url.build());

		client.newCall(request).enqueue(new CallBackHttp(context, this) {
			@Override
			public void onResponseOk(Response response, String body) {
				NextPassages nextPassages = new Gson().fromJson(body, NextPassages.class);

				callback.onResponse(nextPassages);
			}
		});

	}

	public Station GetStationByName(String name)
	{
		for(Station station : stations)
		{
			if (station.name == name)
			{
				return station;
			}
		}
		return null;
	}


	private Request GetRequest(HttpUrl url)
	{
		return new Request.Builder()
				.url(url)
				.header("Authorization", token)
				.build();
	}



	interface CallbackToken
	{
		void onResponse(String token);
	}


	interface CallbackObject<T>
	{
		void onResponse(T object);
	}

}

abstract class CallBackHttp implements Callback
{
	Context context;
	ApiHelper apiHelper;
	public CallBackHttp(Context context, ApiHelper apiHelper)
	{
		super();
		this.context = context;
		this.apiHelper = apiHelper;
	}
	
	
	@Override
	public void onFailure(@NotNull Call call, @NotNull IOException e)
	{
		e.printStackTrace();
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
		alertBuilder.setTitle("Error");
		alertBuilder.setMessage(e.toString());
		alertBuilder.setPositiveButton("Ok", null);
		((AppCompatActivity) context).runOnUiThread(() -> alertBuilder.create().show());
	}

	@Override
	public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException
	{
		if (response.isSuccessful())
		{
			onResponseOk(response, Objects.requireNonNull(response.body()).string());
		}
		else
		{
			System.out.printf("Http Request not Successful: %s \n %s \n", response.request().url(), response.body());
			if (response.code() == 401)
			{
				apiHelper.GetToken(null);
			}
			AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
			alertBuilder.setTitle("Error");
			alertBuilder.setMessage(response.body().string());
			alertBuilder.setPositiveButton("Ok", null);
			((AppCompatActivity) context).runOnUiThread(() -> alertBuilder.create().show());
		}
	}

	public abstract void onResponseOk( Response response, String body);

}
