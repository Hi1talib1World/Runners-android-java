package com.denzo.runners;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.room.Room;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private Button btleft, btright, btmiddle;
    private TextView texttime, textlength, textpace, textcalories;

    private Handler mHandler;
    private static final int UPDATE_TEXTVIEW = 0;
    private static int count = 0;
    private static double s = 0, latitude = 0, longitude = 0;
    private static int calories = 0;

    private static final String DB_NAME = "running_db.sqlite";
    private RunningDAO runningdao;

    public static final String PREFS_NAME = "prefs";
    public static final String PREF_DARK_THEME = "dark_theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check and apply dark theme if necessary
        boolean useDarkTheme = isDarkThemeEnabled();
        if (useDarkTheme) {
            setTheme(R.style.AppThemeDark);  // Set the dark theme if true
        }

        setContentView(R.layout.activity_main);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize UI components
        btleft = findViewById(R.id.button_left);
        btright = findViewById(R.id.button_right);
        btmiddle = findViewById(R.id.button_middle);
        texttime = findViewById(R.id.data_time);
        textlength = findViewById(R.id.data_length);
        textpace = findViewById(R.id.data_pace);
        textcalories = findViewById(R.id.data_calories);

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize FusedLocationProviderClient for location services
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Database setup
        final File dbFile = this.getDatabasePath(DB_NAME);
        if (!dbFile.exists()) {
            try {
                copyDatabaseFile(dbFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Query data from database
        queryDataFromDatabase();

        // Handler for updating TextViews
        mHandler = new Handler(msg -> {
            if (msg.what == UPDATE_TEXTVIEW) {
                updateTextView();
            }
            return true;
        });
    }

    private boolean isDarkThemeEnabled() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return preferences.getBoolean(PREF_DARK_THEME, false);
    }

    private void copyDatabaseFile(String destinationPath) throws IOException {
        InputStream assetsDB = this.getAssets().open(DB_NAME);
        OutputStream dbOut = new FileOutputStream(destinationPath);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = assetsDB.read(buffer)) > 0) {
            dbOut.write(buffer, 0, length);
        }
        dbOut.flush();
        dbOut.close();
    }

    private void queryDataFromDatabase() {
        AppDatabase database = Room.databaseBuilder(this, AppDatabase.class, DB_NAME).allowMainThreadQueries().build();
        runningdao = database.getRunningdataDAO();
        List<Runningdata> runningdataList = runningdao.getAllRuningdata();
        // Log or use the retrieved data
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);  // Inflate the menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                Toast.makeText(this, "Search clicked", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_notifications:
                Toast.makeText(this, "Notifications clicked", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    private void updateTextView() {
        texttime.setText("Time: " + count + "s");
        textlength.setText("Length: " + s + "km");
        textpace.setText("Pace: " + (s / count) + " km/min");  // Example pace calculation
        textcalories.setText("Calories: " + calories);
    }

    // Request for location updates
    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(300)
                .setMinUpdateIntervalMillis(100)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    // Use the location data
                }
            }
        }, getMainLooper());
    }
}
