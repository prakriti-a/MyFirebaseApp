package com.prakriti.firebaseapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class SocialMediaActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private FirebaseAuth firebaseAuth; // initialise always

    private ImageView imgPost;
    private ListView listview_users;
    private Bitmap bitmap;
    private EditText edtMessage;
    private LinearLayout ll_Users;

    private String imageIdentifier;

    private ArrayList<String> usernamesList;
    private ArrayAdapter arrayAdapter;

    private ArrayList<String> userUidList;
    private String imageDownloadLink;

    private static final int MEDIA_REQ_CODE = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_media);
        setTitle(R.string.home_page);

        firebaseAuth = FirebaseAuth.getInstance();

        TextView txtWelcome = findViewById(R.id.txtWelcome);
     //   txtWelcome.setText("Welcome " + firebaseAuth.getCurrentUser().getDisplayName()); // work on this

        imgPost = findViewById(R.id.imgPost);
        edtMessage = findViewById(R.id.edtMessage);
        Button btnShareImage = findViewById(R.id.btnShareImage);
        listview_users = findViewById(R.id.listview_users);
        ll_Users = findViewById(R.id.ll_Users);

        btnShareImage.setOnClickListener(this);
        imgPost.setOnClickListener(this);

        usernamesList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, usernamesList);
        listview_users.setAdapter(arrayAdapter);

        userUidList = new ArrayList<>();
        listview_users.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case  R.id.btnShareImage:
                uploadImageToFirebaseServer();
                break;

            case R.id.imgPost:
                selectImageFromUsersDevice();
                break;
        }
    }


    private void selectImageFromUsersDevice() {
        // check for permission to access device media
        // can also check for build version (<23) as permission protocol is different for >23
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, MEDIA_REQ_CODE);
        }
        else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, MEDIA_REQ_CODE); // for this override onActivityResult()
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == MEDIA_REQ_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectImageFromUsersDevice();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == MEDIA_REQ_CODE && resultCode == RESULT_OK && data != null) {
            // once image is selected
            Uri selectedImageUri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                imgPost.setImageBitmap(bitmap);
                imgPost.animate().alpha(1);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadImageToFirebaseServer() {
        // config storage
        if (bitmap != null) {
            // Get the data from an ImageView as bytes
            imgPost.setDrawingCacheEnabled(true);
            imgPost.buildDrawingCache();
//            Bitmap bitmap = ((BitmapDrawable) imgPost.getDrawable()).getBitmap(); // we already have a bitmap var
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // if you click the button before selecting the image, bitmap obj will be null & app will crash at this line ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            // generate unique name for image
            imageIdentifier = UUID.randomUUID() + ".png";

            // SLOW AF
            UploadTask uploadTask = FirebaseStorage.getInstance().getReference().child("my_images").child(imageIdentifier).putBytes(data);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    Toast.makeText(SocialMediaActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    Toast.makeText(SocialMediaActivity.this, R.string.img_upload_success, Toast.LENGTH_SHORT).show();
                    ll_Users.setVisibility(View.VISIBLE);
                    showListOfUsersOnScreen();

                    // get download link of image uploaded in background
                    taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if(task.isSuccessful()) {
                                imageDownloadLink = task.getResult().toString();
                            }
                        }
                    });
                }
            });
        }
        else {
            Toast.makeText(this, R.string.img_warning, Toast.LENGTH_SHORT).show();
        }
    }

    private void showListOfUsersOnScreen() {
        // access db to shows list of users on screen
        FirebaseDatabase.getInstance().getReference().child("my_users").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // add uniques keys to arraylist
                userUidList.add(snapshot.getKey()); // returns String of unique identifier inside "my_users"
                // when a child is added to my_users db
                String username = (String) snapshot.child("username").getValue();
                usernamesList.add(username);
                // to avoid current user's name showing up in list
//                if(username == firebaseAuth.getCurrentUser().getDisplayName()) {
//                    usernamesList.remove(username);
//                }
                arrayAdapter.notifyDataSetChanged();
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String message;
        if(edtMessage.getText().toString().trim().equalsIgnoreCase("")) {
            message = "";
        }
        else {
            message = edtMessage.getText().toString().trim();
        }
        // send data to tapped user -> specify data to be sent
        HashMap<String, String> dataMap = new HashMap<>();
        dataMap.put("fromUser", firebaseAuth.getCurrentUser().getDisplayName());
        dataMap.put("imageIdentifier", imageIdentifier); // image uuid
        dataMap.put("imageLink", imageDownloadLink); // access download link of image uploaded to server
        dataMap.put("message", message); // message to receiver

        FirebaseDatabase.getInstance().getReference().child("my_users").child(userUidList.get(position))
                // pass tapped user's id from position oon list as child
                .child("received_posts").push().setValue(dataMap)
        // push creates key automatically for the child
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            Toast.makeText(SocialMediaActivity.this, R.string.share_success, Toast.LENGTH_SHORT).show();
                            edtMessage.setText("");
                        }
                        else {
                            Toast.makeText(SocialMediaActivity.this, R.string.share_failure, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                firebaseAuth.signOut();
                // back to main activity
                startActivity(new Intent(this, MainActivity.class)); // ???
                finish();
                break;
            case R.id.item_viewPosts:
                startActivity(new Intent(this, ViewSharedPosts.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void hideKeyboard(View view) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            view.clearFocus();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}