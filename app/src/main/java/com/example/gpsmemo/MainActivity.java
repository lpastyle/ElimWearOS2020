package com.example.gpsmemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * NB: this sample application was widely inspired by :
 * https://github.com/android/wear-os-samples/tree/master/SpeedTracker
 */
public class MainActivity extends WearableActivity implements
        OnSuccessListener<Location>,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    final static String LOG_TAG = MainActivity.class.getSimpleName();
    public static final long LOCATION_UPDATE_INTERVAL = 3000; // duration in milliseconds

    private static final String LOC_PROVIDER = "WATCH";
    private static final int REQUEST_GPS_PERMISSION_RESULT_CODE = 1;
    private static final int MAX_LOCATION_RECORDED = 10;

    private WearableRecyclerView.Adapter adapter;
    private ArrayList<Location> locations = new ArrayList<>();

    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate()");
        setContentView(R.layout.activity_main);

        // Enables Always-on
        setAmbientEnabled(); // inherited from WearableActivity

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "Location permission already granted");
            initLocationMonitoring();
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

    private void initLocationMonitoring() {
        Log.d(LOG_TAG, "initLocationMonitoring()");

        // get last known location
        FusedLocationProviderClient locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationProviderClient.getLastLocation().addOnSuccessListener(this, this);

        // init Google Maps Services
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    protected void createLocationRequest() {
        Log.d(LOG_TAG, "createLocationRequest()");

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.getStatus().isSuccess()) {
                            Log.d(LOG_TAG, "Successfully requested location updates");
                        } else {
                            Log.e(LOG_TAG, "Failed in requesting location updates " + status.getStatusMessage());
                        }
                    }
                });
    }

    private Location makeLoc(double lat, double lon) {
        Location l = new Location(LOC_PROVIDER);
        l.setLatitude(lat);
        l.setLongitude(lon);
        return l;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(LOG_TAG, "onRequestPermissionsResult(): " + Arrays.toString(permissions));

        if (requestCode == REQUEST_GPS_PERMISSION_RESULT_CODE) {
            Log.i(LOG_TAG, "Received response for GPS permission request");

            if ((grantResults.length == 1) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.i(LOG_TAG, "GPS permission granted.");
                initLocationMonitoring();
            } else {
                Log.i(LOG_TAG, "GPS permission NOT granted.");
            }
        } else {
            Log.e(LOG_TAG, "Unhandled Request Permissions Result code");
        }
    }

    /* *********************************************************************************************
     * FUSED LOCATION CALLBACK
     * *********************************************************************************************
     */
    @Override
    public void onLocationChanged(Location location) {
        Log.d(LOG_TAG, "onLocationChanged()");
        int length = locations.size();
        if (length >= MAX_LOCATION_RECORDED)
            locations.remove(length - 1); // remove last element
        locations.add(0, location); // insert new element
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSuccess(Location location) {
        Log.d(LOG_TAG, "onSuccess()");
        if (location != null) {
            locations.add(location);
        } else {
            // Use Polytech-Sophia as the default location !
            locations.add(makeLoc(43.615785, 7.071757));
        }
        adapter.notifyDataSetChanged();
    }
    /* *********************************************************************************************
     * GOOGLE MAPS SERVICES Callbacks
     * *********************************************************************************************
     */

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(LOG_TAG, "onConnected()");
        createLocationRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "onConnectionSuspended()");
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "onConnectionFailed(): " + connectionResult.getErrorMessage());
    }

    /* *********************************************************************************************
     * ACTIVITY LIFECYCLE Callbacks
     * *********************************************************************************************
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause()");
        if ((googleApiClient != null) && (googleApiClient.isConnected()) && (googleApiClient.isConnecting())) {
            googleApiClient.disconnect();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume()");
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }
}
