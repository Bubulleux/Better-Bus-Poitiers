package com.example.vitalisapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.*;

public class NextPassageActivity extends AppCompatActivity {

	private TextView stationTextView;
	private ListView nextPassageList;
	private TextView emptyListTxt;
	private ProgressBar progressBar;
	private FrameLayout listFrameLayout;
	private Button refreshBtn;
	private Button seeAllBtn;

	private CustomAdapter<Passage> listAdapter;

	private List<Passage> passages = new ArrayList<>();

	private ApiHelper apiHelper;
	private Station station;
	private PresetItem preset;
	private Line[] lines;
	private boolean load;
	
	/* Intent Extra:
		"Token": api Token (String)
		"Station": (Station)
		"TimetablePreset": (PresetItem)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		Toast.makeText(this, "Next Passage Loaded", Toast.LENGTH_SHORT).show();
		
		
		setContentView(R.layout.activity_next_passage);
		//Get Extra
		apiHelper = new ApiHelper(this);
		apiHelper.token =(String) getIntent().getSerializableExtra("Token");
		
		
		
		station = (Station) getIntent().getSerializableExtra("Station");
		preset = null;

		if (station == null)
		{
			preset = (PresetItem) getIntent().getSerializableExtra("TimetablePreset");
			station = new Station();
			station.name = preset.stationName;
		}

		//Get View
		stationTextView = findViewById(R.id.station_name);
		nextPassageList = findViewById(R.id.next_passage_list);
		refreshBtn = findViewById(R.id.refresh_btn);
		emptyListTxt = findViewById(R.id.next_passage_list_empty_txt);
		progressBar = findViewById(R.id.progressBar2);
		listFrameLayout = findViewById(R.id.list_frame_layout);
		
		nextPassageList.setEmptyView(emptyListTxt);

		seeAllBtn = findViewById(R.id.see_all_btn);
		
		setLoading(false);
		
		((Button)findViewById(R.id.go_back_btn)).setOnClickListener((View view) ->
		{
			finish();
		});
		
		if (preset == null)
		{
			((ViewGroup) seeAllBtn.getParent()).removeView(seeAllBtn);
		}
		else
		{
			seeAllBtn.setOnClickListener((View view) ->
			{
				if (preset == null)
					return;
				preset = null;
				
				((ViewGroup) view.getParent()).removeView(view);

				Refresh();
			});
		}

		//Assign Function
		refreshBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view)
			{
				Refresh();
			}
		});
		
		((Button) findViewById(R.id.fix_timetable_btn)).setOnClickListener((View view) ->
		{
			Intent intent = new Intent(this, ActivityFixTimeTable.class);
			intent.putExtra("Station", station);
			intent.putExtra("Token", apiHelper.token);
			startActivity(intent);
			finish();
		});
		
		stationTextView.setText(station.name);

		InitAdapter();
		InitClient();
	}
	

	
	private void setLoading(Boolean value)
	{
		progressBar.setVisibility(value ? View.VISIBLE : View.INVISIBLE);
		runOnUiThread(() -> emptyListTxt.setVisibility(value ? View.INVISIBLE : View.VISIBLE));
	}
	
	
	private void InitClient()
	{
		ApiHelper.CallbackToken callbackToken = (String token) ->
		{
			apiHelper.GetStationLine(station, object ->
			{
				lines = object;
				
				apiHelper.GetStationLineId(station, lines[0], object1 ->
				{
					load = true;
					Refresh();
				});
			});
		};
		
		setLoading(true);
		
		if (apiHelper.token == null)
		{
			apiHelper.GetToken(callbackToken);
		}
		else
			callbackToken.onResponse(apiHelper.token);
		
		
	}

	public void Refresh()
	{
		if (!load)
		{
			return;
		}
		
		passages.clear();
		runOnUiThread(() -> listAdapter.notifyDataSetChanged());
		setLoading(true);
		
		apiHelper.GetNextPassage(station, null, object ->
		{
			passages.clear();

			for (Passage passage : object.realtime)
			{
				System.out.printf("Line: %s, Destination: %s, Time: %s\n", passage.line.line_id, passage.destinationName, passage.expectedDepartureTime);
				if (preset != null)
				{
					boolean breakAll = false;
					for (DirectionPreset direction : preset.directions)
					{
						for (String terminus: direction.terminus)
						{
							if (passage.line.line_id.equals(direction.line_id) && passage.destinationName.equals(terminus))
							{
								passages.add(passage);
								breakAll = true;
								break;
							}
						}
						if (breakAll)
							break;
					}
				}
				else
				{
					passages.add(passage);
				}
			}
			setLoading(false);
			runOnUiThread(() -> listAdapter.notifyDataSetChanged());
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