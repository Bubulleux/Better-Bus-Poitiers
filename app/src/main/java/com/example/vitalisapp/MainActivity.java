package com.example.vitalisapp;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
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

	private final ApiHelper apiHelper = new ApiHelper();
	private final List<PresetItem> presets = new ArrayList<>();
	private final String PRESET_FILE_NAME = "timetable_presets.json";
	public LayoutInflater inflater;

	private CustomAdapter<PresetItem> presetListAdapter;

	ActivityResultLauncher<Intent> anctivityGetOtherStation;
	ActivityResultLauncher<Intent> activityEditPreset;

	public static final String PREF_NAME = "com.example.BetterVitalis_Pref";



	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		LoadPresets();

		inflater = LayoutInflater.from(this);
		ListView presetListView = findViewById(R.id.preset_list);

		//Get Api token
		apiHelper.GetToken(null);

		//Initiate Activity Result
		anctivityGetOtherStation = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
				result ->
				{
					if (result.getResultCode() == RESULT_OK && result.getData() != null)
					{
						Bundle bundle = result.getData().getExtras();
						GetStationNextPassage((Station) bundle.get("Station"));
					}
					else
					{
						System.out.println("Error to receive Result");
					}
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
		String presetJson = new Gson().toJson(presets.toArray());

		SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("presets", presetJson);
		editor.apply();
		
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(new ComponentName(this, FavoriteWidget.class)), R.id.list_view);
		
		Toast.makeText(this, getString(R.string.preset_saved), Toast.LENGTH_SHORT).show();
	}

	private void LoadPresets()
	{
		SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		String presetJson = prefs.getString("presets", null);

		if (presetJson == null)
			return;

		presets.clear();
		presets.addAll(Arrays.asList(new Gson().fromJson(presetJson, PresetItem[].class)));

		Toast.makeText(this, getString(R.string.preset_loaded), Toast.LENGTH_SHORT).show();
	}

	public void otherTimetable()
	{
		Intent intent = new Intent(this, ActivityFindStation.class);

		apiHelper.GetAllStations(object ->
		{

			intent.putExtra("Stations", object);
			anctivityGetOtherStation.launch(intent);
		});
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
		intent.putExtra("Token", apiHelper.token);
		apiHelper.GetAllStations(object ->
		{
			intent.putExtra("Stations", object);
			activityEditPreset.launch(intent);
		});
	}


	private void TestInteractor()
	{
		apiHelper.GetToken(token -> {
			System.out.println("Request token Success");
			//output.setText(token);

			apiHelper.GetAllStations(object ->
			{
				for (Station station: object)
				{
					System.out.println(station.name);
				}
				Station station = object[869];
				apiHelper.GetStationLine(station, new ApiHelper.CallbackObject<Line[]>() {
					@Override
					public void onResponse(Line[] object) {
						for(Line line : object)
						{
							System.out.printf("%s  %s  %s\n", line.name, line.line_id, line.color);
						}
						apiHelper.GetStationLineId(station, object[0], new ApiHelper.CallbackObject<StationLineInfo>() {
							@Override
							public void onResponse(StationLineInfo object) {
								System.out.println(object.stop_id);

								apiHelper.GetNextPassage(station, null, new ApiHelper.CallbackObject<NextPassages>() {
									@Override
									public void onResponse(NextPassages object) {
										for (Passage nextPassages: object.realtime)
										{
											System.out.printf("%s %s\n", nextPassages.destinationName, nextPassages.expectedDepartureTime);
										}

										ListView presetListView = (ListView) findViewById(R.id.preset_list);
										System.out.println(presetListView);
									}
								});

							}
						});

					}
				});
			});


		});
	}
}