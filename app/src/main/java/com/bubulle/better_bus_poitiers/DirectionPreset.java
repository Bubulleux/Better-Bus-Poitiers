package com.bubulle.better_bus_poitiers;

import java.io.Serializable;

public class DirectionPreset implements Serializable
{
	public String line_id;
	public String direction;
	public String[] terminus;
	public String line_color;

	public DirectionPreset(String line_id, String direction, String[] terminus, String line_color)
	{

		this.line_id = line_id;
		this.direction = direction;
		this.terminus = terminus;
		this.line_color = line_color;
	}
}
