package com.example.riderapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.riderapp.R.color.colorActionBar;

public class FeedbackActivity extends AppCompatActivity {

    TextView textView;
    IFCMService service;
    String driverUid;
    private String text;
    String pickup, dropoff;
    int amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        service = Global.getFCMService();
        textView = (TextView) findViewById(R.id.cash);

       // textView.setText(textView.getText().toString() + "");

        ImageView imageView = findViewById(R.id.driver_image);
        TextView textViewName = findViewById(R.id.name_of_driver);

        if(getIntent()!=null)
        {
            String url = getIntent().getStringExtra("url");
            String name = getIntent().getStringExtra("name");
            driverUid = getIntent().getStringExtra("token");
            amount = getIntent().getIntExtra("amt",0);

            textView.setText(textView.getText().toString()+amount);

            FirebaseDatabase.getInstance().getReference().child("Tokens")
                    .child(driverUid)
                    .child("token")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            text = dataSnapshot.getValue(String.class);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


            Picasso.with(getApplicationContext()).load(url).into(imageView);
            textViewName.setText(name);

        }

        Button submit = findViewById(R.id.button_submit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                if(text!=null) {

                    //download();
                    Token token = new Token(text);
                    Notification notification = new Notification("submit", ""+amount); // changed
                    Sender sender = new Sender(token.getToken(), notification);

                    service.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {

                            if (response.body().success == 1) {
                                Toast.makeText(getApplicationContext(), "Submitted!", Toast.LENGTH_SHORT).show();

                                Handler handler = new Handler();

                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {

                                        startActivity(new Intent(getApplicationContext(), MapActivity.class));
                                        finish();
                                    }
                                }, 3000);
                            }
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {

                        }
                    });

                }
            }
        });
    }

    private void download() {

        // ride details
        FirebaseDatabase.getInstance().getReference("Loc").child(FirebaseAuth.getInstance().getUid()).child("pickup")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        pickup = dataSnapshot.getValue(String.class);
                        Log.i("gg", pickup);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        FirebaseDatabase.getInstance().getReference("Loc").child(FirebaseAuth.getInstance().getUid()).child("dropoff")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        dropoff = dataSnapshot.getValue(String.class);
                        Log.i("gg", dropoff);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        Calendar calendar = Calendar.getInstance();
        String currentDate = DateFormat.getDateInstance(DateFormat.SHORT).format(calendar.getTime());
        Log.i("gg", currentDate);
        //currentDate = currentDate.replace(" ", "_");

        if(pickup != null && dropoff != null)
            FirebaseDatabase.getInstance().getReference("rides").child(FirebaseAuth.getInstance().getUid()).child(currentDate).setValue(new RideItem(currentDate, pickup, dropoff));
    }
}
