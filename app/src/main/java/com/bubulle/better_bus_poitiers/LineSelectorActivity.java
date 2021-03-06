package com.bubulle.better_bus_poitiers;

import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LineSelectorActivity extends AppCompatActivity
{
	ListView listView;
	Button finishBtn;
	Button selectAllBtn;
	Button unselectAllBtn;

	Line[] lines;
	DirectionPreset[] directionsPreset;
	List<DirectionPreset> allDirections = new ArrayList<>();
	Boolean[][] directionsSelected;

	private CustomAdapter<DirectionPreset> listViewAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_line_selector);

		//Get View
		listView =  findViewById(R.id.line_selection_view);

		finishBtn =  findViewById(R.id.finish_btn);
		finishBtn.setOnClickListener((View view) -> Done());

		selectAllBtn = findViewById(R.id.select_all_btn);
		selectAllBtn.setOnClickListener((View view) -> SelectAll(true));

		unselectAllBtn = findViewById(R.id.unselect_all_btn);
		unselectAllBtn.setOnClickListener((View view) -> SelectAll(false));

		//Get Extra
		lines = (Line[]) getIntent().getExtras().get("Lines");
		directionsPreset = (DirectionPreset[]) getIntent().getExtras().get("Directions");

		//Debug Extra
		System.out.println("Direction in direction editor:");
		StringBuilder out = new StringBuilder("\n");
		for (DirectionPreset direction : directionsPreset)
		{
			out.append(String.format("Line: %s, Direction %s terminus\n", direction.line_id, direction.direction));
			for (String terminus : direction.terminus)
			{
				out.append(String.format("     %s\n", terminus));
			}
		}
		System.out.println(out);


		InitList();

		listViewAdapter = new CustomAdapter<>(this, allDirections, (i, view, viewGroup, inflater, list) ->
		{
			DirectionPreset item = list.get(i);
			ConstraintLayout layout = (ConstraintLayout) inflater.inflate(R.layout.line_selection_item, null);
			((TextView)layout.findViewById(R.id.line_id)).setText(item.line_id);
			layout.findViewById(R.id.line_id).setBackgroundColor(Color.parseColor(item.line_color));
			
			int color = Helper.getTextContrast(item.line_color);
			((TextView)layout.findViewById(R.id.line_id)).setTextColor(color);
			
			((TextView)layout.findViewById(R.id.direction_txt)).setText(item.direction);

			CheckBox checkBox =  layout.findViewById(R.id.line_selected);

			checkBox.setOnCheckedChangeListener((compoundButton, b) ->
			{
				for (int j = 0; j < item.terminus.length; j++)
				{
					directionsSelected[i][j] = b;
				}
				runOnUiThread(() -> listViewAdapter.notifyDataSetChanged());
			});
			LinearLayout terminusList = layout.findViewById(R.id.terminus_list_view);
			boolean allChecked = true;

			for (int j = 0; j < item.terminus.length; j++)
			{
				LinearLayout child = (LinearLayout) inflater.inflate(R.layout.terminus_item, null);
				((TextView)child.findViewById(R.id.terminus_name)).setText(item.terminus[j]);
				CheckBox terminusCheckBox =  child.findViewById(R.id.terminus_selected);

				terminusCheckBox.setChecked(directionsSelected[i][j]);
				if (!directionsSelected[i][j])
					allChecked = false;

				int finalJ = j;
				terminusCheckBox.setOnCheckedChangeListener((compoundButton, b) -> directionsSelected[i][finalJ] = b);

				terminusList.addView(child);
			}
			checkBox.setChecked(allChecked);


			return layout;
		});

		listView.setAdapter(listViewAdapter);

	}

	@Override
	public void onPause()
	{
		super.onPause();
		finish();
	}

	private void SelectAll(boolean select)
	{
		for (Boolean[] booleans : directionsSelected)
		{
			Arrays.fill(booleans, select);
		}

		listViewAdapter.notifyDataSetChanged();
	}

	private void Done()
	{

		Intent intent = new Intent();
		List<DirectionPreset> returnDirection = new ArrayList<>();
		for (int i = 0; i < allDirections.size(); i++)
		{
			DirectionPreset itemDirection = allDirections.get(i);
			List<String> terminus = new ArrayList<>();
			for (int j = 0; j < itemDirection.terminus.length; j ++)
			{
				if (directionsSelected[i][j])
				{
					terminus.add(itemDirection.terminus[j]);
				}
			}
			if (terminus.size() != 0)
			{
				DirectionPreset curDirection = new DirectionPreset(itemDirection.line_id, itemDirection.direction,
						terminus.toArray(new String[0]), itemDirection.line_color);
				returnDirection.add(curDirection);
			}
		}


		intent.putExtra("Directions", returnDirection.toArray(new DirectionPreset[0]));
		setResult(RESULT_OK, intent);
		finish();
	}

	private void InitList()
	{
		//Get all terminus
		for (Line line : lines)
		{
			if (line.direction.aller.length != 0)
				allDirections.add(new DirectionPreset(line.line_id, line.direction.aller[0], line.direction.aller,line.color));

			if (line.direction.retour.length != 0)
				allDirections.add(new DirectionPreset(line.line_id, line.direction.retour[0], line.direction.retour,line.color));
		}

		//Init direction Selected
		directionsSelected = new Boolean[allDirections.size()][];

		System.out.println("Searching already selectd terminus");
		for (int i = 0; i < allDirections.size(); i++)
		{
			DirectionPreset direction = allDirections.get(i);
			directionsSelected[i] = new Boolean[direction.terminus.length];
			for (int j = 0; j < direction.terminus.length; j ++)
				directionsSelected[i][j] = false;

			System.out.printf("Station Direction Debug  %s %s\n", direction.line_id, direction.direction);
			for (DirectionPreset directionPreset : directionsPreset)
			{
				System.out.printf("%s %s  &&  %s %s\n", direction.line_id, direction.direction, directionPreset.line_id, directionPreset.direction);
				if (direction.line_id.equals(directionPreset.line_id) && direction.direction.equals(directionPreset.direction))
				{
					System.out.printf("Finding Direction Line: %s, Direction: %s\n", direction.line_id, direction.direction);
					for (int j = 0; j < direction.terminus.length; j++)
					{
						String terminus = direction.terminus[j];
						for (String terminusPreset: directionPreset.terminus)
						{
							if (terminus.equals(terminusPreset))
							{
								System.out.printf("     Finding Terminus: %s", terminus);
								directionsSelected[i][j] = true;
								break;
							}
						}
					}
				}
			}
		}
	}
}