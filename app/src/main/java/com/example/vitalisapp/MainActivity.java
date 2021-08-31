package com.example.vitalisapp;

import android.content.Intent;
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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{

	private final ApiHelper apiHelper = new ApiHelper();
	private final List<PresetItem> presets = new ArrayList<>();
	public LayoutInflater inflater;

	private CustomAdapter<PresetItem> presetListAdapter;

	ActivityResultLauncher<Intent> anctivityGetOtherStation;
	ActivityResultLauncher<Intent> activityEditPreset;



	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


		//Inisialize Exemple Preset
		presets.add(new PresetItem("Ecole", "Voie Malraux", new DirectionPreset[0], false ));
		presets.add(new PresetItem("Ecole2", "Notre Dame",  new DirectionPreset[0], true ));
		presets.add(new PresetItem("Sus amogus", "Voie Malraux",  new DirectionPreset[0], false ));


		inflater = LayoutInflater.from(this);
		ListView presetListView = (ListView)findViewById(R.id.preset_list);

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


						if (preset == null)
							presets.remove(index);
						else
							presets.set(index, preset);


						runOnUiThread(() -> presetListAdapter.notifyDataSetChanged());
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

				view.setOnClickListener((View viewBtn) -> UpdatePreset(i));

				((TextView)view.findViewById(R.id.name_txt)).setText(item.name);
				((TextView)view.findViewById(R.id.station_text)).setText(item.stationName);
				System.out.printf("%s Has been Updated\n", item.name);

				GridLayout lineGrid = ((GridLayout)view.findViewById(R.id.line_list));
//				for (String line : item.lineId)
//				{
//					LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.line_item, null);
//					TextView textView = ((TextView)layout.findViewById(R.id.line_id));
//
//					textView.setText(line);
//					lineGrid.addView(layout);
//				}

				return view;
			}
		});

		presetListView.setAdapter(presetListAdapter);



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