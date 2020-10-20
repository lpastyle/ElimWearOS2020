package com.example.gpsmemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * NB: this sample application was widely inspired from https://github.com/android/wear-os-samples/tree/master/SpeedTracker
 */
public class MainActivity extends FragmentActivity implements
        AmbientModeSupport.AmbientCallbackProvider,
        ActivityCompat.OnRequestPermissionsResultCallback {

    final static String LOG_TAG = MainActivity.class.getSimpleName();
    public static final long LOCATION_UPDATE_INTERVAL = 3000; // duration in milliseconds

    private static final String LOC_PROVIDER = "WATCH";
    private static final int REQUEST_GPS_PERMISSION_RESULT_CODE = 1;
    private static final int MAX_LOCATION_RECORDED = 10;

    private WearableRecyclerView.Adapter adapter;
    private ArrayList<Location> locations = new ArrayList<>();

    // Ambient mode support (aka Always-on)
    private AmbientModeSupport.AmbientController ambientController;
    // GPS
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate()");
        setContentView(R.layout.activity_main);

        /*
         * Enables Always-on, so our app doesn't shut down when the watch goes into ambient mode.
         * Best practice is to override onEnterAmbient(), onUpdateAmbient(), and onExitAmbient() to
         * optimize the display for ambient mode. However, for brevity, we aren't doing that here
         * to focus on learning location and permissions. For more information on best practices
         * in ambient mode, check this page:
         * https://developer.android.com/training/wearables/apps/always-on.html
         */
        // Enables Ambient mode.
        ambientController = AmbientModeSupport.attach(this);

        // Check that the watch has embedded GPS, exit application otherwise
        if (!hasGps()) {
            Log.w(LOG_TAG, "This hardware doesn't have GPS.");
            // should warn the user that location function is not available.
            finish();
        }
        /*
        // dummy init for test purpose only
        locations.add(makeLoc(42.12812921d, 6.22987212d));
        locations.add(makeLoc(43.24648328d, 7.53783836d));
        locations.add(makeLoc(44.39800122d, 8.40192765d));
        locations.add(makeLoc(45.83232627d, 9.12763238d));
        */
        WearableRecyclerView recyclerView = findViewById(R.id.recycler_launcher_view);
        if (recyclerView != null) {
            recyclerView.setEdgeItemsCenteringEnabled(true);
            recyclerView.setLayoutManager(new WearableLinearLayoutManager(this));

            adapter = new LocationListAdapter(locations, this);
            recyclerView.setAdapter(adapter);
        } else {
            Log.e(LOG_TAG, "recycler view not found");
        }

        // init location provider client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check and ask for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "Location permission already granted");
            startLocationUpdates();
        } else {
            Log.d(LOG_TAG, "ask for the location permission");
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_GPS_PERMISSION_RESULT_CODE);
        }
    }

    /* *********************************************************************************************
     * HELPER METHODS
     * *********************************************************************************************
     */
    private boolean hasGps() {
        Log.d(LOG_TAG, "hasGps()");
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    private void startLocationUpdates() {
        Log.d(LOG_TAG, "startLocationUpdates()");

        // create location request
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // start location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
        } else {
            Log.e(LOG_TAG, "ask for location permission should not happen");
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(LOG_TAG, "onRequestPermissionsResult(): " + Arrays.toString(permissions));

        if (requestCode == REQUEST_GPS_PERMISSION_RESULT_CODE) {
            Log.i(LOG_TAG, "Received response for GPS permission request");

            if ((grantResults.length == 1) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.i(LOG_TAG, "GPS permission granted.");
                startLocationUpdates();
            } else {
                Log.i(LOG_TAG, "GPS permission NOT granted.");
            }
        } else {
            Log.e(LOG_TAG, "Unhandled Request Permissions Result code");
        }
    }

    /* *********************************************************************************************
     * LOCATION Callbacks
     * *********************************************************************************************
     */
    // Warning this is not a method, but a member data declaration
    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Log.d(LOG_TAG, "onLocationResult()");
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                int length = locations.size();
                if (length >= MAX_LOCATION_RECORDED) {
                    locations.remove(length - 1); // remove last element
                }
                locations.add(0, location); // insert new element
                adapter.notifyDataSetChanged();
            }
        }
    };

    /* *********************************************************************************************
     * ACTIVITY LIFECYCLE Callbacks
     * *********************************************************************************************
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause()");
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume()");
        startLocationUpdates();
    }

    /* *********************************************************************************************
     * AMBIENT MODE Callbacks
     * *********************************************************************************************
     */
    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        /**
         * Prepares the UI for ambient mode.
         */
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            super.onEnterAmbient(ambientDetails);
            Log.d(LOG_TAG, "onEnterAmbient() " + ambientDetails);
        }

        /**
         * Restores the UI to active (non-ambient) mode.
         */
        @Override
        public void onExitAmbient() {
            super.onExitAmbient();
            Log.d(LOG_TAG, "onExitAmbient()");
        }
    }
}
