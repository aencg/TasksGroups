package com.example.tasksgroups.ui.activities;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;


import com.example.tasksgroups.R;
import com.example.tasksgroups.data.DummyData;
import com.example.tasksgroups.data.Group;
import com.example.tasksgroups.databinding.ActivityWidgetConfigBinding;
import com.example.tasksgroups.ui.CustomToast;
import com.example.tasksgroups.ui.adapters.GroupAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.View.VISIBLE;

public class ConfigWidgetActivity extends AppCompatActivity    {
    private static final String PREFS_NAME  = "com.example.tasksgroups";
    private static final String PREF_PREFIX_KEY = "prefix_";
    private static final String PREF_TEXT_KEY = "text_";

    RecyclerView recyclerView;
    GroupAdapter mGroupAdapter;
    List<Group> mGroups;
    int appWidgetId;

    //firebase references
    private FirebaseDatabase mFireBaseDatabase;
    private DatabaseReference mGroupsDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;


    public static final int RC_SIGN_IN = 1;

    ActivityWidgetConfigBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //init the ui
        binding = DataBindingUtil.setContentView(this, R.layout.activity_widget_config);
        setResult(RESULT_CANCELED);

        setTitle(R.string.title_widget_activity);

        recyclerView = (RecyclerView) findViewById(R.id.list_config_activity);
        mGroupAdapter = new GroupAdapter(new GroupAdapter.GroupAdapterOnClickHandler() {
            @Override
            public void onClick(Group groupClicked, View view) {
                showAppWidget(groupClicked.getId());

            }
        }, this);
        RecyclerView.LayoutManager layoutManager;
        mGroups = new ArrayList<Group>();

        layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mGroupAdapter);
        mGroupAdapter.setGroupsData(mGroups);


        mFireBaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mGroupsDatabaseReference = mFireBaseDatabase.getReference().child("groups");


        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    onSignedInInitialize();
                }   else{

                    finish();
                }
            }
        };

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //user signed
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                CustomToast.customToast(this, R.string.logged_successfully, Toast.LENGTH_SHORT).show();
                attachDatabaseReadListener();
            } else if (resultCode == RESULT_CANCELED) {
                // user no signed
                noGroups();

            }
        }
    }
    private void onSignedInInitialize() {
        attachDatabaseReadListener();
    }

    private void detachDatabaseReadListener(){
    }

    private void noGroups(){
        //make a dialog to show that we can't create the widget
        AlertDialog.Builder builder = new AlertDialog.Builder(ConfigWidgetActivity.this, R.style.AlertDialogStyle);
        builder.setMessage(R.string.not_possible_load_groups)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        }) ;
        // Create the AlertDialog object and return it
        builder.create().show();
    }
    private void attachDatabaseReadListener(){
        //load the groups from the Firebase real time db
            mGroupsDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Object object = snapshot.getValue();
                    if (object != null) {
                        //add the the readed groups to the adapter of the recycler view
                        Iterable<DataSnapshot> contactChildren = snapshot.getChildren();
                        ArrayList<Group> groups = new ArrayList<>();

                        for (DataSnapshot group : contactChildren) {
                            Group c = group.getValue(Group.class);
                            groups.add(c);
                        }
                        mGroups = groups;
                        mGroupAdapter.setGroupsData(mGroups);
                        binding.pbLoading.setVisibility(View.GONE);
                        binding.tvEmptyGroups.setVisibility(View.INVISIBLE);
                    }   else{
                        // if no groups
                        noGroups();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    binding.pbLoading.setVisibility(View.GONE);
                    noGroups();
                }
            });
    }

    private void showAppWidget(String groupId) {
        appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

        //Retrieve the App Widget ID from the Intent that launched the Activity//

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            //If the intent doesn’t have a widget ID, then call finish()//
            if(appWidgetId==AppWidgetManager.INVALID_APPWIDGET_ID){
                finish();
            }

            final Context context = ConfigWidgetActivity.this;

            //save the widget id and the id of the selected group
            saveGroupIdPref(context, appWidgetId, groupId);
            //loadGroupIdPref(context,appWidgetId);

            //Create the return intent//
            Intent resultValue = new Intent();

            //Pass the original appWidgetId//
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            //Log.e("configActivity", "appWidgetId" + appWidgetId);

            //Set the results from the configuration Activity//

            setResult(RESULT_OK, resultValue);

            //Finish the Activity//
            finish();
        }
    }




    @Override
    protected void onResume() {
        super.onResume();
        binding.pbLoading.setVisibility(VISIBLE);
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mAuthStateListener!= null){
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        mGroupAdapter.clear();
    }

    void loadUI(){
        mGroupAdapter.setGroupsData(mGroups);
        if(mGroups!=null && mGroups.size()!=0){
            //Log.e("groups",mGroups.toString());
        }
    }


    // Write the prefix to the SharedPreferences object for this widget
    protected static void saveGroupIdPref(Context context, int appWidgetId, String groupId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, groupId);
       // Log.e("save","appWidgetId "+appWidgetId +" groupId: "+groupId);
        prefs.commit();
    }
    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    public static String loadGroupIdPref(Context context, int appWidgetId) {

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String groupId = prefs.getString(PREF_PREFIX_KEY + appWidgetId, "");
       // Log.e("load","appWidgetId "+appWidgetId +" groupId: "+groupId);
        return groupId;
    }



    // Write the prefix to the SharedPreferences object for this widget
   public static void saveTextWidgetPref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_TEXT_KEY + appWidgetId, text);
        prefs.commit();
    }
    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    public static String loadTextWidgetPref(Context context, int appWidgetId) {

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String text = prefs.getString(PREF_TEXT_KEY + appWidgetId, "");
        return text;
    }

}
