package com.example.tasksgroups.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.tasksgroups.R;
import com.example.tasksgroups.data.AddedFile;
import com.example.tasksgroups.data.Group;
import com.example.tasksgroups.data.User;
import com.example.tasksgroups.databinding.ActivityNewGroupBinding;
import com.example.tasksgroups.ui.CustomToast;
import com.example.tasksgroups.ui.adapters.AddedFileAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NewGroupActivity extends AppCompatActivity{

    //firebase references
    private FirebaseDatabase mFireBaseDatabase;
    private DatabaseReference mGroupsDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private DatabaseReference mUserDatabaseReference;
    private ValueEventListener mUserEventListener;

    public static final int RC_SIGN_IN = 1;
    public static final int RC_REGULAR_FLOW = 50;
    public static final int RESULT_SIGN_OUT = 100;

    private User mUser;

    ActivityNewGroupBinding binding;

    private DatabaseReference mConnectedRef;
    private ValueEventListener mConnectedListener;

    String TAG = "tag";

    private boolean mIsConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_group);

        setTitle(R.string.new_group_title);

        //get the references
        mFireBaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mGroupsDatabaseReference = mFireBaseDatabase.getReference().child("groups");
        if(mFirebaseAuth.getUid()!=null){
            mUserDatabaseReference = mFireBaseDatabase.getReference().child("users").child(mFirebaseAuth.getUid());
        }

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

        //check if user is connected
        mConnectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        mConnectedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {

                } else {
                    //if no connected, finish the activity
                    CustomToast.customToast(NewGroupActivity.this, R.string.need_connection, Toast.LENGTH_SHORT).show();
                    finish();
                }
                mIsConnected = connected;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Listener was cancelled");
            }
        };

        binding.bReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomToast.customToast(NewGroupActivity.this, R.string.cancel, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        binding.bCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if create button clicked
                //      check the edit texts
                boolean error = false;
                if(binding.etGroupName.getText().toString().trim().equals("")){
                    CustomToast.customToast(NewGroupActivity.this, R.string.group_name_must_not_empty, Toast.LENGTH_SHORT).show();
                    error= true;
                }
                if(binding.etDescription.getText().toString().trim().equals("")){
                    CustomToast.customToast(NewGroupActivity.this, R.string.group_description_must_not_empty, Toast.LENGTH_SHORT).show();
                    error= true;
                }
                if(!error){
                    //if the data is correct
                    // create the group object and create all the entries in the life time database
                    DatabaseReference push = mGroupsDatabaseReference.push();
                    String groupId = push.getKey().toString();
                    Group group = new Group(groupId,
                            binding.etGroupName.getText().toString(),
                            binding.etDescription.getText().toString(), mUser.getId());
                    Map<String, Object> groupMap = group.toMap();

                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put("/groups/" + groupId, groupMap);
                    childUpdates.put("/usersGroup/" + groupId+ "/" + mFirebaseAuth.getUid(), mUser);
                    childUpdates.put("/userGroups/"+mUser.getId()+"/member/"+groupId,true);
                    childUpdates.put("/users/"+mUser.getId(),mUser);

                    mFireBaseDatabase.getReference().updateChildren(childUpdates, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if(error==null){
                                CustomToast.customToast(NewGroupActivity.this, R.string.group_created_successfully,
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }   else{
                                push.removeValue();
                                CustomToast.customToast(NewGroupActivity.this, R.string.error_occurred_try_later,
                                        Toast.LENGTH_SHORT).show();
                                Log.e("error","newgroup: "+error.toString());
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
        if(mUserEventListener!= null){
            mUserDatabaseReference.removeEventListener(mUserEventListener);
            mUserEventListener=null;
        }
    }

    private void attachDatabaseReadListener(){
        //get the reference of user profile
        if(mUserEventListener==null){
            mUserEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if(user!=null){
                        mUser = user;
                    }

                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            };
            mUserDatabaseReference.addValueEventListener(mUserEventListener);
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
        detachDatabaseReadListener();
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