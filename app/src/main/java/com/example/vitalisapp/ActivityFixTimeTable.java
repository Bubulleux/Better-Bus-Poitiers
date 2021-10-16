package com.example.vitalisapp;

import android.app.DatePickerDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.example.vitalisapp.databinding.FixeTimetableItemBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityFixTimeTable extends AppCompatActivity
{
	private EditText dateInput;
	private Spinner lineInput;
	private Spinner destinationInput;
	private ListView timeTableList;
	
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
				curLine = position == 0 ? null : lines.get(position);
				if (curLine == null)
					return;
				
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
				return true;
			}
			
			@Override
			public View getDropDownView(int position, View covertView, ViewGroup parent)
			{
				TextView TextView = (TextView) super.getDropDownView(position, covertView, parent);
				if (position == 0)
				{
					TextView.setText("NaN");
				}
				return TextView;
			}
			
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				TextView textView = (TextView) super.getView(position, convertView, parent);
				if (position == 0)
					textView.setText("NaN");
				
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
		
		((Button)findViewById(R.id.go_back_btn)).setOnClickListener((View view) -> finish());
		timeTableList.setAdapter(listAdapter);
	}
	
	public void GetLines()
	{
		apiHelper.GetStationLine(station, (Line[] linesCallBack) ->
		{
			lines.clear();
			lines.add(new Line());
			lines.addAll(Arrays.asList(linesCallBack));
//			ArrayAdapter<Line> lineArrayAdapter = new ArrayAdapter<Line>(this, R.layout.support_simple_spinner_dropdown_item, lines);
//			lineArrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
			runOnUiThread(() ->
			{
				lineArrayAdapter.notifyDataSetChanged();
			});
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

