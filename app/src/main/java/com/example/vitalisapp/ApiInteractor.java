package com.example.vitalisapp;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;

public class ApiInteractor {
	private OkHttpClient client = new OkHttpClient();
	public String token = "";

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

	public void GetAllStations(CallBackJson callback)
	{
		HttpUrl url = HttpUrl.parse("https://api.scoop.airweb.fr/stops/");
		Request request = new Request.Builder()
				.url(url)
				.header("Authorization", token)
				.build();

		System.out.printf("Header: %s \n", request.header("Authorization"));

		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(@NotNull Call call, @NotNull IOException e) {
				e.printStackTrace();
			}

			@Override
			public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
				System.out.println(response.body().string());

			}
		});
	}

	interface CallbackToken
	{
		void onResponse(String token);
	}

	interface CallBackJson
	{
		void onResponse(String json);
	}
}
