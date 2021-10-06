package com.example.vitalisapp;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.io.Serializable;
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
		stations = (Station[]) getIntent().getSerializableExtra("Stations");
		String[] historyName = (String[]) getIntent().getSerializableExtra("History");
		if (historyName != null)
		{
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

		System.out.printf("Station Count: %d\n", stations.length);

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

		adapter = new CustomAdapter<Station>(this, stationsFind, (i, view, viewGroup, inflater, list) ->
		{
			Station item = list.get(i);
			LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.station_item, null);
			((TextView)layout.findViewById(R.id.station_name)).setText(item.name);
			layout.setOnClickListener(view1 -> GetInfo(item));
			return layout;
		});

		stationListView.setAdapter(adapter);
		UpdateList("");
	}
	
	public void UpdateList(CharSequence charSequence)
	{
		stationsFind.clear();
		if (charSequence.length() == 0)
		{
			stationsFind.addAll(history);
			System.out.printf("Station in history %d \n", history.size());
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
		
		System.out.printf("Size Station Find: %d\n", stationsFind.size());
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
