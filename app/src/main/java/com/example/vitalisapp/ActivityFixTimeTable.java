package com.example.vitalisapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityFixTimeTable extends AppCompatActivity
{
	private EditText dateInput;
	private Spinner lineInput;
	private Spinner destinationInput;
	private ListView timeTableList;
	private TextView labelTextView;
	private ProgressBar progressBar;
	private TextView emptyListTextView;
	private TextView stationNameView;
	
	private Station station;
	private List<Line> lines = new ArrayList<>();
	private Line curLine;
	private StationLineInfo lineInfo;
	int direction;
	
	private ApiHelper apiHelper;
	private Calendar dateSearch;
	
	private CustomAdapter<FixPassage> listAdapter;
	private List<FixPassage> fixPassagesList = new ArrayList<>();
	
	private ArrayAdapter<Line> lineArrayAdapter;
	
	private ActivityResultLauncher<Intent> changeStationResultLauncher;
	
	private boolean isLoading;
	
	/* Intent Extra:
		"Token": api Token (String)
		"Station": (Station)
	 */
	
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
		timeTableList = findViewById(R.id.list_view);
		labelTextView = findViewById(R.id.label_text_view);
		progressBar = findViewById(R.id.progressBar);
		emptyListTextView = findViewById(R.id.timetable_list_empty);
		stationNameView = findViewById(R.id.station_name);
		
		//Init Views
		stationNameView.setText(station.name);
		
		Init_View();
		
		RefreshDate();
		
		if (apiHelper.token == null)
			apiHelper.GetToken((String token) -> GetLines());
		else
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
		
		timeTableList.setEmptyView(emptyListTextView);
		lineInput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				curLine = position == 0 ? null : lines.get(position);
				if (curLine == null)
					return;
				
				runOnUiThread(() ->  emptyListTextView.setText(getString(R.string.timetable_list_empty)));
				
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
		
		lineArrayAdapter = new ArrayAdapter<Line>(this, R.layout.support_simple_spinner_dropdown_item, lines)
		{
			@Override
			public boolean isEnabled(int position)
			{
				return position != 0;
			}
			
			@Override
			public View getDropDownView(int position, View covertView, ViewGroup parent)
			{
				TextView TextView = (TextView) super.getDropDownView(position, covertView, parent);
				if (position == 0)
				{
					TextView.setText(R.string.select_line);
				}
				return TextView;
			}
			
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				TextView textView = (TextView) super.getView(position, convertView, parent);
				if (position == 0)
					textView.setText(R.string.select_line);
				
				return textView;
			}
			
		};
		lineArrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
		lineInput.setAdapter(lineArrayAdapter);
		
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
		
		listAdapter = new CustomAdapter<FixPassage>(this, fixPassagesList, new CustomAdapter.IUpdateAdapter<FixPassage>()
		{
			@Override
			public View getView(int i, View view, ViewGroup viewGroup, LayoutInflater inflater, List<FixPassage> list)
			{
				if (list.size() == 0)
					return view;
				FixPassage item = list.get(i);
				LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fixe_timetable_item, null);
				((TextView) layout.findViewById(R.id.txt_hour)).setText(String.format("%02d", item.hour));
				
				for (int j = 0; j < item.minutes.length; j++)
				{
					LinearLayout linearLayoutMinute = (LinearLayout) inflater.inflate(R.layout.minute_item_fixe_timetable, null);
					((TextView)linearLayoutMinute.findViewById(R.id.minute_txt)).setText(String.format("%02d", item.minutes[j]));
					((TextView)linearLayoutMinute.findViewById(R.id.label_txt)).setText(String.format("%c", item.labels[j]));
					layout.addView(linearLayoutMinute);
				}
				
				return layout;
			}
		});
		timeTableList.setAdapter(listAdapter);
		
		findViewById(R.id.go_back_btn).setOnClickListener((View view) -> finish());
		
		findViewById(R.id.next_passage_btn).setOnClickListener((View view) ->
		{
			Intent intent = new Intent(this, NextPassageActivity.class);
			intent.putExtra("Token", apiHelper.token);
			intent.putExtra("Station", station);
			
			startActivity(intent);
			finish();
		});
		
		stationNameView.setOnLongClickListener((View view ) -> {
			Helper.moreInfoStation(this, station);
			return true;
		});
		
		stationNameView.setOnClickListener((View view) ->
		{
			if (isLoading)
				return;
			Intent intent = new Intent(this, ActivityFindStation.class);
			changeStationResultLauncher.launch(intent);
		});
		
		//ChangeStation
		changeStationResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
		{
			station = (Station) result.getData().getExtras().get("Station");
			stationNameView.setText(station.name);
			GetLines();
		});
	}
	
	public void GetLines()
	{
		setLoading(true);
		lineInput.setSelection(0);
		apiHelper.GetStationLine(station, (Line[] linesCallBack) ->
		{
			setLoading(false);
			lines.clear();
			lines.add(new Line());
			lines.addAll(Arrays.asList(linesCallBack));
//			ArrayAdapter<Line> lineArrayAdapter = new ArrayAdapter<Line>(this, R.layout.support_simple_spinner_dropdown_item, lines);
//			lineArrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
			fixPassagesList.clear();
			runOnUiThread(() ->
			{
				destinationInput.setAdapter(null);
				labelTextView.setText("");
				listAdapter.notifyDataSetChanged();
				lineArrayAdapter.notifyDataSetChanged();
				emptyListTextView.setText(getString(R.string.select_line));
			});
		});
	}
	
	public void setLoading(boolean isLoading)
	{
		this.isLoading = isLoading;
		int visible_if_loading = isLoading ? View.VISIBLE : View.INVISIBLE;
		int invisible_if_loading = isLoading ? View.INVISIBLE : View.VISIBLE;
		runOnUiThread(() ->
		{
			progressBar.setVisibility(visible_if_loading);
			timeTableList.setVisibility(invisible_if_loading);
			if (isLoading)
				emptyListTextView.setVisibility(invisible_if_loading);
		});
	}
	
	public void RefreshDate()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("dd  MMM  yyyy");
		dateInput.setText(sdf.format(dateSearch.getTime()));
		RefreshTimeTable();
	}
	
	public void RefreshTimeTable()
	{
		if (curLine == null)
			return;
		
		setLoading(true);
		String[] terminusArray = (direction == 1) ? lineInfo.boarding_ids.aller : lineInfo.boarding_ids.retour;
		
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
						setLoading(false);
						fixPassagesList.clear();
						int hour =  -1;
						List<Integer> minutes = new ArrayList<>();
						List<Character> labels = new ArrayList<>();
						Calendar scheduleDate = Calendar.getInstance();
						
						SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
						for (Schedule schedule : object.horaire)
						{
							try
							{
								scheduleDate.setTime(sdf.parse(schedule.time));
							} catch (ParseException e)
							{
								e.printStackTrace();
							}
							
							if (hour != scheduleDate.get(Calendar.HOUR_OF_DAY))
							{
								if (hour != -1)
									fixPassagesList.add(new FixPassage(hour, minutes, labels));
								
								hour = scheduleDate.get(Calendar.HOUR_OF_DAY);
								minutes.clear();
								labels.clear();
							}
							minutes.add(scheduleDate.get(Calendar.MINUTE));
							labels.add(schedule.label.charAt(0));
						}
						if (hour != -1)
							fixPassagesList.add(new FixPassage(hour, minutes, labels));
						
						listAdapter.list = fixPassagesList;
						runOnUiThread(() -> listAdapter.notifyDataSetChanged());
						
						StringBuilder labelStringBuilder = new StringBuilder();
						for (Terminus label : object.terminus)
						{
							if (!labelStringBuilder.toString().equals(""))
								labelStringBuilder.append("\n");
							labelStringBuilder.append(String.format("%s: %s", label.label, label.direction));
						}
						runOnUiThread(() -> labelTextView.setText(labelStringBuilder.toString()));
						
					}
				});
	}
	
	public class FixPassage
	{
		int hour;
		int[] minutes;
		char[] labels;
		
		public FixPassage(int hour, List<Integer> minutes, List<Character> labels)
		{
			this.hour = hour;
			this.minutes = new int[minutes.size()];
			this.labels = new char[labels.size()];
			for (int i = 0; i < minutes.size(); i++)
			{
				this.minutes[i] = minutes.get(i);
				this.labels[i] = labels.get(i);
			}
		}
	}
}

