package com.example.vitalisapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

	private final ApiInteractor interactor = new ApiInteractor();
	private final List<PresetItem> presets = new ArrayList<>();
	public LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


		System.out.println("Start");
		//TestInteractor();
		presets.add(new PresetItem("Ecole", "Voie Malraux", new String[]{"1", "11"}, false ));
		presets.add(new PresetItem("Ecole2", "Notre Dame", new String[]{"1", "25"}, true ));
		presets.add(new PresetItem("Sus amogus", "Voie Malraux", new String[]{"1", "34"}, false ));
		GridView presetListView = (GridView)findViewById(R.id.preset_list);
		System.out.println(presetListView);

		if (presetListView == null)
		{
			System.out.println("Preset List view not found");
			return;
		}

		inflater = LayoutInflater.from(this);


		presetListView.setAdapter(new UpdateAdapter<PresetItem>(this, presets, new UpdateAdapter.IUpdateAdapter<PresetItem>() {
			@Override
			public View getView(int i, View view, ViewGroup viewGroup, LayoutInflater inflater, List<PresetItem> list) {
				view = inflater.inflate(R.layout.preset_item, null);
				return view;
			}
		}));

		//presetListView.setAdapter(new CustomAdapter(this));


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