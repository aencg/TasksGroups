package com.example.tasksgroups.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

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
import com.example.tasksgroups.databinding.ActivityNewTaskBinding;
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

public class NewTaskActivity extends AppCompatActivity {

    //firebase references
    private FirebaseDatabase mFireBaseDatabase;
    private DatabaseReference mTasksDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    public static final int RC_SIGN_IN = 1;
    public static final int RC_REGULAR_FLOW = 50;
    public static final int RESULT_SIGN_OUT = 100;

    private ActivityNewTaskBinding binding;

    private DatabaseReference mConnectedRef;
    private ValueEventListener mConnectedListener;

    private static final String TAG = "tag";
    private String mGroupId;
    private boolean mIsConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_task);

        setTitle(R.string.new_task_activity_title);

        Intent intent = getIntent();
        if(intent.hasExtra("ID")){
            mGroupId= intent.getStringExtra("ID");
        }   else{
            finish();
        }
        //Log.e("onCreate","oncreate");

        //get the references
        mFireBaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mTasksDatabaseReference = mFireBaseDatabase.getReference().child("tasks").child(mGroupId);
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
        //check if user is connected to the internet
        mConnectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        mConnectedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {

                } else {
                    CustomToast.customToast(NewTaskActivity.this, R.string.need_connection, Toast.LENGTH_SHORT).show();
                    finish();
                }
                mIsConnected = connected;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Listener was cancelled");
            }
        };

        //setud the ui
        ArrayAdapter<CharSequence> adapterPriority = ArrayAdapter.createFromResource(this,
                R.array.priority_array, R.layout.spinner_item);
        adapterPriority.setDropDownViewResource(R.layout.spinner_item);
        binding.spPriority.setAdapter(adapterPriority);

        binding.bReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomToast.customToast(NewTaskActivity.this, R.string.cancel, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        //if the update button is clicked
        binding.bUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean error = false;
                //check if the info of the editTexts is correct
                if(binding.etTaskName.getText().toString().trim().equals("")){
                    CustomToast.customToast(NewTaskActivity.this, R.string.task_name_must_not_empty, Toast.LENGTH_SHORT).show();
                    error= true;
                }
                if(binding.etDescription.getText().toString().trim().equals("")){
                    CustomToast.customToast(NewTaskActivity.this, R.string.task_description_must_not_empty, Toast.LENGTH_SHORT).show();
                    error= true;
                }

                if(!error){
                    //if the info is correct, update the task in the real time db
                    String[] states = getResources().getStringArray(R.array.estate_array);
                    DatabaseReference push = mTasksDatabaseReference.push();
                    Task task= new Task(push.getKey().toString(),
                            binding.etTaskName.getText().toString(),
                            binding.etDescription.getText().toString(),
                            binding.spPriority.getSelectedItem().toString(),
                            states[0]);

                    push.setValue(task, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if(error==null){
                                CustomToast.customToast(NewTaskActivity.this, R.string.task_created_successfully,
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }   else{
                                CustomToast.customToast(NewTaskActivity.this, R.string.error_occurred_try_later,
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
    }

    private void onSignedInInitialize() {
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
        //check if user logged successfully
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