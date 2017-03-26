package com.pwr.routing;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapzen.android.graphics.MapFragment;
import com.mapzen.android.graphics.MapzenMap;
import com.mapzen.android.graphics.OnMapReadyCallback;
import com.mapzen.android.graphics.model.Marker;
import com.mapzen.android.graphics.model.Polyline;
import com.mapzen.android.routing.MapzenRouter;
import com.mapzen.helpers.RouteEngine;
import com.mapzen.helpers.RouteListener;
import com.mapzen.model.ValhallaLocation;
import com.mapzen.tangram.LngLat;
import com.mapzen.valhalla.Route;
import com.mapzen.valhalla.RouteCallback;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
    boolean myLokalization = false;
    boolean tasks = false;
    DialogWindows dlg = new DialogWindows(context,this);

    private GoogleApiClient client;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapzenMap map) {
                map.setPosition(new LngLat(17.059278, 51.108942));
                map.setZoom(18f);
                final Timer timer = new Timer();
                final Handler handler = new Handler();

                send.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (StartPoint[0] != null || EndPoint[0] != null) {
                            Log.i("mLastUpdateTime: ", mLastUpdateTime);
                            startLocationUpdates();
                            final MapzenRouter router = new MapzenRouter(MainActivity.this);
                            router.setWalking();
                            router.setCallback(new RouteCallback() {
                                @Override
                                public void success(final Route route) {
                                    Log.i("Route", route.getStartCoordinates().getLatitude() + "");

                                    map.removeMarker();
                                    map.removePolyline();
                                    map.setPosition(new LngLat(Double.parseDouble(StartPoint[1]), Double.parseDouble(StartPoint[0])));
                                    map.addMarker(new Marker(Double.parseDouble(EndPoint[1]), Double.parseDouble(EndPoint[0])));
                                    map.setRotation(0f);
                                    map.setZoom(18f);
                                    map.setTilt(0f);
                                    List<LngLat> coordinates = new ArrayList<>();
                                    for (ValhallaLocation location : route.getGeometry()) {
                                        coordinates.add(new LngLat(location.getLongitude(), location.getLatitude()));
                                    }
                                    Polyline polyline = new Polyline(coordinates);
                                    map.addPolyline(polyline);
                                    map.drawRouteLocationMarker(new LngLat(Double.parseDouble(StartPoint[1]), Double.parseDouble(StartPoint[0])));

                                    final RouteEngine engine = new RouteEngine();
                                    RouteListener routeListener = new RouteListener() {

                                        @Override
                                        public void onRouteStart() {

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

                                            router.fetch();
                                            Log.i("RECALCULATE", "Recalculate");
                                        }

                                        @Override
                                        public void onSnapLocation(ValhallaLocation originalLocation,
                                                                   ValhallaLocation snapLocation) {
                                            //Center map on snapLocation
                                            Log.i("RECALCULATE", "Recalculate1");
                                        }

                                        @Override
                                        public void onMilestoneReached(int index, RouteEngine.Milestone milestone) {
                                            Log.i("RECALCULATE", "Recalculate2");
                                        }

                                        @Override
                                        public void onApproachInstruction(int index) {
                                            //String instruction  = route.getRouteInstructions().get(index).getVerbalPreTransitionInstruction();
                                            //Speak pre transition instruction
                                            Log.i("RECALCULATE", "Recalculate3");
                                        }

                                        @Override
                                        public void onInstructionComplete(int index) {
                                            //String instruction  = route.getRouteInstructions().get(index).getVerbalPostTransitionInstruction();
                                            //Speak post transition instruction
                                            Log.i("RECALCULATE", "Recalculate4");
                                        }

                                        @Override
                                        public void onUpdateDistance(int distanceToNextInstruction, int distanceToDestination) {
                                            //Update trip summary UI to reflect distance away from destination
                                            Log.i("RECALCULATE", "Recalculate5");
                                        }

                                        @Override
                                        public void onRouteComplete() {
                                            //Show 'you have arrived' UI
                                            Log.i("RECALCULATE", "Recalculate6");
                                        }
                                    };


                                    engine.setListener(routeListener);
                                    engine.setRoute(route);


                                    TimerTask timerTask = new TimerTask() {
                                        public void run() {

                                            //use a handler to run a toast that shows the current timestamp
                                            handler.post(new Runnable() {
                                                public void run() {
                                                    if (myLokalization == true) {
                                                        Log.d("Handlers", "Called on main thread");
                                                        ValhallaLocation valhallaLocation = new ValhallaLocation();
                                                        valhallaLocation.setBearing(mCurrentLocation.getBearing());
                                                        valhallaLocation.setLatitude(mCurrentLocation.getLatitude());
                                                        valhallaLocation.setLongitude(mCurrentLocation.getLongitude());
                                                        engine.onLocationChanged(valhallaLocation);
                                                        map.clearRouteLocationMarker();
                                                        map.drawRouteLocationMarker(new LngLat(mCurrentLocation.getLongitude(), mCurrentLocation.getLatitude()));

                                                    }
                                                }
                                            });
                                        }
                                    };
                                    if (!tasks) {
                                        timer.schedule(timerTask, 5000, 10000);
                                    }

                                }

                                @Override
                                public void failure(int i) {
                                    Log.e("Eror", i + "");
                                }
                            });

                            Log.i("Loc", String.valueOf(myLokalization));

                            double[] start = {Double.parseDouble(StartPoint[0]), Double.parseDouble(StartPoint[1])};
                            router.setLocation(start);
                            double[] end = {Double.parseDouble(EndPoint[0]), Double.parseDouble(EndPoint[1])};
                            router.setLocation(end);

                            Log.i("START POINT ", StartPoint[0] + StartPoint[1]);
                            Log.i("END POINT ", EndPoint[0] + EndPoint[1]);
                            router.fetch();
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

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    public void myLokalizaction() {
        StartPoint[0] = String.valueOf(mCurrentLocation.getLatitude());
        StartPoint[1] = String.valueOf(mCurrentLocation.getLongitude());
        starting.setText("Moja Lokalizacja");
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
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
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
        Log.i("YeS:", mLastUpdateTime.toString());
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

    public void setStarting(String text){
        starting.setText(text);
    }
    public String getStarting(){
        return starting.getText().toString();
    }
    public void setDestination(String text){
        destination.setText(text);
    }
    public String getDestination(){
        return destination.getText().toString();
    }
    public void setStartPoint(String start1, String start2){
        StartPoint[0] = start1;
        StartPoint[1] = start2;
    }
    public void setEndPoint(String end1, String end2){
        EndPoint[0] = end1;
        EndPoint[1] = end2;
    }
    public void setlokalization(Boolean bool){
        myLokalization = bool;
    }
}
