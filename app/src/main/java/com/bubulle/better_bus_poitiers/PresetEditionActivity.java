package com.bubulle.better_bus_poitiers;

import android.content.Intent;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class PresetEditionActivity extends AppCompatActivity
{
	private EditText nameInput;
	private TextView stationName;
	private Button changeStationBtn;
	private Button changeLineListBtn;
	private CheckBox favoriteCheckBox;
	private TextView positionOutput;
	private Button upBtn;
	private Button downBtn;

	private Button saveBtn;
	private Button deleteBtn;

	private CustomAdapter<Line> gridViewLineAdapter;
	private final List<Line> lineInGridView = new ArrayList<>();

	private PresetItem preset;
	private ApiHelper apiHelper;
	private int index;
	private int newPosition;
	private int presetsCount;
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

		changeLineListBtn = findViewById(R.id.change_line_btn);

		favoriteCheckBox = findViewById(R.id.favorite_check_box);

		positionOutput = findViewById(R.id.output_position);
		upBtn = findViewById(R.id.up_btn);
		downBtn = findViewById(R.id.down_btn);

		saveBtn = findViewById(R.id.save_btn);
		deleteBtn = findViewById(R.id.delete_btn);

		
		
		favoriteCheckBox.setOnCheckedChangeListener((compoundButton, b) -> preset.isFavorite = b);

		//Get Extra
		Intent intent = getIntent();
		preset = (PresetItem) intent.getSerializableExtra("Preset");
		index = (Integer) intent.getSerializableExtra("Index");
		newPosition = index;
		presetsCount = (Integer) intent.getSerializableExtra("PresetsCount");
		
		apiHelper = new ApiHelper(this);



		InitActivityLauncher();
		InitBtn();
		
		//Init Grid Line Adapter
		gridViewLineAdapter = new CustomAdapter<>(this, lineInGridView, (i, view, viewGroup, inflater, list) ->
		{
			LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.line_item, null);
			((TextView)layout.findViewById(R.id.line_id)).setText(list.get(i).line_id);
			layout.findViewById(R.id.line_id).setBackgroundColor(Color.parseColor(list.get(i).color));
			
			int color = Helper.getTextContrast(list.get(i).color);
			((TextView)layout.findViewById(R.id.line_id)).setTextColor(color);

			return layout;
		});
		((GridView)findViewById(R.id.grid_view_line)).setAdapter(gridViewLineAdapter);

		if (preset != null && !preset.stationName.equals(""))
			RefreshLine(preset.stationName);
		
		RefreshInfo();
	}
	
	
	public void InitBtn()
	{
		changeStationBtn.setOnClickListener(view ->
		{
			Intent intent = new Intent(view.getContext(), ActivityFindStation.class);
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

		upBtn.setOnClickListener((View view) ->
		{
			newPosition -= 1;
			if (newPosition < 0)
				newPosition = 0;
			RefreshNewPosition();
		});

		downBtn.setOnClickListener((View view) ->
		{
			newPosition += 1;
			if (newPosition >= presetsCount)
				newPosition = presetsCount - 1;
			RefreshNewPosition();
		});


		saveBtn.setOnClickListener((View view) -> SaveBtn());
		deleteBtn.setOnClickListener((View view) -> DeleteBtn());
	}
	
	@Override
	public void onBackPressed()
	{
		SaveBtn();
	}

	public void DeleteBtn()
	{
		AlertDialog deleteAlert = new AlertDialog.Builder(this).create();
		deleteAlert.setMessage(getString(R.string.delete_msg));

		deleteAlert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.yes),
				(dialogInterface, i) -> Done(true));

		deleteAlert.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.no),
				((dialogInterface, i) -> {}));

		deleteAlert.show();

	}

	public void SaveBtn()
	{
		if (preset.name == null || preset.name.equals(""))
			Toast.makeText(this, getString(R.string.error_name), Toast.LENGTH_LONG).show();

		else if (preset.stationName == null || preset.stationName.equals(""))
			Toast.makeText(this, getString(R.string.error_station), Toast.LENGTH_LONG).show();

		else
			Done(false);
	}

	public void Done(boolean delete)
	{
		Intent intent = new Intent();
		intent.putExtra("Preset", delete ? null : preset);
		intent.putExtra("Index", index);
		intent.putExtra("NewPos", newPosition);
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
						preset.stationLatitude = station.lat;
						preset.stationLongitude = station.lng;
						preset.directions = new DirectionPreset[0];
						RefreshLine(station.name);
						runOnUiThread(this::RefreshInfo);
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
						runOnUiThread(this::RefreshInfo);
					}
				});
	}



	public void RefreshInfo()
	{
		nameInput.setText(preset.name);
		stationName.setText(preset.stationName);
		favoriteCheckBox.setChecked(preset.isFavorite);
		RefreshGridViewLine();
		RefreshNewPosition();
	}

	public void RefreshNewPosition()
	{
		positionOutput.setText(Integer.toString(newPosition + 1));
	}



	public void RefreshLine(String stationName)
	{
		stationLine = null;
		apiHelper.GetStationLine(stationName, object -> stationLine = object);
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
				if (line.line_id.equals(curDirection.line_id))
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