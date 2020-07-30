package com.example.tasksgroups.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.GridLayoutManager;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.tasksgroups.R;
import com.example.tasksgroups.data.User;
import com.example.tasksgroups.databinding.ActivityUsersBinding;
import com.example.tasksgroups.ui.CustomToast;
import com.example.tasksgroups.ui.adapters.UserAdapter;
import com.firebase.ui.auth.AuthUI;
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

public class UsersActivity extends AppCompatActivity implements UserAdapter.UserAdapterOnClickHandler {

    //firebase references
    private FirebaseDatabase mFireBaseDatabase;
    private DatabaseReference mUsersDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private DatabaseReference mConnectedRef;
    private ValueEventListener mConnectedListener;

    private ActivityUsersBinding binding;

    private String mGroupId;
    private UserAdapter mUsersAdapter;
    private List<User> mUsers;
    String TAG = "tag";
    private boolean mIsConnected;

    public static final int RC_SIGN_IN = 1;
    public static final int RC_REGULAR_FLOW = 50;
    public static final int RESULT_SIGN_OUT = 100;
    public static final int RESULT_GROUP_DELETED = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_users);

        setTitle(R.string.users_activity_title);

        Intent intent = getIntent();
        if(intent.hasExtra("ID")){
            mGroupId= intent.getStringExtra("ID");
        }   else{
            finish();
        }

        mUsers = new ArrayList<User>();

        //get the references
        mFireBaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mUsersDatabaseReference = mFireBaseDatabase.getReference().child("usersGroup").child(mGroupId);
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
                    binding.tvLostConnection.setVisibility(View.INVISIBLE);

                } else {
                    //if no connected, finish the activity
                    binding.tvLostConnection.setVisibility(View.VISIBLE);
                }
                mIsConnected = connected;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Listener was cancelled");
            }
        };

        //setup the ui
        mUsersAdapter = new UserAdapter( this, this);
        int spanCount = 2;
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape
            spanCount=3;
        }

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this,spanCount);
        binding.recyclerUsers.setLayoutManager(gridLayoutManager);
        binding.recyclerUsers.setHasFixedSize(true);
        binding.recyclerUsers.setAdapter(mUsersAdapter);
        mUsersAdapter.setUsersData(mUsers);
    }

    private void onSignedInInitialize() {
        attachDatabaseReadListener();
    }

    private void detachDatabaseReadListener(){
        if(mChildEventListener!= null){
            mUsersDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener=null;
        }
    }

    private void onSignedOutCleanup() {
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener(){
        //check if there is any user in the group
        mUsersDatabaseReference.getRef().addListenerForSingleValueEvent(new ValueEventListener() {
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
            //read the users of the group to populate the recycler
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    binding.pbLoading.setVisibility(View.INVISIBLE);
                    User user = snapshot.getValue(User.class);
                    if(user!=null){
                        mUsers.add(user);
                        mUsersAdapter.notifyDataSetChanged();
                        binding.tvEmptyTasks.setVisibility(View.INVISIBLE);
                    }

                }
                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    User user = snapshot.getValue(User.class);
                    if(user!=null){
                        int position = mUsersAdapter.getPositionUser(user.getId());
                        if(position!=-1){
                            mUsers.set(position, user);
                            mUsersAdapter.notifyDataSetChanged();
                        }
                    }

                }
                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if(user!=null){
                        int position = mUsersAdapter.getPositionUser(user.getId());
                        if(position!=-1){
                            mUsers.remove(position);
                            mUsersAdapter.notifyDataSetChanged();
                        }
                    }
                    if(mUsers.size()==0){
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
            mUsersDatabaseReference.addChildEventListener(mChildEventListener);
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
        if(mAuthStateListener!= null){
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        if(mConnectedListener!=null){
            mConnectedRef.removeEventListener(mConnectedListener);
        }
        detachDatabaseReadListener();
        mUsersAdapter.clear();
    }

    @Override
    public void onClick(User userClicked, View view) {

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