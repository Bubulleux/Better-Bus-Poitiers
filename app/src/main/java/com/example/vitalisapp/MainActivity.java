package com.example.vitalisapp;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Inflater;

public class MainActivity extends AppCompatActivity {

	private final ApiInteractor interactor = new ApiInteractor();
	private final List<PresetItem> presets = new ArrayList<>();
	public LayoutInflater inflater;

	ActivityResultLauncher<Intent> anctivityGetOtherStation;



	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


		//Inisialize Exemple Preset
		presets.add(new PresetItem("Ecole", "Voie Malraux", new String[]{"1", "11", "13", "15", "20"}, false ));
		presets.add(new PresetItem("Ecole2", "Notre Dame", new String[]{"1", "25"}, true ));
		presets.add(new PresetItem("Sus amogus", "Voie Malraux", new String[]{"1", "34", "20"}, false ));


		inflater = LayoutInflater.from(this);
		ListView presetListView = (ListView)findViewById(R.id.preset_list);

		//Get Api token
		interactor.GetToken(null);

		//Initiate Activity Result
		anctivityGetOtherStation = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
			new ActivityResultCallback<ActivityResult>()
			{
				@Override
				public void onActivityResult(ActivityResult result)
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
				}
			});

		if (presetListView == null)
		{
			System.out.println("Preset List view not found");
			return;
		}

		findViewById(R.id.button_other).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				((MainActivity)view.getContext()).otherTimetable();
			}
		});



		//Initialize Preset List
		presetListView.setAdapter(new CustomAdapter<PresetItem>(this, presets, new CustomAdapter.IUpdateAdapter<PresetItem>() {
			@Override
			public View getView(int i, View view, ViewGroup viewGroup, LayoutInflater inflater, List<PresetItem> list) {
				view = inflater.inflate(R.layout.preset_item, null);
				PresetItem item = list.get(i);

				((TextView)view.findViewById(R.id.name_txt)).setText(item.name);
				((TextView)view.findViewById(R.id.station_text)).setText(item.stationName);

				GridLayout lineGrid = ((GridLayout)view.findViewById(R.id.line_list));
				for (String line : item.lineId)
				{
					LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.line_item, null);
					TextView textView = ((TextView)layout.findViewById(R.id.line_id));

					textView.setText(line);
					lineGrid.addView(layout);
				}

				return view;
			}
		}));

    }

	public void otherTimetable()
	{
		Intent intent = new Intent(this, ActivityFindStation.class);

		interactor.GetAllStations(new ApiInteractor.CallbackObject<Station[]>() {
			@Override
			public void onResponse(Station[] object) {

				intent.putExtra("Stations", object);
				anctivityGetOtherStation.launch(intent);
			}
		});
	}

	private void GetStationNextPassage(Station station)
	{
		System.out.printf("Station Chose: %s\n", station.name);
	}


	private void TestInteractor()
	{
		interactor.GetToken(token -> {
			System.out.println("Request token Success");
			//output.setText(token);

			interactor.GetAllStations(object ->
			{
				for (Station station: object)
				{
					System.out.println(station.name);
				}
				Station station = object[869];
				interactor.GetStationLine(station, new ApiInteractor.CallbackObject<Line[]>() {
					@Override
					public void onResponse(Line[] object) {
						for(Line line : object)
						{
							System.out.printf("%s  %s  %s\n", line.name, line.line_id, line.color);
						}
						interactor.GetStationLineId(station, object[0], new ApiInteractor.CallbackObject<StationLineInfo>() {
							@Override
							public void onResponse(StationLineInfo object) {
								System.out.println(object.stop_id);

								interactor.GetNextPassage(station, null, new ApiInteractor.CallbackObject<NextPassages>() {
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