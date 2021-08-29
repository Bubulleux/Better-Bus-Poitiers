package com.example.vitalisapp;

public class PresetItem {
	public String name;
	public String stationName;
	public String[] lineId;
	public boolean isFavorite;

	public PresetItem(String  name, String stationName, String lineId[], boolean isFavorite)
	{
		this.name = name;
		this.stationName = stationName;
		this.lineId = lineId;
		this.isFavorite = isFavorite;
	}
}
