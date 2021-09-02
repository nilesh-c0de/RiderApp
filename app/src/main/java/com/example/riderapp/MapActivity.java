package com.example.riderapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.riderapp.Common.Global;
import com.example.riderapp.Model.DataModel;
import com.example.riderapp.Model.FCMResponse;
import com.example.riderapp.Model.Notification;
import com.example.riderapp.Model.RideItem;
import com.example.riderapp.Model.Rider;
import com.example.riderapp.Model.Sender;
import com.example.riderapp.Model.Token;
import com.example.riderapp.Remote.IFCMService;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener, ChildEventListener {

    // constants
    private static final int PERMISSION_CODE = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;
    private static final int GALLERY_PICK = 3;
    // drawer
    private DrawerLayout drawer;
    // fragments
    private AutocompleteSupportFragment pickupFragment, dropFragment;
    // google map
    GoogleMap map;
    private LatLng pickup_loc, drop_loc;
    private int radius = 10;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    //private SettingsClient client;
    private Location mCurrentLocation;
    private Marker srcMarker, destMarker;
    private Marker driverMarker;
    private Geocoder geocoder;
    private Boolean firstTime = false;
    // service
    IFCMService mService;
    // shared preferences
    //private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    // firebase references
    private StorageReference mStorageRef;
    //private FirebaseDatabase mDatabase;
    private DatabaseReference mRef, refToRiderData;
    // navigation header refereneces
    private Uri imageUri;
    private TextView textHeaderName, textHeaderEmail;
    private ImageView pImage;
    // saving key and marker
    private Map<String, Marker> data = new HashMap<>();
    private Map<String, Marker> markerData = new HashMap<>();

    private long backPressedTime;
    private Toast backToast;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // drawer
        drawer = findViewById(R.id.drawer_layout);

        // navigation view
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        // starting services
        mService = Global.getFCMService();

        // shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("userData", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // initializing fusedLocationProviderClient and gecoder
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        // getting firebase references
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference().child("onlineDriver");
        refToRiderData = mDatabase.getReference().child("riderData");
        mStorageRef = FirebaseStorage.getInstance().getReference();


        imageUri = null;
        // header reference
        View view = navigationView.getHeaderView(0);
        textHeaderName = (TextView) view.findViewById(R.id.header_username);
        textHeaderEmail = (TextView) view.findViewById(R.id.header_email);
        pImage = (ImageView) view.findViewById(R.id.profile_image);

        //global = new Global();

        // setting name, email and profile picture in navigation drawer
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("riderData").child(uid).child("fullName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.getValue(String.class);

                if(!name.isEmpty())
                    textHeaderName.setText(name);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        FirebaseDatabase.getInstance().getReference("riderData").child(uid).child("email").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String email = dataSnapshot.getValue(String.class);

                if(!email.isEmpty())
                    textHeaderEmail.setText(email);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        FirebaseDatabase.getInstance().getReference("riderData").child(uid).child("imageUri").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String url = dataSnapshot.getValue(String.class);
                //Log.i("url", url);

                if(url.isEmpty())
                {

                }
                else {
                    Picasso.with(getApplicationContext()).load(url).into(pImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // setting map fragment
        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        mapFragment.getMapAsync(this);

        // setting places api
        Places.initialize(getApplicationContext(),
                "AIzaSyD1MIEiIjEVpl-Abb4rDwQbPEoFMV9OIsw");
        PlacesClient placesClient = Places.createClient(this);

        pickupFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.pickup_location);
        dropFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.drop_location);

        pickupFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));
        dropFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));

        pickupFragment.setHint("Enter pickup location");
        dropFragment.setHint("Enter drop location");

        // choosing profile image
        pImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select an image"), GALLERY_PICK);
            }
        });


        Button btnFindDriver = (Button) findViewById(R.id.button_find_driver);
        btnFindDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (pickup_loc != null && drop_loc != null) {

                    requestPickup(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    //sendRequestToDriver(FirebaseAuth.getInstance().getCurrentUser().getUid());

                    String pickupAddress = getAddress(pickup_loc.latitude, pickup_loc.longitude);
                    String dropAddress = getAddress(drop_loc.latitude, drop_loc.longitude);

                    if(pickupAddress != null && dropAddress != null) {
                        FirebaseDatabase.getInstance().getReference("Loc").child(FirebaseAuth.getInstance().getUid()).child("pickup").setValue(pickupAddress);
                        FirebaseDatabase.getInstance().getReference("Loc").child(FirebaseAuth.getInstance().getUid()).child("dropoff").setValue(dropAddress);

                        Calendar calendar = Calendar.getInstance();
                        String currentDate = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.getTime());
                        Log.i("gg", currentDate);
                        //currentDate = currentDate.replace(" ", "_");

                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

                            FirebaseDatabase.getInstance().getReference("ridess").child(FirebaseAuth.getInstance().getUid()).child("Ride"+timeStamp).setValue(new RideItem(currentDate, pickupAddress, dropAddress));
                    }


                } else {
                    Toast.makeText(getApplicationContext(), "Please add destination by long press on map!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // setting pickup and drop-off fragment listener
        pickupFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng src = place.getLatLng();

                pickupFragment.setText(place.getAddress());

                if (srcMarker != null)
                    srcMarker.remove();

                srcMarker = map.addMarker(new MarkerOptions().position(src).title("Pickup me here!").draggable(true));
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });
        dropFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng dest = place.getLatLng();

                dropFragment.setText(place.getAddress());

                if (destMarker != null)
                    destMarker.remove();

                destMarker = map.addMarker(new MarkerOptions().title("Drop me here!").position(dest).draggable(true).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });

        // updating firebase token to "Token" database
        updateFirebaseToken();
    }

    private void updateFirebaseToken() {

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference("Tokens");

        Token token = new Token(FirebaseInstanceId.getInstance().getToken());


        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token);
        }
    }

    private void requestPickup(String uid) {

        DatabaseReference dbRequest = FirebaseDatabase.getInstance().getReference("riderPickupReqs");
        GeoFire gfRequests = new GeoFire(dbRequest);
        gfRequests.setLocation(uid, new GeoLocation(pickup_loc.latitude, pickup_loc.longitude));

        findDrivers();
    }

    private void findDrivers() {

        DatabaseReference onlineDriver = FirebaseDatabase.getInstance().getReference("onlineDriver");
        GeoFire gfOnlineDrivers = new GeoFire(onlineDriver);


        GeoQuery geoQuery = gfOnlineDrivers.queryAtLocation(new GeoLocation(pickup_loc.latitude, pickup_loc.longitude), radius);

        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                Toast.makeText(getApplicationContext(), "Driver FOUND: " + key, Toast.LENGTH_SHORT).show();
                sendRequestToDriver(key);

            }

            @Override
            public void onKeyExited(String key) {

                //Toast.makeText(getApplicationContext(), "Driver LOST: " + key, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                //Toast.makeText(getApplicationContext(), "Driver MOVED: " + key, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void sendRequestToDriver(String uid) {

        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");

        tokens.orderByKey().equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            final Token token = postSnapshot.getValue(Token.class);

                            //global.setMyToken(token.getToken());

                            //Toast.makeText(getApplicationContext(), "MAP:token:" + token.getToken(), Toast.LENGTH_SHORT).show();

                            final String json_lat_lng = new Gson().toJson(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                            final String riderToken = FirebaseInstanceId.getInstance().getToken();
                            Notification data = new Notification(riderToken, json_lat_lng);
                            Sender sender = new Sender(token.getToken(), data);
                            //Toast.makeText(getApplicationContext(), token.getToken(), Toast.LENGTH_SHORT).show();
                            mService.sendMessage(sender)
                                    .enqueue(new Callback<FCMResponse>() {
                                        @Override
                                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                                            if (response.body().success == 1) {
                                                Toast.makeText(MapActivity.this, "Request sent!", Toast.LENGTH_SHORT).show();

                                                Intent intent = new Intent(getApplicationContext(), RequestActivity.class);
                                                intent.putExtra("token", token.getToken());
                                                intent.putExtra("riderToken", riderToken);
                                                intent.putExtra("json", json_lat_lng);
                                                startActivity(intent);
                                            } else {
                                                Toast.makeText(MapActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<FCMResponse> call, Throwable t) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void startLocationUpdates() {
        createLocationRequest();
        createLocationCallback();
        buildLocationSettingsRequest();
    }

    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                updateMarkerUI();
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void updateMarkerUI() {
        if (mCurrentLocation != null) {

            LatLng mPosition = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            pickup_loc = mPosition;


            map.setMyLocationEnabled(true);

            if (!firstTime) {

                String pickupAddress = getAddress(mPosition.latitude, mPosition.longitude);
                if (pickupAddress != null) {
                    pickupFragment.setText(pickupAddress);
                }

                if (srcMarker != null)
                    srcMarker.remove();

                srcMarker = map.addMarker(new MarkerOptions().title("Pickup").position(mPosition).draggable(true));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(mPosition, 15f));
                firstTime = true;
            }

            //loadAllAvailableDrivers(mPosition);

        }
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @SuppressLint("MissingPermission")
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                map.setMyLocationEnabled(true);
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MapActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }

            }
        });
    }

    @SuppressLint({"MissingSuperCall", "MissingPermission"})
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        //Log.i(TAG, "User agreed to make required location settings changes.");
                        startLocationUpdates();
                        updateMarkerUI();
                        break;
                    case Activity.RESULT_CANCELED:
                        //Log.i(TAG, "User chose not to make required location settings changes.");
                        map.setMyLocationEnabled(false);
                        break;
                }

        }

        if (requestCode == GALLERY_PICK && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(getApplicationContext(), "Image not selected!", Toast.LENGTH_SHORT).show();
            } else {
                imageUri = data.getData();

                //Picasso.with(getApplicationContext()).load(imageUri).into(pImage);

                // Create an image file name
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//                String FileName = JPEG_FILE_PREFIX + timeStamp + "_";
//                File F = File.createTempFile(FileName, "IMG_");
//
//                Calendar calendar = Calendar.getInstance();
//                String currentDate = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.getTime());
//                currentDate = currentDate.replace(" ", "_");

                final StorageReference ref = mStorageRef.child("IMG_"+timeStamp+".jpg");
                ref.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(final Uri uri) {
                                Log.i("uri", uri.toString());

                                Picasso.with(getApplicationContext()).load(uri).into(pImage);
                                DatabaseReference refTo = refToRiderData.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                refTo.child("imageUri").setValue(uri.toString());
                            }
                        });
                    }
                });
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setTrafficEnabled(false);
        map.setIndoorEnabled(false);
        map.setBuildingsEnabled(false);


        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);


        if (checkLocationPermissions()) {
            //startGettingLocation();
            startLocationUpdates();
            updateMarkerUI();
        } else {
            requestLocationPermissions();
        }

        if (map != null)
            mRef.addChildEventListener(this);
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {
                String title = marker.getTitle();
                if (title.equals("Pickup")) {
                    pickupFragment.setText("Getting address...");
                }
                if (title.equals("DropOff")) {
                    dropFragment.setText("Getting address...");
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

                String title = marker.getTitle();
                LatLng position = marker.getPosition();

                if (title.equals("Pickup")) {
                    String address = getAddress(position.latitude, position.longitude);
                    pickup_loc = position;

                    if (address != null) {
                        pickupFragment.setText(address);
                    }
                }
                if (title.equals("DropOff")) {
                    String address = getAddress(position.latitude, position.longitude);
                    drop_loc = position;
                    if (address != null) {
                        dropFragment.setText(address);

                    }
                }
            }
        });

        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                drop_loc = latLng;
                String dropoffAddress = getAddress(drop_loc.latitude, drop_loc.longitude);
                if (dropoffAddress != null)
                    dropFragment.setText(dropoffAddress);

                if (destMarker != null)
                    destMarker.remove();
                destMarker = map.addMarker(new MarkerOptions().title("DropOff").position(latLng).draggable(true).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            }
        });


    }

    private boolean checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestLocationPermissions() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed for accessing location!")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MapActivity.this,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
        }
    }

    private void loadAllAvailableDrivers(LatLng mLocation) {

        final DatabaseReference driverLocations = FirebaseDatabase.getInstance().getReference("onlineDriver");
        GeoFire gf = new GeoFire(driverLocations);


        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(mLocation.latitude, mLocation.longitude), radius);
        geoQuery.removeAllListeners();


        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, final GeoLocation location) {
                //Toast.makeText(getApplicationContext(), "key: " + key, Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "online: "+key, Toast.LENGTH_SHORT).show();


                FirebaseDatabase.getInstance().getReference("driverData")
                        .child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                //getting rider details
                                Rider rider = dataSnapshot.getValue(Rider.class);
                                //add markers
                                //driverMarker = map.addMarker(new MarkerOptions().title(rider.getFullName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.car)).position(new LatLng(location.latitude, location.longitude)));

                                Marker marker = map.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude)).title(rider.getFullName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.caronmap)).rotation(45));

                                if (!markerData.containsKey(key))
                                    markerData.put(key, marker);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }

            @Override
            public void onKeyExited(String key) {

                Toast.makeText(getApplicationContext(), "offline: "+key, Toast.LENGTH_SHORT).show();
                //Toast.makeText(getApplicationContext(), "Driver offline gaya hehe :p", Toast.LENGTH_SHORT).show();
                //driverMarker.remove();


                Marker marker = markerData.get(key);
                if (marker != null)
                    marker.remove();

                markerData.remove(key);
            }

            @Override
            public void onKeyMoved(final String key, final GeoLocation location) {
                Toast.makeText(getApplicationContext(), "moved: "+key, Toast.LENGTH_SHORT).show();
//                FirebaseDatabase.getInstance().getReference("driverData")
//                        .child(key)
//                        .addListenerForSingleValueEvent(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//
//                                Rider rider = dataSnapshot.getValue(Rider.class);
//
//                                if(driverMarker!=null)
//                                    driverMarker.remove();
//                                map.addMarker(new MarkerOptions().title(rider.getFullName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.car)).position(new LatLng(location.latitude, location.longitude)));
//                            }
//
//                            @Override
//                            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                            }
//                        });
//                String key = dataSnapshot.getKey();
//
//                // new lat lng
//                DataModel dataModel = dataSnapshot.getValue(DataModel.class);
//                double lat = dataModel.getLat();
//                double lng = dataModel.getLng();

                // remove old marker
                Marker marker = markerData.get(key);
                float rotation = 45;

                if(marker!=null)
                {
                    rotation = marker.getRotation();
                }

                final float xd = rotation;


                if (marker != null) marker.remove();

                FirebaseDatabase.getInstance().getReference("driverData")
                        .child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                //getting rider details
                                Rider rider = dataSnapshot.getValue(Rider.class);
                                //add markers
                                //driverMarker = map.addMarker(new MarkerOptions().title(rider.getFullName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.car)).position(new LatLng(location.latitude, location.longitude)));

                                Marker marker = map.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude)).title(rider.getFullName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.caronmap)).rotation(xd + 45));

                                markerData.put(key, marker);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });




            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private String getAddress(double latitude, double longitude) {

        List<Address> addressList;

        try {

            addressList = geocoder.getFromLocation(latitude, longitude, 1);

            String address = addressList.get(0).getAddressLine(0);
            String city = addressList.get(0).getLocality();
            String state = addressList.get(0).getAdminArea();
            String country = addressList.get(0).getCountryName();
            String postalCode = addressList.get(0).getPostalCode();
            String knownName = addressList.get(0).getFeatureName();


            return knownName + ", " + city;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show();
                startLocationUpdates();
                updateMarkerUI();
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            case R.id.edit_profile:
                //Toast.makeText(getApplicationContext(), "Edit profile", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                break;
            case R.id.ride_details:
                //Toast.makeText(getApplicationContext(), "Ride details", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), RideHistoryActivity.class));
                break;
            case R.id.log_out:
                //Toast.makeText(getApplicationContext(), "Log out", Toast.LENGTH_SHORT).show();
                logoutUser();
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logoutUser() {
        editor.remove("email");
        editor.remove("pass");
        editor.commit();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //super.onBackPressed();
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                backToast.cancel();
                super.onBackPressed();
                return;
            } else {
                backToast = Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT);
                backToast.show();
            }
            backPressedTime = System.currentTimeMillis();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_types, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.normal_map:
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case R.id.satellite_map:
                map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.night_map:
                MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.night);
                map.setMapStyle(style);
                break;
        }
        return true;
    }

    @Override
    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        Log.i("NILESH: ", "child added");
        // key
        final String key = dataSnapshot.getKey();

        // new

        final double latitude = dataSnapshot.child("l").child("0").getValue(Double.class);
        final double longitude = dataSnapshot.child("l").child("1").getValue(Double.class);

        Log.i("Nilesh: lat: "+latitude+"long: "+longitude, "xd");

        if (key != null) {
            FirebaseDatabase.getInstance().getReference("driverData")
                    .child(key)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            //getting rider details
                            Rider rider = dataSnapshot.getValue(Rider.class);

                            if(rider!=null) {
                                //add markers
                                //driverMarker = map.addMarker(new MarkerOptions().title(rider.getFullName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.car)).position(new LatLng(location.latitude, location.longitude)));

                                Marker marker = map.addMarker(new MarkerOptions().title(rider.getFullName()).position(new LatLng(latitude, longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.caronmap)).rotation(90));
                                data.put(key, marker);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }


//        Marker marker = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.caronmap)).rotation(0));
//        data.put(key, marker);

        // lat lng
        //DataModel dataModel = dataSnapshot.getValue(DataModel.class);
        //double lat = dataModel.getLat();
        //double lng = dataModel.getLng();

        // marker
        //Marker marker = map.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(BitmapDescriptorFactory.fromResource(R.drawable.caronmap)).rotation(45));
        //data.put(key, marker);
    }

    @Override
    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
        //DataModel dataModel = dataSnapshot.getValue(DataModel.class);
        Log.i("NILESH: ", "child changed");
        //DataModel dataModel = dataSnapshot.getValue(DataModel.class);
        //Log.i("database: ", String.valueOf(dataSnapshot.getValue()));

        // key
        final String key = dataSnapshot.getKey();

//        // new lat lng
//        DataModel dataModel = dataSnapshot.getValue(DataModel.class);
//        double lat = dataModel.getLat();
//        double lng = dataModel.getLng();

        final double latitude = dataSnapshot.child("l").child("0").getValue(Double.class);
        final double longitude = dataSnapshot.child("l").child("1").getValue(Double.class);

        // remove old marker
        Marker marker = data.get(key);

        final float rotation = marker.getRotation() + 90;
       // float rotation = marker.getRotation();


        if (marker != null)
            marker.remove();

        if (key != null) {
            FirebaseDatabase.getInstance().getReference("driverData")
                    .child(key)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            //getting rider details
                            Rider rider = dataSnapshot.getValue(Rider.class);
                            //add markers
                            //driverMarker = map.addMarker(new MarkerOptions().title(rider.getFullName()).icon(BitmapDescriptorFactory.fromResource(R.drawable.car)).position(new LatLng(location.latitude, location.longitude)));

                            Marker marker = map.addMarker(new MarkerOptions().title(rider.getFullName()).position(new LatLng(latitude, longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.caronmap)).rotation(rotation));
                            data.put(key, marker);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }

//        // insert new marker
//        Marker newMarker = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.caronmap)).rotation(270));
//
//        data.put(key, newMarker);
    }

    @Override
    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
        //DataModel dataModel = dataSnapshot.getValue(DataModel.class);
        Log.i("NILESH: ", "child removed");

        String key = dataSnapshot.getKey();

        Marker marker = data.get(key);
        if (marker != null)
            marker.remove();

        data.remove(key);
    }

    @Override
    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }
}
