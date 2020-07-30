package com.example.tasksgroups.widget;

/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.app.ActivityManager;
import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.tasksgroups.R;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class TaskGroupWidgetService extends IntentService {

   public static final String ACTION_UPDATE_TASK_WIDGETS = "com.example.tasksgroups.action.update_task_widgets";

    public TaskGroupWidgetService() {
        super("TaskUpdateService");
    }

    public static final String WIDGET_ID = "WIDGET_ID";
    public static final String TEXT = "TEXT";

    /**
     * Starts this service to perform UpdatePlantWidgets action with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpdateTaskWidgets(Context context) {
//        if(!isMyServiceRunning(TaskGroupWidgetService.class, context)){
            Intent intent = new Intent(context, TaskGroupWidgetService.class);
            intent.setAction(ACTION_UPDATE_TASK_WIDGETS);

            context.startService(intent);
//        }
    }

    private static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if(manager!=null){
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(service!=null && serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }}
        return false;

    }

    /**
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
     //   Log.e(getClass().getName(), "onHandleIntent");
        if (intent != null) {
            String action = intent.getAction();
            if(action.equals(ACTION_UPDATE_TASK_WIDGETS)){
                handleActionUpdateTasksWidgets();
            }
        }
    }

    /**
     * Handle action UpdatePlantWidgets in the provided background thread
     */

    private void handleActionUpdateTasksWidgets() {
       // Log.e(getClass().getName(), "handleActionUpdateTasksWidgets");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, TaskGroupWidgetProvider.class));
        //Trigger data update to handle the GridView widgets and force a data refresh
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.list_view_widget);
        //Now update all widgets
        TaskGroupWidgetProvider.updateWidgets(this, appWidgetManager  ,appWidgetIds);
    }
}
