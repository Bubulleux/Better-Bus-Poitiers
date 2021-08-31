package com.example.vitalisapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Debug;
import android.text.Editable;
import android.text.TextWatcher;
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

public class PresetEditionActivity extends AppCompatActivity
{
	private EditText nameInput;
	private TextView stationName;
	private Button changeStationBtn;
	private GridView lineListView;
	private Button changeLineListBtn;
	private CheckBox favoriteCheckBox;

	private Button saveBtn;
	private Button deleteBtn;

	private CustomAdapter<Line> gridViewLineAdapter;
	private List<Line> lineInGridView = new ArrayList<>();

	private PresetItem preset;
	private Station[] stations;
	private ApiHelper apiHelper;
	private int index;
	private Line[] stationLine;

	//Activity Result
	ActivityResultLauncher<Intent> activityResultLauncherFindStation;
	ActivityResultLauncher<Intent> activityResultLauncherSelectLine;



	@Override
	protected void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preset_edition);

		//Get Views
		nameInput = findViewById(R.id.name_preset_edit_text);

		stationName = findViewById(R.id.station_name);
		changeStationBtn = findViewById(R.id.change_station_btn);

		lineListView = findViewById(R.id.grid_view_line);
		changeLineListBtn = findViewById(R.id.change_line_btn);

		favoriteCheckBox = findViewById(R.id.favorite_check_box);

		saveBtn = findViewById(R.id.save_btn);
		deleteBtn = findViewById(R.id.delete_btn);


		InitActivityLauncher();
		InitBtn();


		favoriteCheckBox.setOnCheckedChangeListener((compoundButton, b) -> preset.isFavorite = b);

		//Get Extra
		Intent intent = getIntent();
		preset = (PresetItem) intent.getSerializableExtra("Preset");
		stations = (Station[]) intent.getSerializableExtra("Stations");
		index = (Integer) intent.getSerializableExtra("Index");
		apiHelper = new ApiHelper();
		apiHelper.token = (String) intent.getSerializableExtra("Token");
		apiHelper.stations = stations;

		//Init Grid Line Adapter
		gridViewLineAdapter = new CustomAdapter<>(this, lineInGridView, new CustomAdapter.IUpdateAdapter<Line>() {
			@Override
			public View getView(int i, View view, ViewGroup viewGroup, LayoutInflater inflater, List<Line> list)
			{
				LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.line_item, null);
				((TextView)layout.findViewById(R.id.line_id)).setText(list.get(i).line_id);
				layout.findViewById(R.id.line_id).setBackgroundColor(Color.parseColor(list.get(i).color));

				return layout;
			}
		});
		lineListView.setAdapter(gridViewLineAdapter);

		if (preset != null && !preset.stationName.equals(""))
			RefreshLine(preset.stationName);

		RefreshInfo();
	}



	public void InitBtn()
	{
		changeStationBtn.setOnClickListener(view ->
		{
			Intent intent = new Intent(view.getContext(), ActivityFindStation.class);
			intent.putExtra("Stations", stations);
			activityResultLauncherFindStation.launch(intent);
		});

		changeLineListBtn.setOnClickListener(view ->
		{
			if (stationLine == null)
				return;

			Intent intent = new Intent(view.getContext(), LineSelectorActivity.class);
			intent.putExtra("Lines", stationLine);
			intent.putExtra("Directions", preset.directions);
			activityResultLauncherSelectLine.launch(intent);

		});

		nameInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
			{
				preset.name = String.valueOf(charSequence);
			}

			@Override
			public void afterTextChanged(Editable editable) { }
		});

		saveBtn.setOnClickListener((View view) -> Done(false));
		deleteBtn.setOnClickListener((View view) -> Done(true));


	}

	public void Done(boolean delete)
	{
		Intent intent = new Intent();
		intent.putExtra("Preset", delete ? null : preset);
		intent.putExtra("Index", index);
		setResult(RESULT_OK, intent);
		finish();
	}


	public void InitActivityLauncher()
	{
		activityResultLauncherFindStation = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
				result ->
				{
					if (result.getResultCode() == RESULT_OK && result.getData() != null)
					{
						Station station = (Station) result.getData().getExtras().get("Station");
						preset.stationName = station.name;
						RefreshLine(station.name);
						runOnUiThread(() -> RefreshInfo());
					}
				});

		activityResultLauncherSelectLine = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
				result ->
				{
					if (result.getResultCode() == RESULT_OK && result.getData() != null)
					{
						preset.directions = (DirectionPreset[]) result.getData().getSerializableExtra("Directions");

						StringBuilder out = new StringBuilder("\n");
						for (DirectionPreset direction : preset.directions)
						{
							out.append(String.format("Line: %s, Direction %s terminus\n", direction.line_id, direction.direction));
							for (String terminus : direction.terminus)
							{
								out.append(String.format("     %s\n", terminus));
							}
						}
						System.out.println(out);
						runOnUiThread(() -> RefreshInfo());
					}
				});
	}



	public void RefreshInfo()
	{
		nameInput.setText(preset.name);
		stationName.setText(preset.stationName);
		favoriteCheckBox.setChecked(preset.isFavorite);
		RefreshGridViewLine();
	}

	public void RefreshLine(String stationName)
	{
		stationLine = null;
		apiHelper.GetStationLine(stationName, object ->
		{
			stationLine = object;
		});
	}

	public void RefreshGridViewLine()
	{
		if (preset.directions == null)
			return;

		lineInGridView.clear();
		for (DirectionPreset curDirection : preset.directions)
		{
			boolean contain = false;
			for (Line line : lineInGridView)
			{
				if (line.line_id == curDirection.line_id)
				{
					contain = true;
					break;
				}
			}
			if (!contain)
			{
				Line newLine = new Line();
				newLine.line_id = curDirection.line_id;
				newLine.color = curDirection.line_color;
				lineInGridView.add(newLine);
			}
		}
		gridViewLineAdapter.notifyDataSetChanged();
	}
}