package com.bubulle.better_bus_poitiers;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityFindStation extends AppCompatActivity {

	public Station[] stations;
	public List<Station> history = new ArrayList<>();
	public List<Station> stationsFind = new ArrayList<>();

	public ListView stationListView;
	public EditText stationSearchInput;

	public CustomAdapter<Station> adapter;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_find_station);
		
		
		stationListView = findViewById(R.id.station_list);
		stationSearchInput = findViewById(R.id.station_search_input);


		stationSearchInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
			{
				UpdateList(charSequence);
			}

			@Override
			public void afterTextChanged(Editable editable) { }
		});

		adapter = new CustomAdapter<>(this, stationsFind, (i, view, viewGroup, inflater, list) ->
		{
			Station item = list.get(i);
			LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.station_item, null);
			((TextView)layout.findViewById(R.id.station_name)).setText(item.name);
			layout.setOnClickListener(view1 -> GetInfo(item));
			return layout;
		});

		stationListView.setAdapter(adapter);
		
		
		ApiHelper apiHelper = new ApiHelper(this);
		
		
		ApiHelper.CallbackToken tokenCallback = (String token) ->
				apiHelper.GetAllStations(object ->
				{
					stations = object;
					UpdateHistory();
					runOnUiThread(() ->
					{
						findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
						UpdateList("");
					});
				});
		
		if (ApiHelper.token == null)
			apiHelper.GetToken(tokenCallback);
		else
			tokenCallback.onResponse(ApiHelper.token);
		
	}
	
	public void UpdateHistory()
	{
		String[] historyName = (String[]) getIntent().getSerializableExtra("History");
		if (historyName == null)
			return;
		
		System.out.printf("Station in historyName %d \n", historyName.length);
		for (String historyStation : historyName)
		{
			for (Station station : stations)
			{
				if (station.name.equals(historyStation))
					history.add(station);
			}
		}
	}
	
	public void UpdateList(CharSequence charSequence)
	{
		stationsFind.clear();
		if (charSequence.length() == 0 && history.size() != 0)
		{
			stationsFind.addAll(history);
		}
		else
		{
			for (Station station : stations)
			{
				if (station.name.toLowerCase(Locale.ROOT).contains(String.valueOf(charSequence).toLowerCase(Locale.ROOT)))
				{
					stationsFind.add(station);
				}
			}
		}
		
		adapter.list = stationsFind;
		adapter.notifyDataSetChanged();
	}

	public void GetInfo(Station station)
	{
		Intent intent = new Intent();
		intent.putExtra("Station", station);
		setResult(RESULT_OK, intent);
		finish();
	}
}
