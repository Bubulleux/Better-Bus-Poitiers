package com.bubulle.better_bus_poitiers;

import java.io.Serializable;

class Station implements Serializable
{
	String stop_id;
	String id = "";
	String name;
	Float lat;
	Float lng;
}


class Lines
{
	Line[] lines;
}

class Line implements Serializable
{
	String line_id = "";
	String name = "";
	String color = "";
	Direction direction = null;
	@Override
	public String toString()
	{
		return String.format("%s- %s", line_id, name);
	}
}

class Direction implements Serializable
{
	String[] aller;
	String[] retour;
}

class StationLineInfo
{
	Direction boarding_ids;
	String stop_id;
}

class TimeTable
{
	Schedule[] horaire;
	Terminus[] terminus;
}

class Schedule {String time; String label; }

class Terminus {String label; String direction; }

class NextPassages
{
	boolean realtime_error;
	boolean realtime_empty;
	Passage[] realtime;

}

class Passage
{
	Line line;
	String destinationName;
	boolean realtime;
	String aimedDepartureTime;
	String expectedDepartureTime;

}