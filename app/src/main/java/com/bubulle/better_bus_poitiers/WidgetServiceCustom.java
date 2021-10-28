package com.bubulle.better_bus_poitiers;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.List;

public class WidgetServiceCustom<T> extends RemoteViewsService
{
	protected GetRemoteViewCustom<T> remoteViewCustomInterface;
	protected List<T> list;
	
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent)
	{
		
		return new WidgetItemFactoryCustom(getApplicationContext(), list,
				intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID),
				remoteViewCustomInterface);
	}
	
	class WidgetItemFactoryCustom implements RemoteViewsFactory
	{
		private final Context context;
		private final int appWidgetId;
		private final List<T> list;
		private final GetRemoteViewCustom<T> remoteViewsInterface;
		
		public WidgetItemFactoryCustom(Context context, List<T> list, int appWidgetId, GetRemoteViewCustom<T> remoteViewsInterface)
		{
			this.context = context;
			this.appWidgetId = appWidgetId;
			this.list = list;
			this.remoteViewsInterface = remoteViewsInterface;
			System.out.println("Widget Service Instantiate");
		}
		
		@Override
		public void onCreate()
		{
			
			System.out.println("Widget Service Create");
		}
		
		@Override
		public void onDataSetChanged()
		{
		
		}
		
		@Override
		public void onDestroy()
		{
		
		}
		
		@Override
		public int getCount()
		{
			return list.size();
		}
		
		@Override
		public RemoteViews getViewAt(int position)
		{
			return remoteViewsInterface.getViewAt(position, list, context, appWidgetId);
		}
		
		@Override
		public RemoteViews getLoadingView()
		{
			return remoteViewsInterface.getLoadingViews(context, appWidgetId);
		}
		
		@Override
		public int getViewTypeCount()
		{
			return 1;
		}
		
		@Override
		public long getItemId(int position)
		{
			return position;
		}
		
		@Override
		public boolean hasStableIds()
		{
			return true;
		}
	}
	
	interface GetRemoteViewCustom<T>
	{
		RemoteViews getViewAt(int position, List<T> list, Context context, int id);
		RemoteViews getLoadingViews(Context context, int id);
	}
}

