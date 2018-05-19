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
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
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
    Point StartPoint = Point.fromLngLat(17.057688277787634, 51.10949237944624);
    Point EndPoint = Point.fromLngLat(17.058318177817682, 51.10712000847647);
    DialogWindows dlg = new DialogWindows(context, this);
    ProgressBar progressBar;

    private MapView mapView;
    private NavigationMapRoute navigationMapRoute;
    private DirectionsRoute currentRoute;

    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;

    private Marker endPointMarker;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Mapbox.getInstance(this, getResources().getString(R.string.access_token));
        mapView = findViewById(R.id.mapView);
        send = findViewById(R.id.send);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> {
            map = mapboxMap;
            send.setOnClickListener(v -> {
                if (StartPoint != null && EndPoint != null) {
                    getRoute(StartPoint, EndPoint);
                } else {
                    message("Wybierz punkt startowy i docelowy", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO);
                }
            });
        });

        starting = findViewById(R.id.start);
        destination = findViewById(R.id.destination);

        starting.setText("");
        destination.setText("");
        starting.setOnClickListener(v -> dlg.dialogFirst(0));
        destination.setOnClickListener(v -> dlg.dialogFirst(1));

        progressBar = findViewById(R.id.progressBar);
    }

    @SuppressWarnings({"MissingPermission"})
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

    @SuppressWarnings({"MissingPermission"})
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
                new LatLng(location.getLatitude(), location.getLongitude()), 16));
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
    @SuppressWarnings({"MissingPermission"})
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
    @SuppressWarnings({"MissingPermission"})
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

    public void enableLocation() {
        this.enableLocationPlugin();
    }

    @SuppressWarnings({"MissingPermission"})
    public void disableLocation() {
        locationPlugin.setLocationLayerEnabled(false);
        locationEngine.removeLocationUpdates();
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
        StartPoint = Point.fromLngLat(Double.parseDouble(start2), Double.parseDouble(start1));
    }

    public void setEndPoint(String end1, String end2) {
        EndPoint = Point.fromLngLat(Double.parseDouble(end2), Double.parseDouble(end1));
    }

    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        send.setEnabled(false);
        starting.setEnabled(false);
        destination.setEnabled(false);
    }

    public void hideLoading() {
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
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, retrofit2.Response<DirectionsResponse> response) {
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            message("Nie udało się wyliczyć drogi.", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR);
                            return;
                        } else {
                            if (Objects.requireNonNull(response.body()).routes().size() < 1) {
                                message("Nie udało się wyliczyć drogi.", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR);
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
                        setCameraPosition(createNewLocation(origin));
                        if (endPointMarker != null) {
                            map.removeMarker(endPointMarker);
                        }
                        endPointMarker = map.addMarker(new MarkerOptions().position(new LatLng(destination.latitude(), destination.longitude())));
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                        message("Błąd polączenia z serwerem", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR);
                    }
                });
    }

    public void message(String message, int duration, int type) {
        MDToast mdToast;
        mdToast = MDToast.makeText(context, message, duration, type);
        mdToast.setGravity(Gravity.BOTTOM, 0, 400);
        mdToast.show();
    }

    Location createNewLocation(Point point) {
        Location location = new Location("dummyprovider");
        location.setLongitude(point.longitude());
        location.setLatitude(point.latitude());
        return location;
    }

}
