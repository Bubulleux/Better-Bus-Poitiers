package com.example.vitalisapp;

import android.app.DatePickerDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class ActivityFixTimeTable extends AppCompatActivity
{
	private EditText dateInput;
	private Spinner lineInput;
	private Spinner destinationInput;
	
	private Station station;
	private Line[] lines;
	private Line curLine;
	private StationLineInfo lineInfo;
	int direction;
	
	private ApiHelper apiHelper;
	private Calendar dateSearch;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fix_time_table);
		
		//Init Value
		dateSearch = Calendar.getInstance();
		apiHelper = new ApiHelper(this);
		
		//Get Intent Data
		station = (Station) getIntent().getSerializableExtra("Station");
		apiHelper.token = getIntent().getStringExtra("Token");
		
		
		//Get Views
		dateInput = findViewById(R.id.date_input);
		lineInput = findViewById(R.id.line_spinner);
		destinationInput = findViewById(R.id.destination_spinner);
		
		//Init Views
		((TextView) findViewById(R.id.station_name)).setText(station.name);
		
		Init_View();
		
		RefreshDate();
		GetLines();
		
	}
	
	public void Init_View()
	{
		dateInput.setOnClickListener((view) ->
		{
			DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view1, year, month, dayOfMonth) ->
			{
				dateSearch =  new GregorianCalendar(year, month, dayOfMonth);
				RefreshDate();
			}, dateSearch.get(Calendar.YEAR), dateSearch.get(Calendar.MONTH), dateSearch.get(Calendar.DAY_OF_MONTH));
			datePickerDialog.show();
		});
		
		lineInput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				curLine = lines[position];
				apiHelper.GetStationLineId(station, curLine, (StationLineInfo stationLineInfo) ->
				{
					lineInfo = stationLineInfo;
					String[] directionString = new String[] {
							TextUtils.join(" | ", Arrays.copyOfRange(curLine.direction.retour, 0, lineInfo.boarding_ids.retour.length)),
							TextUtils.join(" | ", Arrays.copyOfRange(curLine.direction.aller, 0, lineInfo.boarding_ids.aller.length))};
					
					ArrayAdapter<String> destinationAdapter = new ArrayAdapter<String>(parent.getContext(), R.layout.support_simple_spinner_dropdown_item, directionString);
					destinationAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
					runOnUiThread(() -> destinationInput.setAdapter(destinationAdapter));
				});
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
			
			}
		});
		
		destinationInput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				direction = position;
				RefreshTimeTable();
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{
			
			}
		});
	}
	
	public void GetLines()
	{
		apiHelper.GetStationLine(station, (Line[] linesCallBack) ->
		{
			lines = linesCallBack;
			ArrayAdapter<Line> lineArrayAdapter = new ArrayAdapter<Line>(this, R.layout.support_simple_spinner_dropdown_item, lines);
			lineArrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
			runOnUiThread(() -> lineInput.setAdapter(lineArrayAdapter));
		});
	}
	
	public void RefreshDate()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("dd  MMM  yyyy");
		dateInput.setText(sdf.format(dateSearch.getTime()));
	}
	
	public void RefreshTimeTable()
	{
		String[] terminusArray = (direction == 1) ? lineInfo.boarding_ids.aller : lineInfo.boarding_ids.retour;
		System.out.printf("terminusLength: %d\n", terminusArray.length);
		int[] terminusArrayInt = new int[terminusArray.length];
		for (int i = 0; i < terminusArrayInt.length; i++)
		{
			terminusArrayInt[i] = Integer.parseInt(terminusArray[i]);
		}
		
		apiHelper.GetTimeTable(lineInfo.stop_id, curLine, terminusArrayInt,
				direction, dateSearch, new ApiHelper.CallbackObject<TimeTable>()
				{
					@Override
					public void onResponse(TimeTable object)
					{
						for (Schedule schedule : object.horaire)
						{
							System.out.printf("%s %s, ", schedule.time, schedule.label);
						}
					}
				});
	}
}