package com.example.tasksgroups.ui.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.tasksgroups.R;
import com.example.tasksgroups.data.User;
import com.example.tasksgroups.databinding.ActivityUserProfileBinding;
import com.example.tasksgroups.ui.CustomToast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    //firebase references
    private FirebaseDatabase mFireBaseDatabase;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mUserPhotosStorageReference;
    private DatabaseReference mUserDatabaseReference;
    private ValueEventListener mUserEventListener;

    private DatabaseReference mUserRequestsReference;
    private DatabaseReference mUserGroupMemberReference;

    private DatabaseReference mConnectedRef;
    private ValueEventListener mConnectedListener;

    public static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER =  2;
    public static final int RESULT_SIGN_OUT = 100;

    String TAG = "tag";

    private boolean mIsConnected;

    private Map<String,Boolean> mUserRequests;
    private Map<String, Boolean> mUserGroupMember;

    private ValueEventListener mUserRequestsListener;
    private ValueEventListener mUserGroupMemberListener;

    private ActivityUserProfileBinding binding;

    private User mUser;
    private String mSavedName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_profile);

        setTitle(R.string.user_profile_activity_title);

        if(savedInstanceState!=null){
            if(savedInstanceState.containsKey("savedName")){
                mSavedName = savedInstanceState.getString("savedName");
            }
        }

        //get the references
        mFireBaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();

        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //user is signed in
                    onSignedInInitialize();
                }   else{
                    //user is signed out
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

        mUserPhotosStorageReference = mFirebaseStorage.getReference().child("userPhotos");

        //  if camera button clicked, start the image picker
        binding.ivAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                String text = getString(R.string.select_profile_image);
                startActivityForResult(Intent.createChooser(intent, text), RC_PHOTO_PICKER);
            }
        });

        // if the update button is clicked, update the profile of the user in the live time db
        binding.bUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newName = binding.etName.getText().toString().trim();
                if(!newName.equals("")){
                    mUser.setName(newName);
                    updateUser();
                }
            }
        });

        binding.bReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    //save the editText info between activity changes
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("savedName", binding.etName.getText().toString());
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


    private void onSignedOutCleanup() {
        detachDatabaseReadListener();
    }

    private void onSignedInInitialize() {
        if(mFirebaseAuth.getUid()!=null){
            Log.e("uid ","not null");
            mUserDatabaseReference = mFireBaseDatabase.getReference().child("users").child(mFirebaseAuth.getUid());
        }

        attachDatabaseReadListener();
    }

    void updateUser(){
        //update the profile of the user where needed in the live time db
        String userId = mUser.getId();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/users/"+userId,mUser);
        if(mUserGroupMember!=null){
            for(String group : mUserGroupMember.keySet()){
                childUpdates.put("/usersGroup/"+group+"/"+userId,mUser);
            }
        }
        if(mUserRequests!=null){
            for(String group : mUserRequests.keySet()){
                childUpdates.put("/requests/"+group+"/"+userId,mUser);
            }
        }
        mFireBaseDatabase.getReference().updateChildren(childUpdates, new DatabaseReference.CompletionListener() {
            //         mUserDatabaseReference.setValue(mUser, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if(error!=null){
                    Log.e("error saving",error.toString());
                    CustomToast.customToast(UserProfileActivity.this, R.string.error_occurred,Toast.LENGTH_SHORT).show();
                }   else{
                    CustomToast.customToast(UserProfileActivity.this,  R.string.updated_successfully,Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    private void detachDatabaseReadListener(){
        if(mUserEventListener!=null){
            mUserDatabaseReference.removeEventListener(mUserEventListener);
            mUserEventListener= null;
        }

        if(mUserRequestsListener!=null && mUserRequestsReference!=null) {
            mUserRequestsReference.removeEventListener(mUserRequestsListener);
            mUserRequestsListener = null;
        }

        if(mUserGroupMemberListener !=null && mUserGroupMemberReference!=null ){
            mUserGroupMemberReference.removeEventListener(mUserGroupMemberListener);
            mUserGroupMemberListener = null;
        }
    }

    private void attachDatabaseReadListener(){
        if(mUserEventListener==null){
            mUserEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if(user!=null){
                        mUser = user;
                        if(mSavedName!=null){
                            binding.etName.setText(mSavedName);
                        }   else{
                            binding.etName.setText(user.getName());
                        }

                        //check if the profile has a profile picture
                        // if not, display the default pic
                        if(user.getDrawable()!=null && !user.getDrawable().isEmpty()){
                            Glide.with(UserProfileActivity.this)
                                    .load(user.getDrawable())
                                    .circleCrop()
                                    .into(binding.ivUser);
                        }   else{
                            binding.ivUser.setBackground(
                                    UserProfileActivity.this.getResources().getDrawable(R.drawable.default_user_icon)
                            );
                        }
                        mUser = user;
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            };
            mUserDatabaseReference.addListenerForSingleValueEvent(mUserEventListener);
            mUserDatabaseReference.addValueEventListener(mUserEventListener);
        }
        String userId = mFirebaseAuth.getUid();
        if(mUserRequestsListener==null) {
            //retrieve in which groups the user has requests
            mUserRequestsReference = mFireBaseDatabase.getReference().child("userGroups").child(userId).child("requests");
            mUserRequestsListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    GenericTypeIndicator<Map<String, Boolean>> t = new GenericTypeIndicator<Map<String, Boolean>>() {
                    };
                    mUserRequests = snapshot.getValue(t);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };

            mUserRequestsReference.addValueEventListener(mUserRequestsListener);
        }


        if(mUserGroupMemberListener ==null){
            //retrieve in which groups the user is member
            mUserGroupMemberReference = mFireBaseDatabase.getReference().child("userGroups").child(userId).child("member");
            mUserGroupMemberListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    GenericTypeIndicator <Map<String, Boolean>> t = new  GenericTypeIndicator <Map<String, Boolean>>(){};
                    mUserGroupMember = snapshot.getValue(t);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            mUserGroupMemberReference.addValueEventListener(mUserGroupMemberListener);
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
        //check if return from select a photo
         if(requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){
            Uri selectedImageUri = data.getData();
            final StorageReference photoRef =
                    mUserPhotosStorageReference.child(mFirebaseAuth.getUid());

            //if photo reference is correct, update the user profile photo
            photoRef.putFile(selectedImageUri).addOnSuccessListener(this,
                    new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {

                                    Uri dlUri = uri;
                                    mUser.setDrawable(dlUri.toString());
                                    updateUser();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    CustomToast.customToast(UserProfileActivity.this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                 .addOnFailureListener(new OnFailureListener() {
                     @Override
                     public void onFailure(@NonNull Exception e) {
                         CustomToast.customToast(UserProfileActivity.this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                     }
                 });
        }
    }

}