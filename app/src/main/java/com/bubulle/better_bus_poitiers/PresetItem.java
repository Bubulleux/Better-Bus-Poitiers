package com.bubulle.better_bus_poitiers;

import java.io.Serializable;

public class PresetItem implements Serializable
{
	public String name;
	public String stationName;
	public DirectionPreset[] directions;
	public boolean isFavorite;

	public PresetItem(String  name, String stationName, DirectionPreset[] directions, boolean isFavorite)
	{
		this.name = name;
		this.stationName = stationName;
		this.directions = directions;
		this.isFavorite = isFavorite;
	}


}

