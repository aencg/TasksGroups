package com.example.tasksgroups.ui;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.example.tasksgroups.R;
import com.example.tasksgroups.ui.activities.NewTaskActivity;

public class CustomToast {
    //wrapper to make a toast with a custom background color
    public static Toast customToast(Context context, CharSequence text, int duration ){
        Toast toast = Toast.makeText(context, text, duration);
        View view = toast.getView();
        view.setBackgroundResource(R.drawable.custom_toast);
        return  toast;
    }

    public static Toast customToast(Context context, int resid, int duration ){
        Toast toast = Toast.makeText(context, resid, duration);
        View view = toast.getView();
        view.setBackgroundResource(R.drawable.custom_toast);
        return toast;
    }
}
