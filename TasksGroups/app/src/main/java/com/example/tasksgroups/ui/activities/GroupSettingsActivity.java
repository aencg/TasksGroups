package com.example.tasksgroups.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.tasksgroups.R;
import com.example.tasksgroups.data.User;
import com.example.tasksgroups.databinding.ActivityGroupSettingsBinding;
import com.example.tasksgroups.ui.CustomToast;
import com.example.tasksgroups.ui.adapters.UserGroupSettingsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GroupSettingsActivity extends AppCompatActivity  {

    //adaptes and lists
    List<User> mUserRequests;
    UserGroupSettingsAdapter mUserRequestsAdapter;

    List<User> mCurrentUsers;
    UserGroupSettingsAdapter mCurrentUsersAdapter;

    //Firebase references
    private FirebaseDatabase mFireBaseDatabase;
    private DatabaseReference mUsersRequestDatabaseReference;
    private DatabaseReference mCurrentUsersDatabaseReference;
    private ChildEventListener mChildCurrentUsersEventListener;
    private ChildEventListener mChildUsersRequestEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    //connection reference
    private DatabaseReference mConnectedRef;
    private ValueEventListener mConnectedListener;

    public static final int RC_SIGN_IN = 1;
    public static final int RC_REGULAR_FLOW = 50;
    public static final int RESULT_SIGN_OUT = 100;
    public static final int RESULT_GROUP_DELETED = 200;

    String mGroupId;
    String mAdminId;

    ActivityGroupSettingsBinding binding;

    String TAG = "tag";

    private boolean mIsConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_group_settings);

        setTitle(R.string.group_settings_title);

        mCurrentUsers = new ArrayList<User>();
        mUserRequests = new ArrayList<User>();

        Intent intent = getIntent();
        if(intent.hasExtra("ID")){
            mGroupId= intent.getStringExtra("ID");
        }   else{
            finish();
        }
        if(intent.hasExtra("ADMIN_ID")){
            mAdminId = intent.getStringExtra("ADMIN_ID");
        }

        //get the references
        mFireBaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mUsersRequestDatabaseReference = mFireBaseDatabase.getReference().child("requests").child(mGroupId);
        mCurrentUsersDatabaseReference = mFireBaseDatabase.getReference().child("usersGroup").child(mGroupId);;
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //user is signed in
                    onSignedInInitialize();
                }   else{
                    //return to the previous activity
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
                //if connection lost, make visible a textview that hide the rest of the views
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    Log.d(TAG, "connected");
                    binding.tvLostConnection.setVisibility(View.INVISIBLE);

                } else {
                    Log.d(TAG, "not connected");
                    binding.tvLostConnection.setVisibility(View.VISIBLE);
                }
                mIsConnected = connected;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Listener was cancelled");
            }
        };

        //setup the requests recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        binding.recyclerInvitationRequests.setLayoutManager(linearLayoutManager);
        mUserRequestsAdapter = new UserGroupSettingsAdapter(
                new UserGroupSettingsAdapter.UserListRecyclerviewClickInterface() {
                    @Override
                    public void onItemClicked(int position, String tag) {
                        User user = mUserRequests.get(position);
                        String userId = user.getId();
                        Map<String, Object> childUpdates;
                        switch(tag){
                            case "delete":
                                CustomToast.customToast(GroupSettingsActivity.this, R.string.delete, Toast.LENGTH_SHORT).show();
                                childUpdates = new HashMap<>();
                                childUpdates.put("/requests/" + mGroupId+ "/"+userId, null);
                                childUpdates.put("/userGroups/"+userId+"/requests/"+mGroupId,null);

                                mFireBaseDatabase.getReference().updateChildren(childUpdates);
                                break;
                            case "admin":
                                CustomToast.customToast(GroupSettingsActivity.this, "admin", Toast.LENGTH_SHORT).show();
                                break;

                            case "confirm":
                                //the admin confirm the user request and authorizes access to the group
                                childUpdates = new HashMap<>();
                                childUpdates.put("/requests/" + mGroupId+"/"+userId, null);
                                childUpdates.put("/usersGroup/" + mGroupId+ "/"+userId, user);
                                childUpdates.put("/userGroups/"+userId+"/member/"+mGroupId,true);

                                mFireBaseDatabase.getReference().updateChildren(childUpdates, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                        if(error!=null){
                                            CustomToast.customToast(GroupSettingsActivity.this,
                                                    R.string.error_occurred,
                                                    Toast.LENGTH_SHORT).show();
                                            Log.e("error","click change to user : "+error.toString());
                                        }
                                    }
                                });
                                break;
                            default:
                             //   Toast.makeText(GroupSettingsActivity.this, "default", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                , this, UserGroupSettingsAdapter.MODE_REQUESTS);
        binding.recyclerInvitationRequests.setAdapter(mUserRequestsAdapter);
        mUserRequestsAdapter.setUsersData(mUserRequests);

        //setup the users recycler view
        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        binding.recyclerCurrentUsers.setLayoutManager(linearLayoutManager2);
        mCurrentUsersAdapter = new UserGroupSettingsAdapter(
                new UserGroupSettingsAdapter.UserListRecyclerviewClickInterface() {
                    @Override
                    public void onItemClicked(int position, String tag) {
                        switch(tag){
                            case "delete":
                                //delete the user from the group
                                CustomToast.customToast(GroupSettingsActivity.this, R.string.delete, Toast.LENGTH_SHORT).show();

                                String userId = mCurrentUsers.get(position).getId();
                                Map<String, Object> childUpdates = new HashMap<>();
                                childUpdates.put("/usersGroup/" + mGroupId+ "/"+userId, null);
                                childUpdates.put("/userGroups/"+userId+"/member/"+mGroupId,null);

                                mFireBaseDatabase.getReference().updateChildren(childUpdates);
                                break;
                            case "admin":
                                  CustomToast.customToast(GroupSettingsActivity.this, "admin", Toast.LENGTH_SHORT).show();
                                break;

                            case "confirm":
                              //  CustomToast.customToast(GroupSettingsActivity.this, "confirm", Toast.LENGTH_SHORT).show();
                                break;
                            default:
                             //   CustomToast.customToast(GroupSettingsActivity.this, "default", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                , this, UserGroupSettingsAdapter.MODE_ACTUAL);
        binding.recyclerCurrentUsers.setAdapter(mCurrentUsersAdapter);
        mCurrentUsersAdapter.setUsersData(mCurrentUsers);
        mCurrentUsersAdapter.setAdminId(mAdminId);

        //if the delete group button is pressed
        binding.bDeleteGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //alert dialog asking to confirm the delete action
                AlertDialog.Builder builder = new AlertDialog.Builder(GroupSettingsActivity.this, R.style.AlertDialogStyle);
                builder.setMessage(R.string.are_sure_delete_group)
                        .setPositiveButton(R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        //delete all the group data
                                        Map<String, Object> childUpdates = new HashMap<>();
                                        childUpdates.put("/groups/" + mGroupId, null);
                                        childUpdates.put("/usersGroup/" + mGroupId, null);
                                        childUpdates.put("/tasks/" + mGroupId, null);
                                        childUpdates.put("/requests/"+ mGroupId, null);
                                        if(mCurrentUsers!=null){
                                            for(User user : mCurrentUsers){
                                                childUpdates.put("/userGroups/"+user.getId()+"/member/"+mGroupId,null);
                                            }
                                        }

                                        if(mUserRequests!=null){
                                            for(User user : mUserRequests){
                                                childUpdates.put("/userGroups/"+user.getId()+"/requests/"+mGroupId,null);
                                            }
                                        }
                                        //TODO delete files

                                        mFireBaseDatabase.getReference().updateChildren( childUpdates,
                                                new DatabaseReference.CompletionListener() {
                                                    @Override
                                                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                                        if(error!=null){
                                                            CustomToast.customToast(GroupSettingsActivity.this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                                                        }   else{
                                                            CustomToast.customToast(GroupSettingsActivity.this, R.string.group_deleted, Toast.LENGTH_SHORT).show();
                                                            Intent resultInt = new Intent();
                                                            resultInt.putExtra("Result", RESULT_GROUP_DELETED);
                                                            setResult(RESULT_GROUP_DELETED,resultInt);
                                                            finish();
                                                        }
                                                    }
                                                });
                                    }
                                })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                CustomToast.customToast(GroupSettingsActivity.this, R.string.canceled,Toast.LENGTH_SHORT).show();

                            }
                        });
                // Create the AlertDialog object and return it
                builder.create().show();

            }
        });

        binding.bReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }



    private void onSignedOutCleanup() {

        mCurrentUsersAdapter.clear();
        mUserRequestsAdapter.clear();
        detachDatabaseReadListener();
    }

    private void onSignedInInitialize() {
          attachDatabaseReadListener();
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


    private void detachDatabaseReadListener(){
        if(mChildCurrentUsersEventListener!= null){
            mCurrentUsersDatabaseReference.removeEventListener(mChildCurrentUsersEventListener);
            mChildCurrentUsersEventListener=null;
        }
        if(mChildUsersRequestEventListener!= null){
            mUsersRequestDatabaseReference.removeEventListener(mChildUsersRequestEventListener);
            mChildUsersRequestEventListener=null;
        }
    }

    private void attachDatabaseReadListener(){
        if(mChildCurrentUsersEventListener==null){
            //retrieve the users to the recycler view
            mChildCurrentUsersEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                   // Log.e("attachchildcurrent",snapshot.toString());
                    User user = snapshot.getValue(User.class);
                    if(user!=null){
                        mCurrentUsers.add(user);
                        mCurrentUsersAdapter.notifyDataSetChanged();
                    }
                }
                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    User user = snapshot.getValue(User.class);
                    if(user!=null){
                        int length = mCurrentUsers.size();
                        for(int i = 0; i< length; i++){
                            if(user.getId().equals(mCurrentUsers.get(i).getId())){
                                mCurrentUsers.set(i,user);
                                break;
                            }
                        }
                        mCurrentUsersAdapter.notifyDataSetChanged();
                    }
                }
                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if(user!=null){
                        int length = mCurrentUsers.size();
                        for(int i = 0; i< length; i++){
                            if(user.getId().equals(mCurrentUsers.get(i).getId())){
                                mCurrentUsers.remove(mCurrentUsers.get(i));
                                break;
                            }
                        }
                        mCurrentUsersAdapter.notifyDataSetChanged();
                    }
                }
                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {


                }
            };
            mCurrentUsersDatabaseReference.addChildEventListener(mChildCurrentUsersEventListener);
        }
        if(mChildUsersRequestEventListener==null){
            //retrieve the users to the recycler view
            mChildUsersRequestEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    User user = snapshot.getValue(User.class);
                    if(user!=null){
                        mUserRequests.add(user);
                        mUserRequestsAdapter.notifyDataSetChanged();
                        binding.tvInvitationRequestsEmpty.setVisibility(View.INVISIBLE);
                    }

                }
                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    User user = snapshot.getValue(User.class);
                    if(user!=null){
                        int length = mUserRequests.size();
                        for(int i = 0; i< length; i++){
                            if(user.getId().equals(mUserRequests.get(i).getId())){
                                mUserRequests.set(i,user);
                                break;
                            }
                        }
                        mUserRequestsAdapter.notifyDataSetChanged();
                    }
                }
                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                  //  Toast.makeText(GroupSettingsActivity.this, "onchildRemoved", Toast.LENGTH_SHORT).show();

                    User user = snapshot.getValue(User.class);
                    if(user!=null){
                        int length = mUserRequests.size();
                        for(int i = 0; i< length; i++){
                            if(user.getId().equals(mUserRequests.get(i).getId())){
                                mUserRequests.remove(mUserRequests.get(i));
                                break;
                            }
                        }
                        if(mUserRequests.size()==0){
                            binding.tvInvitationRequestsEmpty.setVisibility(View.VISIBLE);
                        }
                        mUserRequestsAdapter.notifyDataSetChanged();
                    }


                }
                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                 //   Toast.makeText(GroupSettingsActivity.this, "onchildMoved", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {


                }
            };
            mUsersRequestDatabaseReference.addChildEventListener(mChildUsersRequestEventListener);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
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