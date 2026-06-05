package com.denzo.runners.features.home;

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
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.denzo.runners.R;
import com.denzo.runners.features.activities.HistoryActivity;
import com.denzo.runners.core.database.AppDatabase;
import com.denzo.runners.data.local.dao.RunningDAO;
import com.denzo.runners.data.local.entities.Runningdata;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    public static final String PREFS_NAME = "RunnersPrefs";
    public static final String PREF_DARK_THEME = "dark_theme";

    private MapView map;
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
        // Load osmdroid configuration for offline caching
        Context ctx = getApplicationContext();
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

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
        
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(17.0);

        btleft.setOnClickListener(v -> {
            if (!isRunning) {
                startRunning();
            } else {
                stopRunning();
            }
        });
        btright.setVisibility(View.GONE);
        btnShare.setOnClickListener(v -> shareCurrentRun());
        
        // Handle history button from XML onClick
        findViewById(R.id.history_open).setOnClickListener(v -> onClickStartNewActivity(v));
    }

    public void onClickStartNewActivity(View v) {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
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
                    
                    if (lat != null && lng != null && map != null) {
                        count++;
                        updateFriendMarker(ds.getKey(), name, new GeoPoint(lat, lng));
                    }
                }
                textLiveFriends.setText(count + " Friends Running 🏃");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateFriendMarker(String id, String name, GeoPoint pos) {
        if (friendMarkers.containsKey(id)) {
            friendMarkers.get(id).setPosition(pos);
        } else {
            Marker m = new Marker(map);
            m.setPosition(pos);
            m.setTitle(name);
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(m);
            friendMarkers.put(id, m);
        }
        map.invalidate();
    }

    private void startRunning() {
        isRunning = true;
        btleft.setText("STOP");
        Toast.makeText(this, "Live Sync Enabled", Toast.LENGTH_SHORT).show();
        // Location updates started via requestLocationUpdates()
    }

    private void stopRunning() {
        isRunning = false;
        btleft.setText("START");
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
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location loc = locationResult.getLastLocation();
                if (loc == null) return;

                GeoPoint currentPos = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                map.getController().animateTo(currentPos);

                if (!isRunning || currentUser == null) return;
                
                // Sync live location to Firebase
                Map<String, Object> liveData = new HashMap<>();
                liveData.put("lat", loc.getLatitude());
                liveData.put("lng", loc.getLongitude());
                liveData.put("name", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Friend");
                mDatabase.child("live_runs").child(currentUser.getUid()).setValue(liveData);
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.getMainLooper());
        }
    }

    private void addLeaderboardItem(int rank, String name, double dist) {
        TextView tv = new TextView(this);
        tv.setText(rank + ". " + name + " - " + String.format(Locale.getDefault(), "%.1f km", dist));
        tv.setPadding(0, 8, 0, 8);
        leaderboardContainer.addView(tv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    @Override public void onSensorChanged(SensorEvent event) {}
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
