package com.pwr.routing;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.valdesekamdem.library.mdtoast.MDToast;

import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;


public class MainActivity extends AppCompatActivity implements LocationEngineListener, PermissionsListener {
    private static final String TAG = "APP";
    @BindView(R.id.start)
    AutoCompleteTextView starting;
    @BindView(R.id.destination)
    AutoCompleteTextView destination;
    @BindView(R.id.send)
    ImageView send;
    final Context context = this;
    Point StartPoint = Point.fromLngLat(51.1073569,17.0644340);
    Point EndPoint =  Point.fromLngLat( 51.1090784,17.0592878);
    DialogWindows dlg = new DialogWindows(context, this);
    ProgressBar progressBar;

    private MapView mapView;
    private NavigationMapRoute navigationMapRoute;
    private DirectionsRoute currentRoute;

    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        String MAPBOX_TOKEN = "pk.eyJ1IjoiYm9nZGFuMzMiLCJhIjoiY2pmc3J3NGRlMG5pODMzcW5hOWYxY3UwMSJ9.mwaBc5Ga8gPnJobIbd9mXw";
        Mapbox.getInstance(this, MAPBOX_TOKEN);
        mapView = findViewById(R.id.mapView);
        send = findViewById(R.id.send);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> {
            map = mapboxMap;
            enableLocationPlugin();
            send.setOnClickListener(v -> {
                Log.i("START POINT ", String.valueOf(StartPoint.longitude()));
                Log.i("END POINT ", String.valueOf(EndPoint.longitude()));
                getRoute(StartPoint, EndPoint);
            });
        });

        //MapboxNavigation navigation = new MapboxNavigation(this, MAPBOX_TOKEN);

        // MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
//        mapFragment.getMapAsync(new OnMapReadyCallback() {
//            @Override
//            public void onMapReady(final MapzenMap map) {
//                map.setPosition(new LngLat(17.059278, 51.108942));
//                map.setZoom(18f);
//
//                send.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        if (StartPoint[0] != null && EndPoint[0] != null) {
//                            map.setZoom(18f);
//                            map.setRotation(0f);
//                            map.setTilt(0f);
//                            showLoading();
//                            Log.i("mLastUpdateTime: ", mLastUpdateTime);
//                            startLocationUpdates();
//                            final MapzenRouter router = new MapzenRouter(MainActivity.this);
//                            router.setWalking();
//                            router.setCallback(new RouteCallback() {
//                                @Override
//                                public void success(final Route route) {
//                                    hideLoading();
//
//                                    RouteListener routeListener = new RouteListener() {
//                                        @Override
//                                        public void onRouteStart() {
//                                            map.clearRouteLocationMarker();
//                                            map.clearRouteLine();
//                                            map.clearDroppedPins();
//
//                                            List<LngLat> coordinates = new ArrayList<>();
//                                            for (ValhallaLocation location : route.getGeometry()) {
//                                                coordinates.add(new LngLat(location.getLongitude(), location.getLatitude()));
//                                            }
//                                            map.drawRouteLine(coordinates);
//                                            map.setPosition(new LngLat(Double.parseDouble(StartPoint[1]), Double.parseDouble(StartPoint[0])));
//                                            map.drawRouteLocationMarker(new LngLat(Double.parseDouble(StartPoint[1]), Double.parseDouble(StartPoint[0])));
//                                            map.drawDroppedPin(new LngLat(Double.parseDouble(EndPoint[1]), Double.parseDouble(EndPoint[0])));
//
//                                        }
//
//                                        @Override
//                                        public void onRecalculate(ValhallaLocation location) {
//                                            router.clearLocations();
//                                            StartPoint[0] = String.valueOf(location.getLatitude());
//                                            StartPoint[1] = String.valueOf(location.getLongitude());
//                                            double[] start = {Double.parseDouble(StartPoint[0]), Double.parseDouble(StartPoint[1])};
//                                            router.setLocation(start);
//                                            double[] end = {Double.parseDouble(EndPoint[0]), Double.parseDouble(EndPoint[1])};
//                                            router.setLocation(end);
//                                            Log.i("RECALCULATE", "Recalculate");
//                                            router.fetch();
//                                        }
//
//                                        @Override
//                                        public void onSnapLocation(ValhallaLocation originalLocation,
//                                                                   ValhallaLocation snapLocation) {
//                                            StartPoint[0] = String.valueOf(snapLocation.getLatitude());
//                                            StartPoint[1] = String.valueOf(snapLocation.getLongitude());
//                                            map.clearRouteLocationMarker();
//                                            map.drawRouteLocationMarker(new LngLat(Double.parseDouble(StartPoint[1]), Double.parseDouble(StartPoint[0])));
//                                            Log.i("RECALCULATE", "Recalculate1");
//                                        }
//
//                                        @Override
//                                        public void onMilestoneReached(int index, RouteEngine.Milestone milestone) {}
//
//                                        @Override
//                                        public void onApproachInstruction(int index) {}
//
//                                        @Override
//                                        public void onInstructionComplete(int index) {}
//
//                                        @Override
//                                        public void onUpdateDistance(int distanceToNextInstruction, int distanceToDestination) {}
//
//                                        @Override
//                                        public void onRouteComplete() {}
//                                    };
//                                    engine.setListener(routeListener);
//                                    engine.setRoute(route);
//                                }
//
//                                @Override
//                                public void failure(int i) {
//                                    hideLoading();
//                                    MDToast mdToast = MDToast.makeText(context, "Nie udało się wyliczyć drogi.", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR);
//                                    mdToast.setGravity(Gravity.BOTTOM,0,400);
//                                    mdToast.show();
//                                    Log.e("Eror", i + "");
//                                }
//                            });
//
//                            double[] start = {Double.parseDouble(StartPoint[0]), Double.parseDouble(StartPoint[1])};
//                            router.setLocation(start);
//                            double[] end = {Double.parseDouble(EndPoint[0]), Double.parseDouble(EndPoint[1])};
//                            router.setLocation(end);
//
//                            Log.i("START POINT ", StartPoint[0] + StartPoint[1]);
//                            Log.i("END POINT ", EndPoint[0] + EndPoint[1]);
//                            router.fetch();
//                        }else{
//                            MDToast mdToast = MDToast.makeText(context, "Wybierz punkt startowy i docelowy", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO);
//                            mdToast.setGravity(Gravity.BOTTOM,0,400);
//                            mdToast.show();
//                        }
//                    }
//                });
//            }
//        });
        starting = findViewById(R.id.start);
        destination = findViewById(R.id.destination);

        starting.setText("");
        destination.setText("");
        starting.setOnClickListener(v -> dlg.dialogFirst(0));
        destination.setOnClickListener(v -> dlg.dialogFirst(1));

        progressBar = findViewById(R.id.progressBar);
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Create an instance of LOST location engine
            initializeLocationEngine();

            locationPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
            locationPlugin.setLocationLayerEnabled(true);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    private void initializeLocationEngine() {
        locationEngine = GoogleLocationEngine.getLocationEngine(MainActivity.this);
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    private void setCameraPosition(Location location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 13));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            finish();
        }
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            setCameraPosition(location);
            locationEngine.removeLocationEngineListener(this);
        }
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    protected void onStart() {
        super.onStart();
        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
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
        StartPoint = Point.fromLngLat(Double.parseDouble(start1),Double.parseDouble(start2));
    }

    public void setEndPoint(String end1, String end2) {
        EndPoint = Point.fromLngLat(Double.parseDouble(end1),Double.parseDouble(end2));
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

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder()
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, retrofit2.Response<DirectionsResponse> response) {
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else {
                            if (Objects.requireNonNull(response.body()).routes().size() < 1) {
                                Log.e(TAG, "No routes found");
                                return;
                            }
                        }

                        currentRoute = Objects.requireNonNull(response.body()).routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, map, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }

}
