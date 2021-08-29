package com.example.vitalisapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

public class UpdateAdapter<T> extends BaseAdapter
{
	private final LayoutInflater inflater;
	private final List<T> list;
	private final IUpdateAdapter<T> interfaceAdapter;


	public UpdateAdapter(Context context, List<T> list, IUpdateAdapter<T> interfaceAdapter)
	{
		this.inflater = LayoutInflater.from(context);
		this.list = list;
		this.interfaceAdapter = interfaceAdapter;
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public T getItem(int i) {
		return list.get(i);
	}

	@Override
	public long getItemId(int i)
	{
		return 0;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		return interfaceAdapter.getView(i, view, viewGroup, inflater, list);
	}

	public interface IUpdateAdapter<T>
	{
		View getView(int i, View view, ViewGroup viewGroup, LayoutInflater inflater, List<T> list);
	}
}
