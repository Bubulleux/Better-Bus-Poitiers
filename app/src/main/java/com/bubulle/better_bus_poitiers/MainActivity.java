package com.bubulle.better_bus_poitiers;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
	public static boolean is_running = false;
	
	
	private ApiHelper apiHelper;
	private final List<PresetItem> presets = new ArrayList<>();
	public LayoutInflater inflater;
	

	private CustomAdapter<PresetItem> presetListAdapter;

	ActivityResultLauncher<Intent> activityResultLauncher;
	ActivityResultLauncher<Intent> activityEditPreset;
	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		is_running = true;
		
        setContentView(R.layout.activity_main);
		apiHelper = new ApiHelper(this);
		LoadPresets();

		inflater = LayoutInflater.from(this);
		ListView presetListView = findViewById(R.id.preset_list);
		
		//Check Widget Redirection
		checkRedirection(getIntent());

		//Get Api token
		apiHelper.GetToken(null);

		//Initiate Activity Result
		activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
			result ->
			{
				if (result.getResultCode() == RESULT_OK && result.getData() != null)
				{
					//Config and Show Window
					Bundle bundle = result.getData().getExtras();
					Station station = (Station) bundle.get("Station");
					Helper.saveInHistory(this, station);
					Helper.moreInfoStation(this, station);
				}
				else
					System.out.println("Error to receive Result");
			});

		activityEditPreset = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
			result ->
			{
				if (result.getResultCode() == RESULT_OK && result.getData() != null)
				{
					Bundle bundle = result.getData().getExtras();

					PresetItem preset = (PresetItem) bundle.get("Preset");
					int index = bundle.getInt("Index");
					int newPos = bundle.getInt("NewPos");



					System.out.printf("Index %d, preset %b\n", index, preset == null);

					if (preset == null)
						presets.remove(index);
					else
						presets.set(index, preset);

					//Move
					if (preset != null)
					{
						PresetItem savePreset = presets.get(index);
						presets.remove(index);
						presets.add(newPos, savePreset);
					}
					

					SavePreset();
					runOnUiThread(() -> presetListAdapter.notifyDataSetChanged());
					UpdateWidgets();
				}
			});

		if (presetListView == null)
		{
			System.out.println("Preset List view not found");
			return;
		}

		//Init Btn
		presetListView.setEmptyView(findViewById(R.id.empty_list_preset_text));
		
		findViewById(R.id.button_other).setOnClickListener(view -> ((MainActivity)view.getContext()).otherTimetable());

		findViewById(R.id.button_new).setOnClickListener((View view) ->
		{
			PresetItem preset = new PresetItem("", "", new DirectionPreset[0], false);
			presets.add(preset);
			UpdatePreset(presets.size() - 1);
		});

		//Initialize Preset List
		presetListAdapter = new CustomAdapter<>(this, presets, (i, view, viewGroup, inflater, list) ->
		{
			
			view = inflater.inflate(R.layout.preset_item, null);
			PresetItem item = list.get(i);
			
			view.setOnLongClickListener((View viewBtn) ->
			{
				UpdatePreset(i);
				return true;
			});
			
			view.setOnClickListener((View viewBtn) -> GoToTimetable(item));
			
			((TextView) view.findViewById(R.id.name_txt)).setText(item.name);
			
			if (item.isFavorite)
				((TextView) view.findViewById(R.id.name_txt)).setTypeface(null, Typeface.BOLD);
			
			((TextView) view.findViewById(R.id.station_text)).setText(item.stationName);
			
			GridLayout lineGrid =  view.findViewById(R.id.line_list);
			
			//Add line
			List<String> lineAlreadyAdded = new ArrayList<>();
			
			for (DirectionPreset direction : item.directions)
			{
				boolean contain = false;
				for (String line : lineAlreadyAdded)
				{
					if (line.equals(direction.line_id))
					{
						contain = true;
						break;
					}
				}
				
				if (contain)
					continue;
				
				LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.line_item, null);
				TextView textView = layout.findViewById(R.id.line_id);
				
				textView.setText(direction.line_id);
				textView.setBackgroundColor(Color.parseColor(direction.line_color));
				
				int color = Helper.getTextContrast(direction.line_color);
				textView.setTextColor(color);
				
				lineAlreadyAdded.add(direction.line_id);
				lineGrid.addView(layout);
			}
			
			return view;
		});
		

		presetListView.setAdapter(presetListAdapter);
    }
	
	
	
	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		checkRedirection(intent);
	}
	
	private void checkRedirection(Intent intent)
	{
		//Get Auto Load next Passage
		PresetItem presetStart = (PresetItem) intent.getSerializableExtra("StartNextPassage");
		
		if (presetStart != null)
			GoToTimetable(presetStart);
	}
	
	@Override
	public void onStop()
	{
		is_running = false;
		super.onStop();
	}
	
	private void UpdateWidgets()
	{
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
		int[] ids = widgetManager.getAppWidgetIds(new ComponentName(this, FavoriteWidget.class));
		if (ids.length > 0)
		{
			new FavoriteWidget().onUpdate(this, widgetManager, ids);
		}
	}

	private void SavePreset()
	{
		Helper.savePrefJson(presets.toArray(),"presets", this);
		
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(new ComponentName(this, FavoriteWidget.class)), R.id.list_view);
		
		Toast.makeText(this, getString(R.string.preset_saved), Toast.LENGTH_SHORT).show();
	}

	private void LoadPresets()
	{
		PresetItem[] dataPresets = Helper.loadPrefJson("presets", PresetItem[].class, this);
		
		if (dataPresets == null)
			return;
		
		presets.clear();
		presets.addAll(Arrays.asList(dataPresets));
	}

	public void otherTimetable()
	{
		if (!ApiHelper.CheckConnection(this))
			return;
		
		Intent intent = new Intent(this, ActivityFindStation.class);
		String[] stationsHistoryArray = Helper.loadPrefJson("station_history", String[].class, this);
		if (stationsHistoryArray != null)
		{
			intent.putExtra("History", stationsHistoryArray);
			System.out.println(stationsHistoryArray.length);
		}
		activityResultLauncher.launch(intent);
	}

	public void GoToTimetable(PresetItem timetablePreset)
	{
		if (!ApiHelper.CheckConnection(this))
			return;
		
		Intent intent = new Intent(this, NextPassageActivity.class);
		intent.putExtra("TimetablePreset", timetablePreset);
		startActivity(intent);
	}
	

	public void UpdatePreset(int i)
	{
		if (!ApiHelper.CheckConnection(this))
			return;
		
		Intent intent = new Intent(this, PresetEditionActivity.class);
		intent.putExtra("Index", i);
		intent.putExtra("Preset", presets.get(i));
		intent.putExtra("PresetsCount", presets.size());
		activityEditPreset.launch(intent);
	}


	private void TestApiHelper()
	{
		apiHelper.GetToken(token -> {
			System.out.println("Request token Success");

			apiHelper.GetAllStations(object ->
			{
				for (Station station: object)
				{
					System.out.println(station.name);
				}
				Station station = object[869];
				apiHelper.GetStationLine(station, object1 ->
				{
					for(Line line : object1)
					{
						System.out.printf("%s  %s  %s\n", line.name, line.line_id, line.color);
					}
					apiHelper.GetStationLineId(station, object1[0], object112 ->
					{
						System.out.println(object112.stop_id);

						apiHelper.GetNextPassage(station, null, object11 ->
						{
							for (Passage nextPassages: object11.realtime)
							{
								System.out.printf("%s %s\n", nextPassages.destinationName, nextPassages.expectedDepartureTime);
							}

							ListView presetListView = findViewById(R.id.preset_list);
							System.out.println(presetListView);
						});
					});
				});
			});
		});
	}
}