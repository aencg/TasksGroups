package com.example.tasksgroups.ui.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.tasksgroups.R;
import com.example.tasksgroups.data.AddedFile;
import com.example.tasksgroups.databinding.ActivityFilesBinding;
import com.example.tasksgroups.ui.CustomToast;
import com.example.tasksgroups.ui.adapters.AddedFileAdapter;
import com.example.tasksgroups.utils.Utils;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.GridLayoutManager;

import static com.example.tasksgroups.utils.Utils.getRealPath;

public class FilesActivity extends AppCompatActivity implements AddedFileAdapter.AddedFileAdapterOnClickHandler, AddedFileAdapter.ClickMenuOptionHandler {


    public static final String TAG = "tag";
    public static final int RC_SIGN_IN = 1;
    public static final int RC_REGULAR_FLOW = 50;
    public static final int RESULT_SIGN_OUT = 100;
    public static final int RESULT_GROUP_DELETED = 200;
    private static final int REQUEST_CODE_DOC = 180;
    private static final int REQUEST_CODE = 1000;
    boolean mIsConnected;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    //real time db reference
    private FirebaseDatabase mFireBaseDatabase;
    private DatabaseReference mAddedFilesDatabaseReference;
    private ChildEventListener mChildAddedFilesDbEventListener;
    //storage references
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mAddedFilesStorageReference;
    private StorageReference mDownloadStorageReference;
    private StorageReference mStorageUploadRef;
    private OnCompleteListener<UploadTask.TaskSnapshot> mUploadCompleteListener;
    private UploadTask mUploadTask;
    //reference for conection
    private DatabaseReference mConnectedRef;
    private ValueEventListener mConnectedListener;
    private ActivityFilesBinding binding;
    private String mGroupId;
    private String mFileId;
    private Uri mUriSelectedFile;
    private AddedFileAdapter mAddedFilesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_files);

        //read the group id
        Intent intent = getIntent();
        if (intent.hasExtra("ID")) {
            mGroupId = intent.getStringExtra("ID");
        } else {
            finish();
        }

        setTitle(R.string.title_files_activity);
        List<AddedFile> mAddedFiles = new ArrayList<AddedFile>();

        mFireBaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();


        mConnectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        mConnectedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    boolean connected = snapshot.getValue(Boolean.class);
                    if (connected) {

                        Log.d(TAG, "connected");
                        binding.tvLostConnection.setVisibility(View.INVISIBLE);

                    } else {
                        //hide controls if disconected
                        Log.d(TAG, "not connected");
                        binding.tvLostConnection.setVisibility(View.VISIBLE);
                    }
                    mIsConnected = connected;
                } catch (Exception e) {

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Listener was cancelled");
            }
        };

        //uploaded files reference
        mAddedFilesDatabaseReference = mFireBaseDatabase.getReference().child("addedFiles").child(mGroupId);
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //user is signed in, attach the other listeners
                    onSignedInInitialize();
                } else {
                    Intent resultInt = new Intent();
                    resultInt.putExtra("Result", RESULT_SIGN_OUT);
                    setResult(RESULT_SIGN_OUT, resultInt);
                    finish();
                }
            }
        };


        mAddedFilesStorageReference = mFirebaseStorage.getReference().child("addedFiles").child(mGroupId);

        //setup the recycler for the files
        int spanCount = 2;
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            spanCount = 3;
        }
        mAddedFilesAdapter = new AddedFileAdapter(this, this, this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, spanCount);

        binding.recycler.setLayoutManager(gridLayoutManager);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(mAddedFilesAdapter);
        mAddedFilesAdapter.setAddedFilesData(mAddedFiles);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean uploading = false;
                if (mUriSelectedFile != null) {
                    uploading = true;
                }

                if (uploading) {
                    //if uploading show a toast and do nothing
                    CustomToast.customToast(FilesActivity.this, R.string.upload_pending, Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] mimeTypes =
                        {"application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .doc & .docx
                                "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .ppt & .pptx
                                "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xls & .xlsx
                                "text/plain",
                                "application/pdf",
                                "application/zip",
                                "image/jpeg",
                                "image/png"};

                //start file picker to select the file to upload
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

                } else {
                    StringBuilder builder = new StringBuilder();
                    for (String mimeType : mimeTypes) {
                        builder.append(mimeType);
                        builder.append("|");
                    }
                    String mimeTypesStr = builder.toString();
                    intent.setType(mimeTypesStr.substring(0, mimeTypesStr.length() - 1));
                }
                String text = getString(R.string.choose_file);
                startActivityForResult(Intent.createChooser(intent, text), REQUEST_CODE_DOC);
            }
        });

    }


    private void onSignedInInitialize() {
        attachDatabaseReadListener();
    }

    private void detachDatabaseReadListener() {
        //remove the real time listener of the firebase db
        if (mChildAddedFilesDbEventListener != null) {
            mAddedFilesDatabaseReference.removeEventListener(mChildAddedFilesDbEventListener);
            mChildAddedFilesDbEventListener = null;
        }
    }

    private void attachDatabaseReadListener() {

        mAddedFilesAdapter.clear();
        //add a single value event to read if the files db is empty
        mAddedFilesDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    // if is empty hide the progressbar and show the empty text view
                    binding.pbLoading.setVisibility(View.GONE);
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FilesActivity", "error " + error.toString());
            }
        });
        if (mChildAddedFilesDbEventListener == null) {
            //add a event listener to add, remove and modify in real time the files to the ui
            mChildAddedFilesDbEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    binding.pbLoading.setVisibility(View.INVISIBLE);
                    AddedFile file = snapshot.getValue(AddedFile.class);
                    if (file != null) {
                        //add the file to the adapter
                        mAddedFilesAdapter.addFile(file);
                        binding.tvEmpty.setVisibility(View.INVISIBLE);
                    }

                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    AddedFile addedFile = snapshot.getValue(AddedFile.class);
                    if (addedFile != null) {
                        int position = mAddedFilesAdapter.getPositionById(addedFile.getId());
                        if (position != -1) {
                            mAddedFilesAdapter.setFilePosition(position, addedFile);
                        }
                    }

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    AddedFile addedFile = snapshot.getValue(AddedFile.class);
                    if (addedFile != null) {
                        int position = mAddedFilesAdapter.getPositionById(addedFile.getId());
                        if (position != -1) {
                            mAddedFilesAdapter.removeFilePosition(position);
                        }
                    }
                    if (mAddedFilesAdapter.getItemCount() == 0) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            mAddedFilesDatabaseReference.addChildEventListener(mChildAddedFilesDbEventListener);
        }

        //read the shared preferences and launch a pending upload if necessary
        loadUploadTasksNew(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        //show the loading progress bar
        binding.pbLoading.setVisibility(View.VISIBLE);
        //add the listener to check if we are signed in
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        //add the listener to check if we are connected
        mConnectedRef.addValueEventListener(mConnectedListener);

        //check the permission to access the external storage
        if (Build.VERSION.SDK_INT >= 23) {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                //show the dialog for permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //check if the is a upload before we exit the activity
        if (mStorageUploadRef != null) {
            List<UploadTask> activeUploadTasks = mStorageUploadRef.getActiveUploadTasks();
            if (activeUploadTasks != null) {
                for (UploadTask task : activeUploadTasks) {
                    boolean isInProgress = task.isInProgress();
                    task.pause();
                    CustomToast.customToast(FilesActivity.this, R.string.upload_paused, Toast.LENGTH_SHORT).show();
                    if (mUploadCompleteListener != null) {
                        task.removeOnCompleteListener(mUploadCompleteListener);
                    }
                }
            }
        }
        //save the upload to retry later
        saveUploadTaskNew(this);

        //detach the listeners
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }

        if (mConnectedListener != null) {
            mConnectedRef.removeEventListener(mConnectedListener);
        }

        detachDatabaseReadListener();
    }

    //make a toast when single click on a file view
    @Override
    public void onOptionClick(AddedFile fileCliked, View view) {

        CustomToast.customToast(FilesActivity.this, R.string.long_click_for_options, Toast.LENGTH_SHORT).show();
    }

    private void onSignedOutCleanup() {
        mAddedFilesAdapter.clear();
        detachDatabaseReadListener();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);


        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                CustomToast.customToast(this, R.string.logged_successfull, Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                //if signed out return to the previous activity
                onSignedOutCleanup();
                Intent resultInt = new Intent();
                resultInt.putExtra("Result", RESULT_SIGN_OUT);
                setResult(RESULT_SIGN_OUT, resultInt);
                finish();
            }
        }
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.
        else if (requestCode == REQUEST_CODE_DOC && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            if (resultData != null) {
                Uri selectedFile = resultData.getData();

                File file = new File(Utils.getFileName(this, selectedFile));
                long fileSize = Utils.getFileSize(FilesActivity.this, selectedFile);

                //limit the file size to upload to aprox 5mb
                if (fileSize > 5000000L) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(FilesActivity.this, R.style.AlertDialogStyle);
                    builder.setMessage(R.string.selected_file_big)
                            .setPositiveButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {

                                        }
                                    });
                    // Create the AlertDialog object and return it
                    builder.create().show();

                } else {
                    String fileName = Utils.getFileName(this, selectedFile);
                    //check if a upload is working
                    boolean uploading = false;
                    if (mUriSelectedFile != null) {
                        uploading = true;
                    }

                    if (!uploading) {
                        int position = mAddedFilesAdapter.getPositionByName(fileName);
                        Log.e("filename ", "name: " + fileName + " position:" + position + " size:" + mAddedFilesAdapter.getItemCount());

                        //check if file already in cloud storage
                        if (position != -1) {

                            AlertDialog.Builder builder = new AlertDialog.Builder(FilesActivity.this, R.style.AlertDialogStyle);
                            builder.setMessage(R.string.same_name_file_overwrite)
                                    .setPositiveButton(R.string.yes,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    String idFile = mAddedFilesAdapter.getAddedFileByPosition(position).getId();

                                                    CustomToast.customToast(FilesActivity.this, R.string.uploading, Toast.LENGTH_SHORT).show();
                                                    mUploadTask = uploadFile(selectedFile, idFile);
                                                }
                                            })
                                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            CustomToast.customToast(FilesActivity.this, R.string.canceled, Toast.LENGTH_SHORT).show();

                                        }
                                    });
                            // Create the AlertDialog object and return it
                            builder.create().show();
                        } else {
                            //uploading a new file
                            CustomToast.customToast(this, R.string.uploading, Toast.LENGTH_SHORT).show();

                            mUploadTask = uploadFile(selectedFile, "");
                        }

                    } else {

                        //updload ongoing, show a dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(FilesActivity.this, R.style.AlertDialogStyle)
                                .setMessage(R.string.upload_pending)
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {

                                            }
                                        });
                        builder.create().show();
                    }

                }
            }
        }
    }

    //  helper function to create a upload task to upload a file to firebase storage
    private UploadTask uploadFile(Uri selectedFile, String idFile) {

        String fileName = Utils.getFileName(this, selectedFile);
        mStorageUploadRef = mAddedFilesStorageReference.child(fileName);
        mUploadCompleteListener = newCompleteListener(idFile);
        //we save the file id and the uri of the file to save them if the upload is paused later
        mFileId = idFile;
        mUriSelectedFile = selectedFile;

        UploadTask uploadTask = mStorageUploadRef.putFile(selectedFile);
        uploadTask.addOnCompleteListener(this, mUploadCompleteListener);

        return uploadTask;
    }

    //helper function to create a complete listener for the upload task
    private OnCompleteListener<UploadTask.TaskSnapshot> newCompleteListener(String idFile) {
        return new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                //check if upload successful
                if (task.isSuccessful()) {
                    //retrive the uri of the file in the firebase storage
                    task.getResult()
                            .getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            //create the entry in the firebase real time db
                            DatabaseReference ref;
                            AddedFile addedFile;
                            String url = uri.toString();
                            String fileName = Utils.getFileName(FilesActivity.this, uri);
                            if (idFile.equals("")) {
                                ref = mAddedFilesDatabaseReference.push();
                                addedFile = new AddedFile(ref.getKey(), fileName, url);
                            } else {
                                ref = mAddedFilesDatabaseReference.child(idFile);
                                addedFile = new AddedFile(idFile, fileName, url);
                            }


                            ref.setValue(addedFile, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                    if (error != null) {
                                        Log.e("error saving", error.toString());
                                        String errorOcurred = getString(R.string.error_occurred);
                                        CustomToast.customToast(FilesActivity.this, errorOcurred + " " + error.toString(), Toast.LENGTH_SHORT).show();
                                    } else {
                                        CustomToast.customToast(FilesActivity.this, R.string.file_added, Toast.LENGTH_SHORT).show();
                                    }

                                    mUriSelectedFile = null;
                                }
                            });
                        }
                    })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    CustomToast.customToast(FilesActivity.this, "Upload Failed", Toast.LENGTH_SHORT).show();
                                    mUriSelectedFile = null;
                                }
                            }).addOnCanceledListener(new OnCanceledListener() {
                        @Override
                        public void onCanceled() {
                            CustomToast.customToast(FilesActivity.this, "Upload Canceled", Toast.LENGTH_SHORT).show();
                            mUriSelectedFile = null;
                        }
                    });
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(FilesActivity.this, R.style.AlertDialogStyle);
                    builder.setMessage(R.string.problem_during_upload)
                            .setPositiveButton(R.string.ok,
                                    new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    });
                    builder.show();
                }

            }
        };
    }

    //check the option clicked in the menu that shows when a file item is long clicked
    @Override
    public void onOptionClick(AddedFile addedFileClicked, String option) {

        if (option.equals("delete")) {

            AlertDialog.Builder builder = new AlertDialog.Builder(FilesActivity.this, R.style.AlertDialogStyle);
            builder.setMessage(R.string.are_sure_delete_file)
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // Create a reference to the file to delete
                                    StorageReference desertRef = mAddedFilesStorageReference.child(addedFileClicked.getName());

                                    // Delete the file
                                    desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // File deleted successfully from cloud storage
                                            //  next delete from Firebase real time db
                                            mAddedFilesDatabaseReference.child(addedFileClicked.getId()).setValue(null, new DatabaseReference.CompletionListener() {
                                                @Override
                                                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                                    if (error != null) {
                                                        CustomToast.customToast(FilesActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        CustomToast.customToast(FilesActivity.this, R.string.file_deleted, Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            // an error occurred!
                                            CustomToast.customToast(FilesActivity.this, exception.toString(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            CustomToast.customToast(FilesActivity.this, R.string.canceled, Toast.LENGTH_SHORT).show();

                        }
                    });
            // Create the AlertDialog object and return it
            builder.create().show();


        } else if (option.equals("download")) {
            //first create a reference to the file in the device memory
            File rootPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = addedFileClicked.getName();
            final File file = new File(rootPath, fileName);

            if (file.exists()) {
                //if there is a file with same name, don't download the file
                CustomToast.customToast(FilesActivity.this, R.string.file_exists, Toast.LENGTH_SHORT).show();
            } else {
                //check the permissions to write in memory
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // if permissions not granted, can't download the file
                    CustomToast.customToast(FilesActivity.this, R.string.download_files_not_allowed, Toast.LENGTH_SHORT).show();

                } else {
                    //create the file
                    try {
                        boolean newFile = file.createNewFile();
                        Log.e("newFile", "created " + newFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("newFile", "created exception" + e.toString());
                    }

                    Log.e("ruta", mAddedFilesStorageReference.child(addedFileClicked.getName()).getPath());
                    Log.e("file", file.toString() + " can write: " + file.canWrite());

                    boolean downloading = false;
                    //check if a download is ongoing
                    if (mDownloadStorageReference != null) {
                        List<FileDownloadTask> activeDownloadTasks = mDownloadStorageReference.getActiveDownloadTasks();
                        if (activeDownloadTasks != null && activeDownloadTasks.size() > 0) {
                            for (FileDownloadTask task : activeDownloadTasks) {
                                if (task.isInProgress() || task.isPaused())
                                    downloading = true;
                            }
                        }
                    }
                    if (downloading) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(FilesActivity.this, R.style.AlertDialogStyle);
                        builder.setMessage(R.string.download_in_progress)
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                            }

                                        });
                        builder.show();
                    } else {
                        //start the download
                        mDownloadStorageReference = mAddedFilesStorageReference.child(addedFileClicked.getName());
                        mDownloadStorageReference.getFile(file).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                CustomToast.customToast(FilesActivity.this, R.string.file_downloaded, Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                CustomToast.customToast(FilesActivity.this, "File exception: " + exception.toString(), Toast.LENGTH_SHORT).show();
                                Log.e("download error", exception.toString());
                                Log.e("download error", exception.getCause().toString());
                            }
                        });
                    }
                }
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v("permission", "Permission: " + permissions[0] + "was " + grantResults[0]);
            //resume tasks needing this permission
        } else {
            CustomToast.customToast(FilesActivity.this, R.string.storage_permission_required, Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    //function to save in shared preferences an ongoing upload
    private void saveUploadTaskNew(Context context) {

        String pathString = "";
        if (mUriSelectedFile != null) {
            //Log.e("prueba uri.getpath",mUriSelectedFile.getPath());
            pathString = getRealPath(context, mUriSelectedFile);
        }

        //if pathString ="" we save that there isn't any upload ongoing
        // else we save the uri of the file

        // the idFile represent if the file is a new reference in the cloud storage or we are goint to overwrite the file
        SharedPreferences.Editor prefs = context.getSharedPreferences("FileActivity", 0).edit();
        prefs.putString("path" + mGroupId, pathString);
        prefs.putString("idFile" + mGroupId, mFileId);
        prefs.commit();

//        Log.e("saveUpload", "path" + mGroupId + ":   " + pathString);
//        Log.e("saveUpload", "idFile" + mGroupId + ": " + mFileId);
    }

    private void loadUploadTasksNew(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("FileActivity", 0);
        String path = prefs.getString("path" + mGroupId, "");
        String fileId = prefs.getString("idFile" + mGroupId, "");
        SharedPreferences.Editor editor = context.getSharedPreferences("FileActivity", 0).edit();
        editor.putString("path" + mGroupId, "");
        editor.commit();

        if (path.equals("")) {
            //there is not uploading saved
            return;
        }

//        Log.e("loadUpload", "path" + mGroupId + ":   " + path);
//        Log.e("loadUpload", "idFile" + mGroupId + ": " + fileId);

        //check if the file exists in memory
        Uri fileUri = null;
        File file = null;
        try {
            file = new File(java.net.URLDecoder.decode(path, StandardCharsets.UTF_8.toString()));
            if (!file.exists()) {
                Log.e("file no exists", file.toString());
                return;
            }
            fileUri = Uri.fromFile(file);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        if (fileUri != null) {
            //upload the file
            String fileName = file.getName();
            mStorageUploadRef =  mAddedFilesStorageReference.child(fileName);
            mUploadCompleteListener = newCompleteListener(fileId);
            mFileId = fileId;
            mUriSelectedFile = fileUri;
            CustomToast.customToast(FilesActivity.this, R.string.uploading, Toast.LENGTH_SHORT).show();

            mUploadTask = mStorageUploadRef.putFile(fileUri);
            mUploadTask.addOnCompleteListener(this, mUploadCompleteListener);
        }
    }

}