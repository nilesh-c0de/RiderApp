package com.example.riderapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.riderapp.Model.Rider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    FirebaseAuth auth;
    FirebaseDatabase database;
    DatabaseReference users;

    LinearLayout linearLayout_login, linearLayout_register;
    BottomSheetBehavior bottomSheetBehaviorLogin, bottomSheetBehaviorRegister;

    Button btnSignup, btnLogin;
    EditText editEmail, editPassword;
    private String savedEmail, savedPassword;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sharedPreferences = getSharedPreferences("userData", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        //get the bottom sheet view
        linearLayout_login = findViewById(R.id.bottom_sheet_login);
        linearLayout_register = findViewById(R.id.bottom_sheet_register);
        //init the bottom sheet view
        bottomSheetBehaviorLogin = BottomSheetBehavior.from(linearLayout_login);
        bottomSheetBehaviorRegister = BottomSheetBehavior.from(linearLayout_register);


        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        users = database.getReference("riderData");

        btnSignup = (Button) findViewById(R.id.btn_signup);
        btnLogin = (Button) findViewById(R.id.btn_login);

        editEmail = (EditText) findViewById(R.id.input_email);
        editPassword = (EditText) findViewById(R.id.input_password);

        final EditText rEmail = (EditText) findViewById(R.id.edit_email);
        final EditText rName = (EditText) findViewById(R.id.edit_name);
        final EditText rPass = (EditText) findViewById(R.id.edit_pass);
        final EditText rCpass = (EditText) findViewById(R.id.edit_cpass);
        final EditText rMb = (EditText) findViewById(R.id.edit_mb);

        loadUserData();
        if (savedEmail != null && savedPassword != null) {
            startActivity(new Intent(getApplicationContext(), MapActivity.class));
        }

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //showRegisterDialog();
                bottomSheetBehaviorRegister.setState(BottomSheetBehavior.STATE_EXPANDED);

            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //showLoginDialog();
                bottomSheetBehaviorLogin.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        Button loginButton = (Button) findViewById(R.id.button_login);
        Button registerButton = (Button) findViewById(R.id.button_register);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String name = rName.getText().toString();
                final String email = rEmail.getText().toString();
                final String pass = rPass.getText().toString();
                final String cpass = rCpass.getText().toString();
                final String mb = rMb.getText().toString();

                if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || cpass.isEmpty() || mb.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please fill all details", Toast.LENGTH_SHORT).show();
                } else {
                    auth.createUserWithEmailAndPassword(email, pass)
                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {

                                        Log.d(TAG, "createUserWithEmail:success");
                                        users.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                final Rider usr = new Rider(email, pass, cpass, name, mb, "");
                                                users.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(usr);
                                                Toast.makeText(getApplicationContext(), "Registered successfully!", Toast.LENGTH_SHORT).show();
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                Toast.makeText(getApplicationContext(), "Failed!", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                        Toast.makeText(getApplicationContext(), "Error!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String textEmail = editEmail.getText().toString();
                final String textPassword = editPassword.getText().toString();

                if (textEmail.isEmpty() || textPassword.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please enter email & password!", Toast.LENGTH_SHORT).show();
                } else {
                    auth.signInWithEmailAndPassword(textEmail, textPassword)
                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {

                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "signInWithEmail:success");
                                        Toast.makeText(getApplicationContext(), "Authencation success :)", Toast.LENGTH_SHORT).show();

                                        // save session
                                        saveLoginData(textEmail, textPassword);

                                        // go to map activity
                                        startActivity(new Intent(getApplicationContext(), MapActivity.class));

                                    } else {
                                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                                        Toast.makeText(getApplicationContext(), "Authencation failed!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }

            }
        });

    }

    private void loadUserData() {
        savedEmail = sharedPreferences.getString("email", null);
        savedPassword = sharedPreferences.getString("pass", null);
    }

    private void saveLoginData(String textEmail, String textPassword) {


        editor.putString("email", textEmail);
        editor.putString("pass", textPassword);
        editor.commit();
    }
}
