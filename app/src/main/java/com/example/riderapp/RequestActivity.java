package com.example.riderapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.riderapp.Common.Global;
import com.example.riderapp.Model.FCMResponse;
import com.example.riderapp.Model.Notification;
import com.example.riderapp.Model.RideItem;
import com.example.riderapp.Model.Sender;
import com.example.riderapp.Model.Token;
import com.example.riderapp.Remote.IFCMService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestActivity extends AppCompatActivity {

    ImageView imgClose;
    TextView textView, textCost;
    private BroadcastReceiver mGpsSwitchStateReceiver;
    private ProgressBar progressBar;
    ImageView tickMark;
    IFCMService service;

    String tokenExtra, riderTokenExtra, jsonExtra;
    String riderUid;

    String pickup, dropoff;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        riderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.i("lol", riderUid);

        if (getIntent() != null) {
            tokenExtra = getIntent().getStringExtra("token");
            riderTokenExtra = getIntent().getStringExtra("riderToken");
            jsonExtra = getIntent().getStringExtra("json_lat_lng");
        }

        service = Global.getFCMService();

        imgClose = (ImageView) findViewById(R.id.imgClose);
        tickMark = (ImageView) findViewById(R.id.correctMark);
        textView = (TextView) findViewById(R.id.txtReq);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textCost = (TextView) findViewById(R.id.txtCost);


        textCost.setText(textCost.getText().toString() + "40-120");


        mGpsSwitchStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String my_msg = intent.getStringExtra("msg");

                if (my_msg != null) {
                    if (my_msg.equals("Accepted")) {

                        tickMark.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.INVISIBLE);
                        textView.setText("A C C E P T E D");




                        Token token = new Token(tokenExtra);

                        Notification notification = new Notification("riderUid", riderUid);
                        Sender sender = new Sender(token.getToken(), notification);

                        service.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
                            @Override
                            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {

                                if (response.body().success == 1) {
                                    Toast.makeText(getApplicationContext(), "done.", Toast.LENGTH_SHORT).show();
                                    //finish();
                                }
                            }

                            @Override
                            public void onFailure(Call<FCMResponse> call, Throwable t) {

                            }
                        });


                    }
                    if (my_msg.equals("Rejected")) {
                        progressBar.setVisibility(View.INVISIBLE);
                        textView.setText("D E C L I N E D");
                        finish();
                    }
                }
            }
        };


        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getApplicationContext(), "Request cancelled!", Toast.LENGTH_SHORT).show();
                Token token = new Token(tokenExtra);
               // Toast.makeText(getApplicationContext(), "Ra:token:" + token.getToken(), Toast.LENGTH_SHORT).show();

                Notification notification = new Notification("rejectedNotice", jsonExtra);
                Sender sender = new Sender(token.getToken(), notification);

                service.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
                    @Override
                    public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {

                        if (response.body().success == 1) {
                            Toast.makeText(getApplicationContext(), "Request cancelled!", Toast.LENGTH_SHORT).show();
                            //finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<FCMResponse> call, Throwable t) {

                    }
                });
                finish();
            }
        });
    }




    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mGpsSwitchStateReceiver, new IntentFilter("MyMessage"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mGpsSwitchStateReceiver);
        finish();
    }
}
