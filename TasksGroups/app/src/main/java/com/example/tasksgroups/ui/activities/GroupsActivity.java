package com.example.tasksgroups.ui.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.tasksgroups.R;
import com.example.tasksgroups.data.Group;
import com.example.tasksgroups.data.User;
import com.example.tasksgroups.databinding.ActivityGroupsBinding;
import com.example.tasksgroups.ui.CustomToast;
import com.example.tasksgroups.ui.adapters.GroupAdapter;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.View.VISIBLE;

public class GroupsActivity extends AppCompatActivity implements GroupAdapter.GroupAdapterOnClickHandler {

    GroupAdapter mGroupAdapter;
    List<Group> mGroups;

    ActivityGroupsBinding binding;

    //Firebase references
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private FirebaseDatabase mFireBaseDatabase;
    private DatabaseReference mGroupsDatabaseReference;
    private DatabaseReference mRequestsDatabaseReference;
    private DatabaseReference mUserDatabaseReference;
    private DatabaseReference mConnectedRef;

    private ChildEventListener mChildGroupsListener;

    private ValueEventListener mUserEventListener;
    private ValueEventListener mConnectedListener;



    String TAG = "tag";
    private User mUser;

    private boolean mIsConnected;
    public static final int RC_SIGN_IN = 1;
    public static final int RC_REGULAR_FLOW = 50;
    public static final int RESULT_SIGN_OUT = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_groups);

        mGroups = new ArrayList<Group>();

        binding.tvLostConnection.setVisibility(View.INVISIBLE);

        //get the references
        mFireBaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mGroupsDatabaseReference = mFireBaseDatabase.getReference().child("groups");
        mRequestsDatabaseReference = mFireBaseDatabase.getReference().child("requests");

        mConnectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        mConnectedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //user is signed in
                    onSignedInInitialize();
                } else {
                    //create the Firebase sign in UI and start the activity for result
                    startFirebaseSignInActivity();
                }
            }
        };


        //setup the recyvler view of the groups
        mGroupAdapter = new GroupAdapter(this, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        binding.recyclerGroups.setLayoutManager(linearLayoutManager);
        binding.recyclerGroups.setHasFixedSize(true);
        binding.recyclerGroups.setAdapter(mGroupAdapter);
        mGroupAdapter.setGroupsData(mGroups);

        //if we click in the fab, start the activity to create a new group
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GroupsActivity.this, NewGroupActivity.class);
                startActivityForResult(intent, RC_REGULAR_FLOW);
            }
        });

    }

    private void startFirebaseSignInActivity(){
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(providers)
                        .setTheme(R.style.GreenTheme)
                        .setLogo(R.drawable.logo_login)
                        .build(),
                RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                CustomToast.customToast(this, R.string.logged_successfully, Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                onSignedOutCleanup();
                finish();
            }
        }
        if (requestCode == RC_REGULAR_FLOW) {
            if(resultCode == RESULT_SIGN_OUT) {
                //if the user return to this activity because had signed out
                //  start the sign in Firebase activity for result
                startFirebaseSignInActivity();

            }
        }
    }

    private void onSignedOutCleanup() {
        mGroupAdapter.clear();
        detachDatabaseReadListener();
    }

    private void onSignedInInitialize() {
        if(mFirebaseAuth.getUid()!=null){
            mUserDatabaseReference = mFireBaseDatabase.getReference().child("users").child(mFirebaseAuth.getUid());
            attachDatabaseReadListener();
        }
    }

    private void detachDatabaseReadListener() {
        if (mChildGroupsListener != null) {
            mGroupsDatabaseReference.removeEventListener(mChildGroupsListener);
            mChildGroupsListener = null;
        }

        if (mUserEventListener != null) {
            mUserDatabaseReference.removeEventListener(mUserEventListener);
            mUserEventListener = null;
        }
    }

    private void attachDatabaseReadListener() {
        if (mChildGroupsListener == null) {
            //read the db to check if there is any group
            mGroupsDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Object object = snapshot.getValue();
                    if (object == null) {
                        binding.pbLoading.setVisibility(View.GONE);
                        binding.tvEmptyGroups.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            //setup the child listener to add, update and remove the groups from the recycler view
            mChildGroupsListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    Group group = snapshot.getValue(Group.class);
                    if (group != null) {
                        mGroups.add(group);
                        mGroupAdapter.notifyDataSetChanged();
                    }

                    binding.pbLoading.setVisibility(View.INVISIBLE);
                    binding.tvEmptyGroups.setVisibility(View.GONE);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    binding.pbLoading.setVisibility(View.INVISIBLE);
                    //update the group in the recycler view
                    Group group = snapshot.getValue(Group.class);
                    if (group != null) {
                        int position = mGroupAdapter.getPositionGroup(group.getId());
                        if (position != -1) {
                            mGroups.set(position, group);
                            mGroupAdapter.notifyDataSetChanged();
                            binding.tvEmptyGroups.setVisibility(View.INVISIBLE);
                        }
                    }
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    //remove the deleted group from the recycler view
                    Group group = snapshot.getValue(Group.class);
                    if (group != null) {
                        int position = mGroupAdapter.getPositionGroup(group.getId());
                        if (position != -1) {
                            mGroups.remove(position);
                            mGroupAdapter.notifyDataSetChanged();
                        }
                    }
                    //if the recycler view is empty, show the empty view
                    if (mGroups.size() == 0) {
                        binding.tvEmptyGroups.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {


                }
            };
            mGroupsDatabaseReference.addChildEventListener(mChildGroupsListener);
        }

        //this value event listener check if the user is a new user and create a profile in the life time db
        mUserDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    mUser = user;
                } else {
                    User userNew = new User();
                    userNew.setId(Objects.requireNonNull(mFirebaseAuth.getUid()));
                    userNew.setName(Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getDisplayName());
                    mUser = userNew;
                    mUserDatabaseReference.getRef().setValue(mUser, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if(error!=null){
                                AlertDialog.Builder builder = new AlertDialog.Builder(GroupsActivity.this, R.style.AlertDialogStyle);
                                builder.setMessage(R.string.error_occurred)
                                        .setPositiveButton(R.string.ok,
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                    finish();
                                                    }
                                                }).show();
                            }   else{
                                CustomToast.customToast(GroupsActivity.this, R.string.user_created, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //add the listener for the user profile in the db
        if (mUserEventListener == null) {
            mUserEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
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

    public void singOut(Context context) {
        AuthUI.getInstance().signOut(context);
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.pbLoading.setVisibility(VISIBLE);
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        mConnectedRef.addValueEventListener(mConnectedListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mConnectedListener != null) {
            mConnectedRef.removeEventListener(mConnectedListener);
        }

        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        mGroupAdapter.clear();
    }

    //if a group of the recycler view is clicked
    @Override
    public void onClick(Group groupClicked, View view) {
        String groupId = groupClicked.getId();
        String myId = mFirebaseAuth.getUid().toString();

        //first check if the users is in the users list of the group
        DatabaseReference mUsersGroupDatabaseReference = mFireBaseDatabase.getReference().child("usersGroup")
                .child(groupId).child(myId);

        mUsersGroupDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //if is in the list, can start the group activity and
                if (snapshot.exists()) {
                    Intent intent = new Intent(GroupsActivity.this, GroupActivity.class);
                    intent.putExtra("ID", groupClicked.getId());
                    intent.putExtra("NAME", groupClicked.getName());
                    startActivityForResult(intent, RC_REGULAR_FLOW);
                } else {
                    //if is not in the list, we send a request for access to group admin
                    CustomToast.customToast(GroupsActivity.this, R.string.request_sended, Toast.LENGTH_SHORT).show();
                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put("/userGroups/" + mUser.getId() + "/requests/" + groupId, true);
                    childUpdates.put("/requests/" + groupId + "/" + mUser.getId(), mUser);

                    mFireBaseDatabase.getReference().updateChildren(childUpdates);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            case R.id.profile_menu:
                //start the activity where we can edit user's profile
                if (mIsConnected) {
                    Intent intent = new Intent(this, UserProfileActivity.class);
                    startActivity(intent);
                } else {
                    CustomToast.customToast(GroupsActivity.this, R.string.option_not_available_without_connection,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}