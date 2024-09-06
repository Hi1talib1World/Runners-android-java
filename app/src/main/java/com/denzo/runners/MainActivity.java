package com.denzo.runners;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationRequest.Builder;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private CameraPosition mCameraPosition;

    private Button btleft = null;
    private Button btright = null;
    private Button btmiddle = null;
    private TextView texttime = null;
    private TextView textlength = null;
    private TextView textpace = null;
    private TextView textcalories = null;

    private Handler mHandler = null;
    private static int count = 0;
    private static double s = 0;
    private static double d = 0;
    private static double sum = 0;
    private static double latitude = 0;
    private static double longitude = 0;
    private static double vdraw = 0;
    private static int v = 0;
    private static int pace = 0;
    private static int calories = 0;

    private boolean isPause = false;
    private boolean isStop = true;
    private static int delay = 1000; //1s
    private static int period = 1000; //1s
    private static final int UPDATE_TEXTVIEW = 0;

    private static double EARTH_RADIUS = 6378.137;//radius of earth

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    List<Double> latList = new ArrayList<Double>();
    List<Double> lonList = new ArrayList<Double>();

    private Location previousLocation = null;
    private ArrayList<Polyline> runningRoute = new ArrayList<Polyline>();
    private ArrayList<Location> points = new ArrayList<Location>();

    private boolean isDraw = false;
    private boolean showBound = false;

    String DB_NAME = "running_db.sqlite";
    RunningDAO runningdao;

    private String startTime = null;
    public static final String PREFS_NAME = "prefs";
    public static final String PREF_DARK_THEME = "dark_theme";

    private LatLng POI_1 = new LatLng(51.0264519, 13.7262368);
    private LatLng POI_2 = new LatLng(51.029106, 13.724735);
    private LatLng POI_3 = new LatLng(51.029029, 13.736457);

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean useDarkTheme = isDarkThemeEnabled();

        if (useDarkTheme) {
            setTheme(R.style.AppThemeDark);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Switch toggle = findViewById(R.id.switch1);
        toggle.setChecked(useDarkTheme);
        toggle.setOnCheckedChangeListener((view, isChecked) -> toggleTheme(isChecked));

        btleft = findViewById(R.id.button_left);
        btright = findViewById(R.id.button_right);
        btmiddle = findViewById(R.id.button_middle);
        texttime = findViewById(R.id.data_time);
        textlength = findViewById(R.id.data_length);
        textpace = findViewById(R.id.data_pace);
        textcalories = findViewById(R.id.data_calories);

        btleft.setOnClickListener(listener);
        btmiddle.setOnClickListener(listener);
        btright.setOnClickListener(listener);

        btleft.setEnabled(true);
        btright.setEnabled(false);
        btmiddle.setEnabled(false);

        final File dbFile = this.getDatabasePath(DB_NAME);
        if (!dbFile.exists()) {
            try {
                copyDatabaseFile(dbFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        queryDataFromDatabase();

        mHandler = new Handler(msg -> {
            if (msg.what == UPDATE_TEXTVIEW) {
                updateTextView();
            }
            return true;
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest mLocationRequest = new LocationRequest.Builder(300)
                .setMinUpdateIntervalMillis(100)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1340);
        } else {
            requestLocationUpdates();
        }
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

    public void queryDataFromDatabase() {
        AppDatabase database = Room.databaseBuilder(this, AppDatabase.class, DB_NAME).allowMainThreadQueries().build();
        runningdao = database.getRunningdataDAO();
        List<Runningdata> runningdata_list = runningdao.getAllRuningdata();
        for (int i = 0; i < runningdata_list.size(); i++) {
            int oldId = runningdata_list.get(i).getId();
            String oldStarttime = runningdata_list.get(i).getStarttime();
            double oldDistance = runningdata_list.get(i).getDistance();
            double oldCalorie = runningdata_list.get(i).getCalorie();
            System.out.println("Database shows here: "+"i:"+i+"oldId:"+oldId +"oldStarttime"+oldStarttime+"oldDistance"+oldDistance+"oldCalorie"+oldCalorie);
        }
    }

    public void AddDataRecordtoDB(View view) {
        TextView TV_distance = findViewById(R.id.data_length);
        Double runningDistance = Double.parseDouble(TV_distance.getText().toString());

        TextView TV_calories = findViewById(R.id.data_calories);
        Double runningCalories = Double.parseDouble(TV_calories.getText().toString());

        Runningdata NewRunningdata = new Runningdata();
        NewRunningdata.setDistance(runningDistance);
        NewRunningdata.setCalorie(runningCalories);
        NewRunningdata.setStarttime(startTime);

        runningdao.insert(NewRunningdata);
        System.out.println("I did insertion");
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    private void requestLocationUpdates() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        LocationServices.getSettingsClient(this)
                .checkLocationSettings(builder.build())
                .addOnSuccessListener(this, locationSettingsResponse -> {
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
                })
                .addOnFailureListener(this, e -> {
                    // Handle failure
                });
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                if (previousLocation != null) {
                    LatLng newLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    latList.add(location.getLatitude());
                    lonList.add(location.getLongitude());
                    drawRoute();
                }
                previousLocation = location;
            }
        }
    };

    private void drawRoute() {
        PolylineOptions polylineOptions = new PolylineOptions().color(R.color.colorAccent).width(5);
        for (int i = 0; i < latList.size(); i++) {
            polylineOptions.add(new LatLng(latList.get(i), lonList.get(i)));
        }
        mMap.addPolyline(polylineOptions);
    }

    private void updateTextView() {
        texttime.setText("Time: " + count + "s");
        textlength.setText("Length: " + s + "km");
        textpace.setText("Pace: " + pace + "min/km");
        textcalories.setText("Calories: " + calories);
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_left:
                    // Left button logic
                    break;
                case R.id.button_middle:
                    // Middle button logic
                    break;
                case R.id.button_right:
                    // Right button logic
                    break;
            }
        }
    };

    private void toggleTheme(boolean darkTheme) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(PREF_DARK_THEME, darkTheme);
        editor.apply();
        recreate();
    }
}
