package com.example.tasksgroups.widget;

import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.tasksgroups.R;
import com.example.tasksgroups.data.DummyData;
import com.example.tasksgroups.data.Group;
import com.example.tasksgroups.data.Task;
import com.example.tasksgroups.ui.activities.ConfigWidgetActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

class TaskListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private List<Task> mTasks = new ArrayList<Task>();
    private Context mContext;
    int mWidgetId;

    String mGroupId;

    FirebaseAuth auth;

    ValueEventListener mValueEventListener;
    DatabaseReference mDatabaseReference;


    public TaskListRemoteViewsFactory(Context context, String groupId, int widgetId) {
   //     Log.e(getClass().getName(), "constructor");
        mContext = context;
        mWidgetId = widgetId;

        mGroupId = groupId;
    //    Log.e("constructor","groupId"+mGroupId);

    }



    @Override
    public void onCreate() {
        if(mDatabaseReference!=null && mValueEventListener!=null){
            mDatabaseReference.removeEventListener(mValueEventListener);
            mValueEventListener = null;
       //     Log.e(getClass().getName(), "oncreate no era null");
        }
     //   Log.e(getClass().getName(), "onCreate widget:"+mWidgetId+" group:"+mGroupId);
       initializeData();
    }

    public void onDestroy() {
       // Log.e(getClass().getName(), "onDestroy");
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.

        if(mDatabaseReference!=null && mValueEventListener!=null){
            mDatabaseReference.removeEventListener(mValueEventListener);
            mValueEventListener = null;
        }
    }

    @Override
    public int getCount() {
      //  Log.e(getClass().getName(),"getCount");
        if (mTasks == null) {
         //   Log.e(getClass().getName(),"getCount 0");
            return 0;
        }
       // Log.e(getClass().getName(),"getCount "+ mTasks.size());
        return mTasks.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
      //  Log.e(getClass().getName(),"getViewAt");

        if (mTasks == null || mTasks.size() == 0) {
          //  Log.e(getClass().getName(),"getViewAt task 0 null");
            return null;
        }

        // position will always range from 0 to getCount() - 1.
        // We construct a remote views item based on our widget item xml file, and set the
        // text based on the position.
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_list_item);
        Task task = mTasks.get(position);


        //String text = (ingredient.getId()+1)+" "+ingredient.getDescription();
        String text = task.getName()+ " - " + task.getPriority();
        rv.setTextViewText(R.id.appwidget_item_text, text); //mSteps.get(position).text);

      //  Log.e(getClass().getName(),"getViewAt return view");

        // Return the remote views object.
        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
      //  Log.e(getClass().getName(), "getLoadingView");
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void initializeData() throws NullPointerException {

       // Log.e(getClass().getName(),"initializeData");
        mGroupId = ConfigWidgetActivity.loadGroupIdPref(mContext,mWidgetId);
        try {

            auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();
            if(user!=null){
                String userId = auth.getCurrentUser().getUid();
                DatabaseReference mainNode = FirebaseDatabase.getInstance().getReference();

                DatabaseReference childNode = mainNode.child("tasks").child(mGroupId);
                mDatabaseReference = childNode;
                if(mValueEventListener==null){
                    mValueEventListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                           // Log.e("group id",mGroupId);
                        //    Log.e("onDataChange","dataSnap Value: "+dataSnapshot.getValue());
                            if(dataSnapshot.getValue()!=null){
                                mTasks.clear();

                                Iterable<DataSnapshot> contactChildren = dataSnapshot.getChildren();
                                for (DataSnapshot snapshot : contactChildren) {
                                    Task task = snapshot.getValue(Task.class);
                                    //Log.e("onDataChange","dataSnap task: "+task.toString());
                                    //  Log.e("onDataChange","dataSnap snapitem: "+snapshot.toString());
                                    //Log.e("onDataChange","dataSnap task:"+task.toString());
                                    mTasks.add(task);
                                }


                            }   else{
                                mTasks.clear();
                                ConfigWidgetActivity.saveTextWidgetPref(mContext, mWidgetId, mContext.getText(R.string.no_tasks_in_group).toString());
//                                TaskGroupWidgetService.startActionUpdateTextEmpty(mContext
//                                        ,mWidgetId,mContext.getText(R.string.no_tasks_in_group).toString());
                            }
                            TaskGroupWidgetService.startActionUpdateTaskWidgets(mContext);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            mTasks.clear();
                            ConfigWidgetActivity.saveTextWidgetPref(mContext, mWidgetId, mContext.getText(R.string.error_occurred).toString());
//                            TaskGroupWidgetService.startActionUpdateTextEmpty(mContext,mWidgetId,
//                                    mContext.getText(R.string.error_occurred).toString());
                        }
                    };
                    childNode.addValueEventListener(mValueEventListener );
                }


            } else{
                mTasks.clear();
               // mTasks.add(new Task("","You must be signed to see data","","",""));
                ConfigWidgetActivity.saveTextWidgetPref(mContext, mWidgetId, mContext.getText(R.string.error_occurred).toString());

//                TaskGroupWidgetService.startActionUpdateTextEmpty(mContext,mWidgetId,
//                        mContext.getText(R.string.error_occurred).toString());
            }


        }catch (Exception e){
          // Log.e("TaksListWidgetService","error "+ e.toString());
        }


    }



    @Override
    public void onDataSetChanged() {
       // Log.e(getClass().getName(), "onDataSetChanged widget:"+mWidgetId+" group:"+mGroupId);
       initializeData();
    }
}