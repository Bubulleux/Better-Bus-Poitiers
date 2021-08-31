package com.example.vitalisapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.*;

public class NextPassageActivity extends AppCompatActivity {

	private TextView stationTextView;
	private ListView nextPassageList;
	private Button refreshBtn;

	private CustomAdapter<Passage> listAdapter;

	private List<Passage> passages = new ArrayList<>();

	private ApiHelper apiHelper;
	private Station station;
	private Line[] lines;
	private boolean load;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_next_passage);
		//Get Extra
		apiHelper = new ApiHelper();
		apiHelper.token =(String) getIntent().getSerializableExtra("Token");
		station = (Station) getIntent().getSerializableExtra("Station");

		//Get View
		stationTextView = findViewById(R.id.station_name);
		nextPassageList = findViewById(R.id.next_passage_list);
		refreshBtn = findViewById(R.id.refresh_btn);

		//Assign Function
		refreshBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view)
			{
				Refresh();
			}
		});
		stationTextView.setText(station.name);

		InitAdapter();
		InitClient();

	}

	private void InitClient()
	{
		apiHelper.GetStationLine(station, new ApiHelper.CallbackObject<Line[]>() {
			@Override
			public void onResponse(Line[] object)
			{
				lines = object;

				apiHelper.GetStationLineId(station, lines[0], new ApiHelper.CallbackObject<StationLineInfo>() {
					@Override
					public void onResponse(StationLineInfo object)
					{
						load = true;
						Refresh();
					}
				});
			}
		});
	}

	public void Refresh()
	{
		if (!load)
		{
			return;
		}
		apiHelper.GetNextPassage(station, null, new ApiHelper.CallbackObject<NextPassages>() {
			@Override
			public void onResponse(NextPassages object)
			{
				passages.clear();

				for (Passage passage : object.realtime)
				{
					System.out.printf("Line: %s, Destination: %s, Time: %s\n", passage.line.line_id, passage.destinationName, passage.expectedDepartureTime);
					passages.add(passage);
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run()
					{
						listAdapter.notifyDataSetChanged();
					}
				});
			}
		});
	}

	public void InitAdapter()
	{
		listAdapter = new CustomAdapter<Passage>(this, passages, new CustomAdapter.IUpdateAdapter<Passage>() {
			@Override
			public View getView(int i, View view, ViewGroup viewGroup, LayoutInflater inflater, List<Passage> list)
			{

				Passage item = list.get(i);
				RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.next_passage_item, null);

				((TextView)layout.findViewById(R.id.direction_txt)).setText(item.destinationName);
				((TextView)layout.findViewById(R.id.line_id)).setText(item.line.line_id);
				((TextView)layout.findViewById(R.id.line_id)).setBackgroundColor(Color.parseColor(item.line.color));
				System.out.println(item.line.color);

				Date time = Helper.getDate(item.expectedDepartureTime);
				//long diff = ChronoUnit.MINUTES.between(Calendar.getInstance().getTime(), time);
				long diff = (time.getTime() - Calendar.getInstance().getTime().getTime()) / (1000 * 60);

				((TextView)layout.findViewById(R.id.time_txt)).setText(new SimpleDateFormat("HH : mm").format(time));
				((TextView)layout.findViewById(R.id.time_relative_txt)).setText(diff + " min");

				return layout;
			}
		});

		nextPassageList.setAdapter(listAdapter);
	}
}