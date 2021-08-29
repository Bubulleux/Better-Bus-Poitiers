package com.example.vitalisapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class CustomAdapter extends BaseAdapter {
	private LayoutInflater inflater;

	public CustomAdapter(Context conctex)
	{
		inflater = LayoutInflater.from(conctex);
	}
	@Override
	public int getCount() {
		return 0;
	}

	@Override
	public Object getItem(int i) {
		return null;
	}

	@Override
	public long getItemId(int i) {
		return 0;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		return inflater.inflate(R.layout.preset_item, null);
	}
}
