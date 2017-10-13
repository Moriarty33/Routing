package com.pwr.routing;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.mapzen.android.graphics.MapFragment;
import com.mapzen.android.graphics.MapzenMap;
import com.mapzen.android.graphics.OnMapReadyCallback;
import com.mapzen.android.routing.MapzenRouter;
import com.mapzen.helpers.RouteEngine;
import com.mapzen.helpers.RouteListener;
import com.mapzen.model.ValhallaLocation;
import com.mapzen.tangram.LngLat;
import com.mapzen.valhalla.Route;
import com.mapzen.valhalla.RouteCallback;
import com.valdesekamdem.library.mdtoast.MDToast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.InjectView;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    protected static final String TAG = "location-updates-sample";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    protected Location mCurrentLocation;
    protected Boolean mRequestingLocationUpdates = false;
    protected String mLastUpdateTime = "";
    LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    @InjectView(R.id.start)
    AutoCompleteTextView starting;
    @InjectView(R.id.destination)
    AutoCompleteTextView destination;
    @InjectView(R.id.send)
    ImageView send;
    final Context context = this;
    String[] StartPoint = new String[2];
    String[] EndPoint = new String[2];
    boolean myLocation = false;
    DialogWindows dlg = new DialogWindows(context, this);
    ProgressBar progressBar;
    final RouteEngine engine = new RouteEngine();

    private GoogleApiClient client;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapzenMap map) {
                map.setPosition(new LngLat(17.059278, 51.108942));
                map.setZoom(18f);

                send.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (StartPoint[0] != null && EndPoint[0] != null) {
                            map.setZoom(18f);
                            map.setRotation(0f);
                            map.setTilt(0f);
                            showLoading();
                            Log.i("mLastUpdateTime: ", mLastUpdateTime);
                            startLocationUpdates();
                            final MapzenRouter router = new MapzenRouter(MainActivity.this);
                            router.setWalking();
                            router.setCallback(new RouteCallback() {
                                @Override
                                public void success(final Route route) {
                                    hideLoading();

                                    RouteListener routeListener = new RouteListener() {
                                        @Override
                                        public void onRouteStart() {
                                            map.clearRouteLocationMarker();
                                            map.clearRouteLine();
                                            map.clearDroppedPins();

                                            List<LngLat> coordinates = new ArrayList<>();
                                            for (ValhallaLocation location : route.getGeometry()) {
                                                coordinates.add(new LngLat(location.getLongitude(), location.getLatitude()));
                                            }
                                            map.drawRouteLine(coordinates);
                                            map.setPosition(new LngLat(Double.parseDouble(StartPoint[1]), Double.parseDouble(StartPoint[0])));
                                            map.drawRouteLocationMarker(new LngLat(Double.parseDouble(StartPoint[1]), Double.parseDouble(StartPoint[0])));
                                            map.drawDroppedPin(new LngLat(Double.parseDouble(EndPoint[1]), Double.parseDouble(EndPoint[0])));

                                        }

                                        @Override
                                        public void onRecalculate(ValhallaLocation location) {
                                            router.clearLocations();
                                            StartPoint[0] = String.valueOf(location.getLatitude());
                                            StartPoint[1] = String.valueOf(location.getLongitude());
                                            double[] start = {Double.parseDouble(StartPoint[0]), Double.parseDouble(StartPoint[1])};
                                            router.setLocation(start);
                                            double[] end = {Double.parseDouble(EndPoint[0]), Double.parseDouble(EndPoint[1])};
                                            router.setLocation(end);
                                            Log.i("RECALCULATE", "Recalculate");
                                            router.fetch();
                                        }

                                        @Override
                                        public void onSnapLocation(ValhallaLocation originalLocation,
                                                                   ValhallaLocation snapLocation) {
                                            StartPoint[0] = String.valueOf(snapLocation.getLatitude());
                                            StartPoint[1] = String.valueOf(snapLocation.getLongitude());
                                            map.clearRouteLocationMarker();
                                            map.drawRouteLocationMarker(new LngLat(Double.parseDouble(StartPoint[1]), Double.parseDouble(StartPoint[0])));
                                            Log.i("RECALCULATE", "Recalculate1");
                                        }

                                        @Override
                                        public void onMilestoneReached(int index, RouteEngine.Milestone milestone) {}

                                        @Override
                                        public void onApproachInstruction(int index) {}

                                        @Override
                                        public void onInstructionComplete(int index) {}

                                        @Override
                                        public void onUpdateDistance(int distanceToNextInstruction, int distanceToDestination) {}

                                        @Override
                                        public void onRouteComplete() {}
                                    };
                                    engine.setListener(routeListener);
                                    engine.setRoute(route);
                                }

                                @Override
                                public void failure(int i) {
                                    hideLoading();
                                    MDToast mdToast = MDToast.makeText(context, "Nie udało się wyliczyć drogi.", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR);
                                    mdToast.setGravity(Gravity.BOTTOM,0,400);
                                    mdToast.show();
                                    Log.e("Eror", i + "");
                                }
                            });

                            double[] start = {Double.parseDouble(StartPoint[0]), Double.parseDouble(StartPoint[1])};
                            router.setLocation(start);
                            double[] end = {Double.parseDouble(EndPoint[0]), Double.parseDouble(EndPoint[1])};
                            router.setLocation(end);

                            Log.i("START POINT ", StartPoint[0] + StartPoint[1]);
                            Log.i("END POINT ", EndPoint[0] + EndPoint[1]);
                            router.fetch();
                        }else{
                            MDToast mdToast = MDToast.makeText(context, "Wybierz punkt startowy i docelowy", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO);
                            mdToast.setGravity(Gravity.BOTTOM,0,400);
                            mdToast.show();
                        }
                    }
                });
            }
        });
        starting = (AutoCompleteTextView) findViewById(R.id.start);
        destination = (AutoCompleteTextView) findViewById(R.id.destination);
        send = (ImageView) findViewById(R.id.send);
        starting.setText("");
        destination.setText("");
        starting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dialogFirst(0);
            }
        });
        destination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dialogFirst(1);
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);

            }

            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {

                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);

            }

        }
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }


    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        if(mGoogleApiClient.isConnected()){
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }

    }


    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        client.connect();
        mGoogleApiClient.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();

        super.onStop();
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        }
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        Log.i("Update Location:", mLastUpdateTime);

        ValhallaLocation valhallaLocation = new ValhallaLocation();
        valhallaLocation.setBearing(mCurrentLocation.getBearing());
        valhallaLocation.setLatitude(mCurrentLocation.getLatitude());
        valhallaLocation.setLongitude(mCurrentLocation.getLongitude());
        if(myLocation && engine.getRoute() != null){
            engine.onLocationChanged(valhallaLocation);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    public void setStarting(String text) {
        starting.setText(text);
    }

    public String getStarting() {
        return starting.getText().toString();
    }

    public void setDestination(String text) {
        destination.setText(text);
    }

    public String getDestination() {
        return destination.getText().toString();
    }

    public void setStartPoint(String start1, String start2) {
        StartPoint[0] = start1;
        StartPoint[1] = start2;
    }

    public void setEndPoint(String end1, String end2) {
        EndPoint[0] = end1;
        EndPoint[1] = end2;
    }

    public void setlokalization(Boolean bool) {
        myLocation = bool;
    }

    public void myLokalizaction() {
        if(isLocationEnabled(context)){
            final Timer timerLocation = new Timer();
            timerLocation.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startLocationUpdates();
                            if(mCurrentLocation != null) {
                                    StartPoint[0] = String.valueOf(mCurrentLocation.getLatitude());
                                    StartPoint[1] = String.valueOf(mCurrentLocation.getLongitude());
                                    setlokalization(true);
                                    starting.setText("Moja Lokalizacja");
                                    timerLocation.cancel();
                                    hideLoading();
                            } else {
                                showLoading();
                                starting.setText("Wyszukuje lokalizację..");
                            }
                        }
                    });
                }
            }, 0, 500);
        } else {showSettingDialog();}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 255 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            myLokalizaction();
        }else {
            MDToast mdToast = MDToast.makeText(context, "Aplikacja nie ma dostępu do lokalizacji", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR);
            mdToast.setGravity(Gravity.BOTTOM,0,400);
            mdToast.show();
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }

    private void showSettingDialog() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//Setting priotity of Location request to high
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);//5 sec Time interval for location update
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true); //this is the key ingredient to show dialog always when GPS is off

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, 225);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MDToast mdToast;
        switch (requestCode) {
            case 225:
                switch (resultCode) {
                    case RESULT_OK:
                        mdToast = MDToast.makeText(context, "Lokalizacja jest włączona", MDToast.LENGTH_SHORT, MDToast.TYPE_SUCCESS);
                        mdToast.setGravity(Gravity.BOTTOM,0,400);
                        mdToast.show();
                        myLokalizaction();
                        break;
                    case RESULT_CANCELED:
                        mdToast = MDToast.makeText(context, "Nie ma dostępu do lokalizacji", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR);
                        mdToast.setGravity(Gravity.BOTTOM,0,400);
                        mdToast.show();
                        break;
                }
                break;
        }
    }

    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        send.setEnabled(false);
        starting.setEnabled(false);
        destination.setEnabled(false);
    }

    public void hideLoading(){
        progressBar.setVisibility(View.INVISIBLE);
        send.setEnabled(true);
        starting.setEnabled(true);
        destination.setEnabled(true);
    }
}
