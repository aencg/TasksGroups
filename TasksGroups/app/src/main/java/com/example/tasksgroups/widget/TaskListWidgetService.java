package com.example.tasksgroups.widget;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViewsService;


import com.example.tasksgroups.ui.activities.ConfigWidgetActivity;

public class TaskListWidgetService extends RemoteViewsService {

    private static final String GROUP_ID = "GROUP_ID";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
       // Log.e(getClass().getName(), "onGetViewFactory");
        Bundle extras = intent.getExtras();
        String groupId = "";
        int widgetId =-1;
        if (extras != null) {
           // Log.e("onGetViewFactory","extras"+extras.toString());
            Context context = this.getApplicationContext();

            widgetId  = extras.getInt(GROUP_ID, 0);
            groupId = ConfigWidgetActivity.loadGroupIdPref(context, widgetId);
        }
        TaskListRemoteViewsFactory factory = new TaskListRemoteViewsFactory(this.getApplicationContext(), groupId, widgetId);
        return factory;
    }
}

