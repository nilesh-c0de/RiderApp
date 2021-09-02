package com.example.riderapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.riderapp.Model.DataModel;
import com.example.riderapp.Model.RideItem;
import com.example.riderapp.Model.Rider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.util.Calendar;

public class RouteActivity extends AppCompatActivity implements OnMapReadyCallback {

    SupportMapFragment mapFragment;

    LinearLayout layout;
    BottomSheetBehavior bottomSheetBehavior;
    TextView messageTextView;
    BroadcastReceiver broadcastReceiver;
    ImageView driverImage;
    TextView driverName;
    String driverUid;
    private String url;
    private String fullName;
    private String pickup, dropoff;
    double lat, lng;

    GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        mapFragment = new SupportMapFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        mapFragment.getMapAsync(this);

        layout = findViewById(R.id.bottom_sheet_driver);
        bottomSheetBehavior = BottomSheetBehavior.from(layout);
        messageTextView = (TextView) findViewById(R.id.shortMessage);

        driverImage = (ImageView)findViewById(R.id.driver_profile);
        driverName = (TextView)findViewById(R.id.driver_name);


        if(getIntent()!=null)
        {
            driverUid = getIntent().getStringExtra("driverUid");
        }

        FirebaseDatabase.getInstance().getReference("online").child(driverUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                DataModel dataModel = dataSnapshot.getValue(DataModel.class);
                lat = dataModel.getLat();
                lng = dataModel.getLng();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        FirebaseDatabase.getInstance().getReference("driverData").child(driverUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Rider butDriver = dataSnapshot.getValue(Rider.class);

                url = butDriver.getImageUri();
                fullName = butDriver.getFullName();

                if(!url.isEmpty() && !fullName.isEmpty()) {
                    Picasso.with(getApplicationContext()).load(url).into(driverImage);
                    driverName.setText(fullName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("shortMessage")) {
                    String inputMessage = intent.getStringExtra("sm");

                    if (inputMessage != null) {
                        if (inputMessage.equals("arrived")) {
                            messageTextView.setText("driver is here - departs in 4 mins");
                        }
                        if (inputMessage.equals("trip")) {
                            messageTextView.setText("your trip starts in 2 min!");
                        }
                        if (inputMessage.equals("completed")) {
                            messageTextView.setText("drop-off at 4:00 pm");
                            Intent fbintent = new Intent(getBaseContext(), AmountActivity.class); // changed
                            fbintent.putExtra("name", fullName);
                            fbintent.putExtra("url", url);
                            fbintent.putExtra("token", driverUid);
                            startActivity(fbintent);
                        }
                    }
                }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("shortMessage"));

        final ImageView imageView = (ImageView) findViewById(R.id.arrow_image);

        final float rotation = imageView.getRotation();

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int state = bottomSheetBehavior.getState();

                if (state == BottomSheetBehavior.STATE_COLLAPSED)
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                if (state == BottomSheetBehavior.STATE_EXPANDED)
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            }
        });


        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int i) {
                switch (i) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        imageView.setRotation(180);
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        imageView.setRotation(rotation);
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        LatLng latLng = new LatLng(lat, lng);
        map.addMarker(new MarkerOptions().position(latLng).title("Driver is here"));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
    }
}
