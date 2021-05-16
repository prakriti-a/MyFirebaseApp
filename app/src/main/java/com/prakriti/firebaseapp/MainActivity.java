package com.prakriti.firebaseapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.INotificationSideChannel;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText edtEmail, edtUsername, edtPassword;
    private Button btn_signUp, btn_signIn;

    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // FirebaseApp.initializeApp(this);

        edtEmail = findViewById(R.id.edtEmail);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);

        btn_signUp = findViewById(R.id.btn_signUp);
        btn_signIn = findViewById(R.id.btn_signIn);

        btn_signUp.setOnClickListener(this);
        btn_signIn.setOnClickListener(this);

        firebaseAuth = FirebaseAuth.getInstance(); // initialise to sign up/sign in the user
        firebaseDatabase = FirebaseDatabase.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if(currentUser != null) {
            // transition to next activity
            transitionToSocialMediaActivity();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_signUp:
                signUpNewUser();
                break;
            case R.id.btn_signIn:
                signInExistingUser();
                break;
        }
    }

    private void signUpNewUser() {
        // validate user with email & password to sign up
        if(isFieldNull(edtEmail) || isFieldNull(edtUsername) || isFieldNull(edtPassword)) {
            return;
        }
        else {
            String signUpEmail = edtEmail.getText().toString().trim();
            String signUpPassword = edtPassword.getText().toString().trim();
            String signUpUsername = edtUsername.getText().toString().trim();

            firebaseAuth.createUserWithEmailAndPassword(signUpEmail, signUpPassword)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, R.string.signup_success, Toast.LENGTH_SHORT).show();
                                // store username to db
                                // getReference() gives access to the root of our db
                                firebaseDatabase.getReference().child("my_users")
                                        .child(task.getResult().getUser().getUid())
                                        .child("username").setValue(signUpUsername);
                                // create child under root, and create multiple children of that child, each of which will hold resp username
                                // use each users unique ID to add to db

                                // update the user's profile
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(signUpUsername)
                                        //.setPhotoUri()
                                        .build();
                                firebaseAuth.getCurrentUser().updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        Toast.makeText(MainActivity.this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                                    }
                                });

                                transitionToSocialMediaActivity();
                            }
                            else {
                                Toast.makeText(MainActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void signInExistingUser() {
        if(isFieldNull(edtEmail) || isFieldNull(edtPassword)) {
            return;
        }
        else {
            String signInEmail = edtEmail.getText().toString().trim();
            String signInPassword = edtPassword.getText().toString().trim();

            firebaseAuth.signInWithEmailAndPassword(signInEmail, signInPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, R.string.signin_success, Toast.LENGTH_SHORT).show();
                        transitionToSocialMediaActivity();
                    }
                    else {
                        Toast.makeText(MainActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private void transitionToSocialMediaActivity() {
        edtEmail.setText("");
        edtPassword.setText("");
        edtUsername.setText("");
        startActivity(new Intent(this, SocialMediaActivity.class));
        finish();
    }

    // check for empty fields submitted
    public static boolean isFieldNull(EditText field) {
        if (field.getText().toString().trim().equalsIgnoreCase("")) {
            field.setError("This field cannot be blank");
            field.requestFocus();
            return true;
        }
        return false;
        // equals() compares contents, == compares objects
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