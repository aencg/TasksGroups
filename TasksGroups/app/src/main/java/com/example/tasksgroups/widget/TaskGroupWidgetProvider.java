package com.example.tasksgroups.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.tasksgroups.R;
import com.example.tasksgroups.ui.activities.ConfigWidgetActivity;
import com.example.tasksgroups.ui.activities.GroupsActivity;

/**
 * Implementation of App Widget functionality.
 */
public class TaskGroupWidgetProvider extends AppWidgetProvider {

    private static final String GROUP_ID = "GROUP_ID";

  //  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,  int appWidgetId) {

        String groupId = ConfigWidgetActivity.loadGroupIdPref(context, appWidgetId);


        //Log.e("RecipeWidgetProvider", "updateAppWidget appWidgetId "+appWidgetId+" pref "+groupId);
        RemoteViews rv = getListRemoteViews(context, appWidgetId);
        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
       // Log.e(getClass().getName(), "onUpdate");
        //Start the intent service update widget action, the service takes care of updating the widgets UI

       TaskGroupWidgetService.startActionUpdateTaskWidgets(context);
    }

    /**
     * Updates all widget instances given the widget Ids and display information
     *
     * @param context          The calling context
     * @param appWidgetManager The widget manager
     * @param appWidgetIds     Array of widget Ids to be updated
     */
    public static void updateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }


    /**
     * Creates and returns the RemoteViews to be displayed in the GridView mode widget
     *
     * @param context The context
     * @return The RemoteViews for the GridView mode widget
     */
    private static RemoteViews getListRemoteViews(Context context, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_list_view);
        // Set the GridWidgetService intent to act as the adapter for the GridView
        Intent intent = new Intent(context, TaskListWidgetService.class);
        intent.putExtra(GROUP_ID, widgetId);
        intent.setData(Uri.fromParts("content", String.valueOf(widgetId), null));

        views.setRemoteAdapter(R.id.list_view_widget, intent);

        Intent appIntent = new Intent(context, GroupsActivity.class);
        PendingIntent appPendingIntent = PendingIntent.getActivity(context, 0,
                appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.list_view_widget, appPendingIntent);



        // Handle empty gardens
        views.setEmptyView(R.id.list_view_widget, R.id.empty_widget_view);


        String text = ConfigWidgetActivity.loadTextWidgetPref(context, widgetId);
        if(text.equals("")){
            views.setTextViewText(R.id.empty_widget_view,context.getString(R.string.empty_tasks));

        }   else{
            views.setTextViewText(R.id.empty_widget_view,text);
        }
        views.setInt(R.id.empty_widget_view,"setBackgroundResource",
                R.drawable.round_button_user_group_config);
        views.setInt(R.id.empty_widget_view,"setBackgroundResource",
                R.drawable.round_button_user_group_config);
        views.setInt(R.id.list_view_widget,"setBackgroundResource",
                R.drawable.round_button_user_group_config);
        return views;
    }

   // @TargetApi(Build.VERSION_CODES.JELLY_BEAN)

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        TaskGroupWidgetService.startActionUpdateTaskWidgets(context);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }


}

