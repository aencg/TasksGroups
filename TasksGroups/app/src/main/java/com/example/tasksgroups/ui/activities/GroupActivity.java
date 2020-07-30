package com.example.tasksgroups.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.example.tasksgroups.R;
import com.example.tasksgroups.data.Group;
import com.example.tasksgroups.data.Task;

import java.util.ArrayList;
import java.util.List;

import com.example.tasksgroups.databinding.ActivityGroupBinding;
import com.example.tasksgroups.ui.adapters.TaskAdapter;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class GroupActivity extends AppCompatActivity implements TaskAdapter.TaskAdapterOnClickHandler {

    TaskAdapter mTasksAdapter;
    String mGroupId;

    //real time db reference
    private FirebaseDatabase mFireBaseDatabase;
    private DatabaseReference mTasksDatabaseReference;
    private ChildEventListener mChildEventListener;


    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    //the parameters of the query change when a different option in the menu is selected
    private Query mQuery;

    //the selected option of the menu is stored in the shared preferences
    SharedPreferences mSharedPreferences;
    private Menu mMenu;
    private String mAdminId;

    public static final int RC_SIGN_IN = 1;
    public static final int RC_REGULAR_FLOW = 50;
    public static final int RESULT_SIGN_OUT = 100;
    public static final int RESULT_GROUP_DELETED = 200;


    ActivityGroupBinding binding;
    private String mUserId;

    //reference for conection
    private DatabaseReference mConnectedRef;
    private ValueEventListener mConnectedListener;

    String TAG = "tag";

    private boolean mIsConnected;

    private String mGroupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_group);

        Intent intent = getIntent();
        if(intent.hasExtra("ID")){
            mGroupId= intent.getStringExtra("ID");
        }   else{
            finish();
        }

        if(intent.hasExtra("NAME")){
            mGroupName= intent.getStringExtra("NAME");
        }   else{
            finish();
        }
        //Log.e("groupActivity","id group: "+mGroupId);

        setTitle(mGroupName);

        mSharedPreferences = getSharedPreferences("com.example.tasksgroups",MODE_PRIVATE);


        List<Task> mTasks = new ArrayList<Task>();

        //prepare the firebase references
        mFireBaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mTasksDatabaseReference = mFireBaseDatabase.getReference().child("tasks").child(mGroupId);


        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //user is signed in, attach the other listeners
                    onSignedInInitialize();
                }   else{
                    //if sign out return to the previous activity
                    onSignedOutCleanup();
                    Intent resultInt = new Intent();
                    resultInt.putExtra("Result", RESULT_SIGN_OUT);
                    setResult(RESULT_SIGN_OUT,resultInt);
                    finish();
                }
            }
        };
        //
        mConnectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        mConnectedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //check if user is connected
                try{
                    boolean connected = snapshot.getValue(Boolean.class);
                    if (connected) {
                        binding.tvLostConnection.setVisibility(View.INVISIBLE);

                    } else {
                        //display the textview that hides the rest of the views
                        binding.tvLostConnection.setVisibility(View.VISIBLE);
                    }
                    mIsConnected = connected;
                } catch(Exception e){
                    //Log.e(TAG, e.toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
               // Log.w(TAG, "Listener was cancelled");
            }
        };

        //setup the recyclerview
        mTasksAdapter = new TaskAdapter( this, this, getOrderAdapterTask());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        binding.recyclerTasks.setLayoutManager(linearLayoutManager);
        binding.recyclerTasks.setHasFixedSize(true);
        binding.recyclerTasks.setAdapter(mTasksAdapter);
        mTasksAdapter.setTasksData(mTasks);


        //if we click in the fab, start the new task activity
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Log.e("groupActivity","onclick groupId:"+mGroupId);
                Intent intent = new Intent(GroupActivity.this, NewTaskActivity.class);
                intent.putExtra("ID",mGroupId);
                startActivityForResult(intent,RC_REGULAR_FLOW);

            }
        });

    }



    private Query queryFromPrefs(){
        //make a new query from the options selected on the menu
        Query query = null;
        int filterBy = mSharedPreferences.getInt("FILTER_BY"+mGroupId,R.id.show_all_option_menu);

        switch(filterBy) {
            case R.id.only_available_option_menu:
                query = mTasksDatabaseReference.getRef().orderByChild("state").equalTo("Available");
                break;
            case R.id.only_assigned_option_menu:
                query = mTasksDatabaseReference.getRef().orderByChild("state").equalTo("Assigned");
                break;
            case R.id.only_completed_option_menu:
                query = mTasksDatabaseReference.getRef().orderByChild("state").equalTo("Completed");
                break;
            case R.id.show_all_option_menu:
            default:
                query = mTasksDatabaseReference.getRef().orderByChild("id");
        }
        return query;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //check if the is logged in
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                //  Toast.makeText(this, R.string.logged_successfull, Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Intent resultInt = new Intent();
                resultInt.putExtra("Result", RESULT_SIGN_OUT);
                setResult(RESULT_SIGN_OUT,resultInt);
                finish();
            }
        } else if(requestCode == RC_REGULAR_FLOW){
            //check if the child activity has returned because the user has signed out
            if(resultCode == RESULT_SIGN_OUT){
                Intent resultInt = new Intent();
                resultInt.putExtra("Result", RESULT_SIGN_OUT);
                setResult(RESULT_SIGN_OUT,resultInt);
                finish();
            }
            //check if the child activity has returned because the we have deleted the group
            if(resultCode == RESULT_GROUP_DELETED){
                Intent resultInt = new Intent();
                resultInt.putExtra("Result", RESULT_GROUP_DELETED);
                setResult(RESULT_GROUP_DELETED,resultInt);
                finish();
            }
        }
    }


    private void onSignedOutCleanup() {
        mTasksAdapter.clear();
        detachDatabaseReadListener();
    }
    private void onSignedInInitialize( ) {
        mUserId = mFirebaseAuth.getUid();
        attachDatabaseReadListener();
    }

    private void detachDatabaseReadListener(){
        if(mChildEventListener!= null){
            //mTasksDatabaseReference.removeEventListener(mChildEventListener);
            if(mQuery!=null){
                mQuery.removeEventListener(mChildEventListener);
                mChildEventListener=null;
            }
        }
    }

    private void attachDatabaseReadListener(){

        mFireBaseDatabase.getReference().child("groups").child(mGroupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Group group = snapshot.getValue(Group.class);
                if(group!=null){
                    mAdminId = group.getAdminId();
                    if(mAdminId!=null && mAdminId.equals(mUserId)){
                        if(mMenu!=null) mMenu.findItem(R.id.group_info_option_menu).setVisible(true);

                    }   else{
                        if(mMenu!=null) mMenu.findItem(R.id.group_info_option_menu).setVisible(false);
                    }
                }   else{
                    //the group has been deleted
                    Intent resultInt = new Intent();
                    resultInt.putExtra("Result", RESULT_GROUP_DELETED);
                    setResult(RESULT_GROUP_DELETED,resultInt);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

            //query the tasks of the group once to know if there is any task
            mQuery = queryFromPrefs();
            mQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object object = snapshot.getValue();
                if(object==null){
                    binding.pbLoading.setVisibility(View.GONE);
                    binding.tvEmptyTasks.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        if(mChildEventListener==null){

            //create the child event listener to retrieve the tasks from real time database
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    binding.pbLoading.setVisibility(View.INVISIBLE);
                    Task task = snapshot.getValue(Task.class);
                    if(task!=null){
                        mTasksAdapter.addTask(task);
                        //Log.e("onChildAdded",mTasks.toString());
                        binding.tvEmptyTasks.setVisibility(View.INVISIBLE);
                    }

                }
                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                   Task task = snapshot.getValue(Task.class);
                    if(task!=null){
                        int position = mTasksAdapter.getPositionTask(task.getId());
                        if(position!=-1){

                            mTasksAdapter.setTask(position, task);
                        }
                    }

                }
                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    Task task = snapshot.getValue(Task.class);
                    if(task!=null){
                        int position = mTasksAdapter.getPositionTask(task.getId());
                        if(position!=-1){
                            mTasksAdapter.removeTask(position);
                            mTasksAdapter.notifyDataSetChanged();
                        }
                    }
                    if(mTasksAdapter.getItemCount() ==0){
                        binding.tvEmptyTasks.setVisibility(View.VISIBLE);
                    }

                }
                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {


                }
            };
//            mTasksDatabaseReference.addChildEventListener(mChildEventListener);
           mQuery.addChildEventListener(mChildEventListener);
        }
    }

    public void singOut(Context context){
        AuthUI.getInstance().signOut(context);
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.pbLoading.setVisibility(View.VISIBLE);
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        mConnectedRef.addValueEventListener(mConnectedListener);

    }

    @Override
    protected void onPause() {
        super.onPause();
        //detach the listeners
        if(mConnectedListener!=null){
            mConnectedRef.removeEventListener(mConnectedListener);
        }
        if(mAuthStateListener!= null){
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        mTasksAdapter.clear();
    }


    //if we click in one of the tasks of the recycler, we start the updateTask Activity
    @Override
    public void onClick(Task taskClicked, View view) {

        Intent intent = new Intent(GroupActivity.this, UpdateTaskActivity.class);
        intent.putExtra("TASKID",taskClicked.getId());
        intent.putExtra("GROUPID",mGroupId);

        startActivityForResult(intent,RC_REGULAR_FLOW);
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate the menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.group_menu, menu);
        mMenu = menu;

        //read the saved preferences and check the needed option
       int id_priority = mSharedPreferences.getInt("ORDER_BY"+mGroupId,R.id.priority_option_menu);
       MenuItem priority = menu.findItem(id_priority);
       if(priority==null){
           priority = menu.findItem(R.id.priority_option_menu);
       }
       priority.setChecked(true);

        //read the saved preferences and check the needed option
       int id_filter = mSharedPreferences.getInt("FILTER_BY"+mGroupId,R.id.show_all_option_menu);
       MenuItem filter = menu.findItem(id_filter);
       if(filter==null){
           filter= menu.findItem(R.id.show_all_option_menu);
       }
        filter.setChecked(true);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MenuItem menuItem;
        Intent intent;

        //check the selected item
        int filterBy = mSharedPreferences.getInt("FILTER_BY"+mGroupId,R.id.show_all_option_menu);
        switch(item.getItemId()) {
            case R.id.group_info_option_menu:
                //start the admin groupsettings activity
                intent = new Intent(this, GroupSettingsActivity.class);
                intent.putExtra("ID",mGroupId);
                intent.putExtra("ADMIN_ID",mAdminId);
                startActivityForResult(intent,RC_REGULAR_FLOW);
                return true;
            case R.id.users_menu_option:
                //start the users activity
                intent = new Intent(GroupActivity.this, UsersActivity.class);
                intent.putExtra("ID",mGroupId);
                startActivityForResult(intent,RC_REGULAR_FLOW);
                return true;

            case R.id.files_option_menu:
                //start the files activity
                intent = new Intent(GroupActivity.this, FilesActivity.class);
                intent.putExtra("ID",mGroupId);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(intent,RC_REGULAR_FLOW);
                return true;

            case R.id.show_all_option_menu:
            case R.id.only_available_option_menu:
            case R.id.only_assigned_option_menu:
            case R.id.only_completed_option_menu:
                mSharedPreferences.edit().putInt("FILTER_BY"+mGroupId,item.getItemId()).apply();
                menuItem= mMenu.findItem(item.getItemId());
                menuItem.setChecked(true);
                //item selected query option has changed, make a new query
                if(filterBy != item.getItemId()) {
                    binding.pbLoading.setVisibility(View.VISIBLE);
                    mTasksAdapter.clear();
                    detachDatabaseReadListener();
                    //
                    attachDatabaseReadListener();
                }
                return true;
            //filter the tasks in the adapter of the recycler view
            case R.id.priority_option_menu:
                //filter the tasks by priority
                mSharedPreferences.edit().putInt("ORDER_BY"+mGroupId,R.id.priority_option_menu).apply();
                menuItem= mMenu.findItem(R.id.priority_option_menu);
                menuItem.setChecked(true);
                mTasksAdapter.setComparator(TaskAdapter.COMPARE_PRIORITY);
                return true;
            case R.id.state_option_menu:
                //filter the tasks by priority
                mSharedPreferences.edit().putInt("ORDER_BY"+mGroupId,R.id.state_option_menu).apply();
                menuItem = mMenu.findItem(R.id.state_option_menu);
                mTasksAdapter.setComparator(TaskAdapter.COMPARE_STATE);
                menuItem.setChecked(true);
                return true;
            case R.id.id_option_menu:
                //filter the tasks by priority
                    mSharedPreferences.edit().putInt("ORDER_BY"+mGroupId,R.id.id_option_menu).apply();
                    menuItem = mMenu.findItem(R.id.id_option_menu);
                    mTasksAdapter.setComparator(TaskAdapter.COMPARE_STATE);
                    menuItem.setChecked(true);
                    return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //retrieve the order option saved in preferences
    private int getOrderAdapterTask(){
        int id_priority = mSharedPreferences.getInt("ORDER_BY"+mGroupId,R.id.priority_option_menu);
        int retorno;
        switch (id_priority) {
            case R.id.priority_option_menu:
                retorno = TaskAdapter.COMPARE_PRIORITY;
                break;
            case R.id.state_option_menu:
                retorno = TaskAdapter.COMPARE_STATE;
                break;
            default:
                retorno = TaskAdapter.COMPARE_ID;
        }
        return retorno;
    }

}