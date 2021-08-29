package com.example.vitalisapp;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

import com.google.gson.Gson;

public class ApiInteractor {
	private OkHttpClient client = new OkHttpClient();
	public String token = "";
	Station[] stations;

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
				callback.onResponse(token);
			}
		});
	}

	public void GetAllStations(CallbackObject<Station[]> callback)
	{
		HttpUrl url = HttpUrl.parse("https://api.scoop.airweb.fr/stops/");
		Request request = new Request.Builder()
				.url(url)
				.header("Authorization", token)
				.build();

		System.out.printf("Header: %s \n", request.header("Authorization"));

		client.newCall(request).enqueue(new CallBackHttp() {
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
		HttpUrl url = Objects.requireNonNull(HttpUrl.parse("https://api.scoop.airweb.fr/gtfs/Line/getStationLines.json?networks=[1]"))
				.newBuilder()
				.addQueryParameter("station", station.name)
				.build();


		Request request = GetRequest(url);

		client.newCall(request).enqueue(new CallBackHttp() {
			@Override
			public void onResponseOk(Response response, String body)
			{
				System.out.println(body);
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

		client.newCall(request).enqueue(new CallBackHttp() {
			@Override
			public void onResponseOk(Response response, String body)
			{
				StationLineInfo info = new Gson().fromJson(body, StationLineInfo.class);
				station.id = info.stop_id;

				for (int i = 0; i < info.boarding_ids.aller.length; i++)
					GetStationByName(line.direction.aller[i]).id = info.boarding_ids.aller[i];

				for (int i = 0; i < info.boarding_ids.retour.length; i++)
					GetStationByName(line.direction.retour[i]).id = info.boarding_ids.retour[i];


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
				.addQueryParameter("max", "20")
				.addQueryParameter("stopPoint", station.id);
		if (line != null)
		{
			url.addQueryParameter("line", line.name);
		}

		Request request = GetRequest(url.build());

		client.newCall(request).enqueue(new CallBackHttp() {
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
	@Override
	public void onFailure(@NotNull Call call, @NotNull IOException e) {
		e.printStackTrace();
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
			System.out.printf("Http Request not Successful: %s", response.request().url());
		}
	}

	public abstract void onResponseOk( Response response, String body);

}
