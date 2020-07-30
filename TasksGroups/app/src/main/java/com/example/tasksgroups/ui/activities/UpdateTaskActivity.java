package com.example.tasksgroups.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.tasksgroups.R;
import com.example.tasksgroups.data.AddedFile;
import com.example.tasksgroups.data.Task;
import com.example.tasksgroups.data.User;
import com.example.tasksgroups.databinding.ActivityUpdateTaskBinding;
import com.example.tasksgroups.ui.CustomToast;
import com.example.tasksgroups.ui.adapters.AddedFileAdapter;
import com.example.tasksgroups.ui.adapters.UserAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class UpdateTaskActivity extends AppCompatActivity  {

    //firebase references
    private FirebaseDatabase mFireBaseDatabase;
    private DatabaseReference mTaskDatabaseReference;
    private ValueEventListener mValueEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    ActivityUpdateTaskBinding binding;

    private DatabaseReference mConnectedRef;
    private ValueEventListener mConnectedListener;

    private String TAG = "tag";

    private String mGroupId;
    private String mTaskId;
    private Task mTask;

    private boolean mIsConnected;

    public static final int RC_SIGN_IN = 1;
    public static final int RC_REGULAR_FLOW = 50;
    public static final int RESULT_SIGN_OUT = 100;

    Bundle mSavedInstanceState;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_update_task);

        if(savedInstanceState!=null){
            mSavedInstanceState = savedInstanceState;
        }

        setTitle(R.string.update_task_activity_title);


        Intent intent = getIntent();
        if(intent.hasExtra("GROUPID")){
            mGroupId= intent.getStringExtra("GROUPID");
        }   else{
            finish();
        }

        if(intent.hasExtra("TASKID")){
            mTaskId= intent.getStringExtra("TASKID");
        }   else{
            finish();
        }



        //get the references
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFireBaseDatabase = FirebaseDatabase.getInstance();
        mTaskDatabaseReference = mFireBaseDatabase.getReference().child("tasks").child(mGroupId).child(mTaskId);

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //user is signed in
                    onSignedInInitialize();
                }   else{
                    Intent resultInt = new Intent();
                    resultInt.putExtra("Result", RESULT_SIGN_OUT);
                    setResult(RESULT_SIGN_OUT,resultInt);
                    finish();
                }
            }
        };

        mConnectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        mConnectedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {

                } else {
                    CustomToast.customToast(UpdateTaskActivity.this, R.string.need_connection, Toast.LENGTH_SHORT).show();
                    finish();
                }
                mIsConnected = connected;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Listener was cancelled");
            }
        };

        //setup the ui
        ArrayAdapter<CharSequence> adapterState = ArrayAdapter.createFromResource(this,
        R.array.estate_array, R.layout.spinner_item);
        adapterState.setDropDownViewResource(R.layout.spinner_item);
        binding.spEstate.setAdapter(adapterState);

        ArrayAdapter<CharSequence> adapterPriority = ArrayAdapter.createFromResource(this,
                R.array.priority_array, R.layout.spinner_item);
        adapterPriority.setDropDownViewResource(R.layout.spinner_item);
        binding.spPriority.setAdapter(adapterPriority);

        //if the delete button is clicked
        binding.bDeleteTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //ask for confirmation of the delete action
                AlertDialog.Builder builder = new AlertDialog.Builder(UpdateTaskActivity.this, R.style.AlertDialogStyle);
                builder.setMessage(R.string.are_sure_delete_task)
                        .setPositiveButton(R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        //delete the task
                                        mTaskDatabaseReference.setValue(null, new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                                if(error!=null){
                                                    CustomToast.customToast(UpdateTaskActivity.this, R.string.error_occurred_try_later,
                                                            Toast.LENGTH_SHORT).show();
                                                }   else{
                                                    //if deleted seccessfully, return to the previous activity
                                                    CustomToast.customToast(UpdateTaskActivity.this, R.string.task_deleted_successfully,
                                                            Toast.LENGTH_SHORT).show();
                                                    Intent resultInt = new Intent();
                                                    resultInt.putExtra("Result", Activity.RESULT_OK);
                                                    setResult(Activity.RESULT_OK,resultInt);
                                                    finish();
                                                }
                                            }
                                        });
                                    }
                                })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                CustomToast.customToast(UpdateTaskActivity.this, R.string.canceled,Toast.LENGTH_SHORT).show();

                            }
                        });
                // Create the AlertDialog object and return it
                builder.create().show();
            }
        });

        //if return button clicked, return to the previous activity without savings changes
        binding.bReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomToast.customToast(UpdateTaskActivity.this, R.string.cancel, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        //if update button clicked, save the changes made in the live time db
        binding.bUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check the edittexts
                boolean error = false;
                if(binding.etTaskName.getText().toString().trim().equals("")){
                    CustomToast.customToast(UpdateTaskActivity.this, R.string.task_name_must_not_empty, Toast.LENGTH_SHORT).show();
                    error= true;
                }
                if(binding.etDescription.getText().toString().trim().equals("")){
                    CustomToast.customToast(UpdateTaskActivity.this, R.string.task_description_must_not_empty, Toast.LENGTH_SHORT).show();
                    error= true;
                }
                if(!error) {
                    // if the info is correct update the task
                    String[] states = getResources().getStringArray(R.array.estate_array);
                    Task task = mTask;
                    //retrieve the info from the ui
                    task.setName(binding.etTaskName.getText().toString());
                    task.setDescription(binding.etDescription.getText().toString());
                    task.setPriority(binding.spPriority.getSelectedItem().toString());
                    task.setState(binding.spEstate.getSelectedItem().toString());
                    task.setId(mTaskId);
                    //send the update action to the db
                    mTaskDatabaseReference.setValue(task, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if(error==null){
                                CustomToast.customToast(UpdateTaskActivity.this, R.string.task_updated_successfully,
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }   else{
                                CustomToast.customToast(UpdateTaskActivity.this, R.string.error_occurred_try_later,
                                        Toast.LENGTH_SHORT).show();
                                Log.e("error","newtask: "+error.toString());
                            }
                        }
                    });
                }
            }
        });
    }

    private void onSignedOutCleanup() {
          detachDatabaseReadListener();
    }

    private void onSignedInInitialize() {
          attachDatabaseReadListener();
    }

    private void detachDatabaseReadListener(){
        if(mValueEventListener!= null){
            mTaskDatabaseReference.removeEventListener(mValueEventListener);
            mValueEventListener=null;
        }
    }

    private void attachDatabaseReadListener(){
        if(mValueEventListener==null){
            //retrive the info of the task and populate the ui
            mValueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(mSavedInstanceState!=null){
                        recoverUIFromSavedInstanceState(mSavedInstanceState);
                    }   else{
                        Task task = snapshot.getValue(Task.class);
                        if(task!=null){
                            mTask = task;
                            populateUI(task);
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("UpdateTask","database error: "+error.toString());
                }
            };

            mTaskDatabaseReference.addListenerForSingleValueEvent(mValueEventListener);
        }
    }

    protected  void populateUI(Task task){
        binding.etTaskName.setText((task.getName()));
        binding.etDescription.setText(task.getDescription());
        String[] stateArray = getResources().getStringArray(R.array.estate_array);
        for(int i=0; i<stateArray.length; i++){
            if(stateArray[i].equals(task.getState())){
                binding.spEstate.setSelection(i);
                break;
            }
        }

        String[] priorityArray = getResources().getStringArray(R.array.priority_array);
        for(int i=0; i<priorityArray.length; i++){
            if(priorityArray[i].equals(task.getPriority())){
                binding.spPriority.setSelection(i);
                break;
            }
        }
    }

    //save the ui info when the activity changes of state
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("taskName",  binding.etTaskName.getText().toString());
        outState.putString("taskDescription",  binding.etDescription.getText().toString());
        outState.putString("priority",  binding.spPriority.getSelectedItem().toString());
        outState.putString("estate", binding.spEstate.getSelectedItem().toString());
    }

    //re populate the ui
    private void recoverUIFromSavedInstanceState(Bundle savedInstanceState){
        if(savedInstanceState!=null) {
            if (savedInstanceState.containsKey("taskName")) {
                binding.etTaskName.setText(savedInstanceState.getString("taskName", ""));
            }
            if (savedInstanceState.containsKey("taskDescription")) {
                binding.etDescription.setText(savedInstanceState.getString("taskDescription", ""));
            }
            if (savedInstanceState.containsKey("priority")) {
                String priority = savedInstanceState.getString("priority");
                String[] priorityArray = getResources().getStringArray(R.array.priority_array);
                for (int i = 0; i < priorityArray.length; i++) {
                    if (priorityArray[i].equals(priority)) {
                        binding.spPriority.setSelection(i);
                        break;
                    }
                }
            }
            if (savedInstanceState.containsKey("estate")) {
                String estate = savedInstanceState.getString("estate");
                String[] estateArray = getResources().getStringArray(R.array.estate_array);
                for (int i = 0; i < estateArray.length; i++) {
                    if (estateArray[i].equals(estate)) {
                        binding.spEstate.setSelection(i);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        mConnectedRef.addValueEventListener(mConnectedListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mAuthStateListener!= null){
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        if(mConnectedListener!=null){
            mConnectedRef.removeEventListener(mConnectedListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //check if the user is logged
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                CustomToast.customToast(this, R.string.logged_successfull, Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                onSignedOutCleanup();
                Intent resultInt = new Intent();
                resultInt.putExtra("Result", RESULT_SIGN_OUT);
                setResult(RESULT_SIGN_OUT, resultInt);
                finish();
            }
        }
    }
}