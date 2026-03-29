package com.denzo.runners;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.room.Room;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private Button btleft, btright;
    private ImageButton btnShare, btnSocial;
    private TextView texttime, textlength, textpace, textsteps, textLiveFriends;
    private LinearLayout leaderboardContainer;

    private boolean isRunning = false;
    private double totalDistanceKm = 0.0;
    private int secondsElapsed = 0;
    private Location lastLocation = null;
    
    // Firebase Social
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;
    private Map<String, Marker> friendMarkers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // UI Initialization
        btleft = findViewById(R.id.button_left);
        btright = findViewById(R.id.button_right);
        btnShare = findViewById(R.id.button_share);
        btnSocial = findViewById(R.id.button_social);
        texttime = findViewById(R.id.data_time);
        textlength = findViewById(R.id.data_length);
        textpace = findViewById(R.id.data_pace);
        textsteps = findViewById(R.id.data_steps);
        textLiveFriends = findViewById(R.id.text_live_friends);
        leaderboardContainer = findViewById(R.id.leaderboard_container);

        // Firebase Setup
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        setupSocialFeatures();
        setupLocationTracking();
        
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        btleft.setOnClickListener(v -> startRunning());
        btright.setOnClickListener(v -> stopRunning());
        btnShare.setOnClickListener(v -> shareCurrentRun());
    }

    private void setupSocialFeatures() {
        // 1. Friend Leaderboard (Real-time)
        mDatabase.child("leaderboard").orderByChild("distance").limitToLast(10)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    leaderboardContainer.removeAllViews();
                    int rank = 1;
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        String name = postSnapshot.child("name").getValue(String.class);
                        Double dist = postSnapshot.child("distance").getValue(Double.class);
                        if (name != null && dist != null) {
                            addLeaderboardItem(rank++, name, dist);
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });

        // 2. Live Friends Tracking
        mDatabase.child("live_runs").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (currentUser != null && ds.getKey().equals(currentUser.getUid())) continue;
                    
                    Double lat = ds.child("lat").getValue(Double.class);
                    Double lng = ds.child("lng").getValue(Double.class);
                    String name = ds.child("name").getValue(String.class);
                    
                    if (lat != null && lng != null && mMap != null) {
                        count++;
                        updateFriendMarker(ds.getKey(), name, new LatLng(lat, lng));
                    }
                }
                textLiveFriends.setText(count + " Friends Running 🏃");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateFriendMarker(String id, String name, LatLng pos) {
        if (friendMarkers.containsKey(id)) {
            friendMarkers.get(id).setPosition(pos);
        } else {
            Marker m = mMap.addMarker(new MarkerOptions().position(pos).title(name));
            friendMarkers.put(id, m);
        }
    }

    private void startRunning() {
        isRunning = true;
        Toast.makeText(this, "Live Sync Enabled", Toast.LENGTH_SHORT).show();
        // Location updates started via requestLocationUpdates()
    }

    private void stopRunning() {
        isRunning = false;
        if (currentUser != null) {
            mDatabase.child("live_runs").child(currentUser.getUid()).removeValue();
            // Update personal record on leaderboard
            Map<String, Object> update = new HashMap<>();
            update.put("name", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Runner");
            update.put("distance", totalDistanceKm);
            mDatabase.child("leaderboard").child(currentUser.getUid()).setValue(update);
        }
    }

    private void shareCurrentRun() {
        // Capture a screenshot of the stats card
        View view = findViewById(R.id.stats_card_main);
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            FileOutputStream stream = new FileOutputStream(cachePath + "/run_stats.png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            File imageFile = new File(cachePath, "run_stats.png");
            Uri contentUri = FileProvider.getUriForFile(this, "com.denzo.runners.fileprovider", imageFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out my run on Runners! " + totalDistanceKm + "km crushed! 🏃🔥");
            startActivity(Intent.createChooser(shareIntent, "Share Run"));

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupLocationTracking() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isRunning || currentUser == null) return;
                Location loc = locationResult.getLastLocation();
                if (loc != null) {
                    // Sync live location to Firebase
                    Map<String, Object> liveData = new HashMap<>();
                    liveData.put("lat", loc.getLatitude());
                    liveData.put("lng", loc.getLongitude());
                    liveData.put("name", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Friend");
                    mDatabase.child("live_runs").child(currentUser.getUid()).setValue(liveData);
                }
            }
        };
    }

    private void addLeaderboardItem(int rank, String name, double dist) {
        TextView tv = new TextView(this);
        tv.setText(rank + ". " + name + " - " + String.format(Locale.getDefault(), "%.1f km", dist));
        tv.setPadding(0, 8, 0, 8);
        leaderboardContainer.addView(tv);
    }

    @Override public void onMapReady(@NonNull GoogleMap googleMap) { mMap = googleMap; }
    @Override public void onSensorChanged(SensorEvent event) {}
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
