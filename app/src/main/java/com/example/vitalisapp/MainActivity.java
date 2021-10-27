package com.example.vitalisapp;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
	public static boolean is_running = false;
	
	
	private ApiHelper apiHelper;
	private final List<PresetItem> presets = new ArrayList<>();
	private final String PRESET_FILE_NAME = "timetable_presets.json";
	public LayoutInflater inflater;
	
	private boolean isLoading = false;
	private ProgressBar progressBar;

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
		progressBar = findViewById(R.id.progressBar);

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
		presetListAdapter = new CustomAdapter<PresetItem>(this, presets, new CustomAdapter.IUpdateAdapter<PresetItem>() {
			@Override
			public View getView(int i, View view, ViewGroup viewGroup, LayoutInflater inflater, List<PresetItem> list) {

				view = inflater.inflate(R.layout.preset_item, null);
				PresetItem item = list.get(i);

				view.setOnLongClickListener((View viewBtn) ->
				{
					UpdatePreset(i);
					return true;
				});

				view.setOnClickListener((View viewBtn) -> GoToTimetable(item));

				((TextView)view.findViewById(R.id.name_txt)).setText(item.name);

				if (item.isFavorite)
					((TextView)view.findViewById(R.id.name_txt)).setTypeface(null, Typeface.BOLD);

				((TextView)view.findViewById(R.id.station_text)).setText(item.stationName);

				GridLayout lineGrid = ((GridLayout)view.findViewById(R.id.line_list));

				//Add line
				List<String> lineAlreadyAdded = new ArrayList<>();

				for (DirectionPreset direction : item.directions)
				{
					boolean contain = false;
					for (String line: lineAlreadyAdded)
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

					lineAlreadyAdded.add(direction.line_id);
					lineGrid.addView(layout);
				}

				return view;
			}
		});
		

		presetListView.setAdapter(presetListAdapter);
		
		//Get Auto Load next Passage
		PresetItem presetStart = (PresetItem) getIntent().getSerializableExtra("StartNextPassage");
		if (presetStart != null)
		{
			GoToTimetable(presetStart);
		}
    }
	
	@Override
	public void onStop()
	{
		is_running = false;
		super.onStop();
	}
	
	private void setLoading(boolean value)
	{
		isLoading = value;
		progressBar.setVisibility(value ? View.VISIBLE : View.INVISIBLE);
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

		Toast.makeText(this, getString(R.string.preset_loaded), Toast.LENGTH_SHORT).show();
	}

	public void otherTimetable()
	{
		Intent intent = new Intent(this, ActivityFindStation.class);
		String[] stationsHistoryArray = Helper.loadPrefJson("station_history", String[].class, this);
		if (stationsHistoryArray != null)
		{
			intent.putExtra("History", stationsHistoryArray);
			System.out.println(stationsHistoryArray.length);
		}
		intent.putExtra("Token", apiHelper.token);;
		activityResultLauncher.launch(intent);
	}

	public void GoToTimetable(PresetItem timetablePreset)
	{
		Intent intent = new Intent(this, NextPassageActivity.class);
		intent.putExtra("Token", apiHelper.token);
		intent.putExtra("TimetablePreset", timetablePreset);
		startActivity(intent);
	}
	
	
	
	private void GetStationNextPassage(Station station)
	{
		System.out.printf("Station Chose: %s\n", station.name);
		Intent intent = new Intent(this, NextPassageActivity.class);
		intent.putExtra("Token", apiHelper.token);
		intent.putExtra("Station", station);
		startActivity(intent);
	}
	
	

	public void UpdatePreset(int i)
	{
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

							ListView presetListView = (ListView) findViewById(R.id.preset_list);
							System.out.println(presetListView);
						});
					});
				});
			});
		});
	}
}