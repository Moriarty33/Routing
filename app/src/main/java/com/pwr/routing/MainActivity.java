package com.pwr.routing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
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
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.valdesekamdem.library.mdtoast.MDToast;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import timber.log.Timber;


public class MainActivity extends AppCompatActivity implements LocationEngineListener, PermissionsListener {
    private static final String TAG = "APP";
    @BindView(R.id.start)
    AutoCompleteTextView starting;
    @BindView(R.id.destination)
    AutoCompleteTextView destination;
    @BindView(R.id.send)
    ImageView send;
    @BindView(R.id.cardview)
    CardView cardview;
    @BindView(R.id.startButton)
    FloatingActionButton navigationButton;
    @BindView(R.id.backButton)
    FloatingActionButton backButton;
    final MainActivity context = this;
//    Point StartPoint = Point.fromLngLat(17.057688277787634, 51.10949237944624);
//    Point EndPoint = Point.fromLngLat(17.058318177817682, 51.10712000847647);
    Point StartPoint;
    Point EndPoint;
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

    public enum ViewState {
        SELECT,
        NAVIGATION
    }

    private LocationEngineListener updateStartPointWithLocationEnabledListener = new LocationEngineListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnected() {
            setStartPoint(String.valueOf(locationEngine.getLastLocation().getLatitude()), String.valueOf(locationEngine.getLastLocation().getLongitude()));
        }

        @Override
        public void onLocationChanged(Location location) {
            setStartPoint(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
        }
    };

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
            setCameraPosition(createNewLocation(Point.fromLngLat(17.057688277787634, 51.10949237944624)));
            send.setOnClickListener(v -> {
                if (StartPoint != null && EndPoint != null) {
                    getRoute(StartPoint, EndPoint);
                    setViewVisibleState(ViewState.NAVIGATION);
                } else {
                    message(getString(R.string.NOT_SET_ANY_POINT), MDToast.LENGTH_SHORT, MDToast.TYPE_INFO);
                }
            });
        });

        starting = findViewById(R.id.start);
        destination = findViewById(R.id.destination);
        navigationButton = findViewById(R.id.startButton);
        navigationButton.setOnClickListener(v -> {
            Boolean shouldSimulateRoute = locationPlugin == null || !locationPlugin.isLocationLayerEnabled();
            NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                    .directionsRoute(currentRoute)
                    .shouldSimulateRoute(shouldSimulateRoute)
                    .directionsProfile(DirectionsCriteria.PROFILE_WALKING)
                    .build();

            // Call this method with Context from within an Activity
            NavigationLauncher.startNavigation(MainActivity.this, options);
        });


        starting.setText("");
        destination.setText("");
        starting.setOnClickListener(v -> dlg.dialogFirst(0));
        destination.setOnClickListener(v -> dlg.dialogFirst(1));


        progressBar = findViewById(R.id.progressBar);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> setViewVisibleState(ViewState.SELECT));

        try {
            addCerts();
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void setViewVisibleState(ViewState viewState) {
        if (viewState == ViewState.NAVIGATION) {
            hideViewElement(R.id.cardview);
            showViewElement(R.id.startButton);
            showViewElement(R.id.backButton);
        }

        if (viewState ==  ViewState.SELECT) {
            hideViewElement(R.id.startButton);
            hideViewElement(R.id.backButton);
            showViewElement(R.id.cardview);
        }

    }

    private void showViewElement(Integer target){
        YoYo.with(Techniques.SlideInUp)
                .duration(800)
                .playOn(findViewById(target));
        findViewById(target).setVisibility(View.VISIBLE);
    }

    private void hideViewElement(Integer target){
        YoYo.with(Techniques.SlideOutDown)
                .duration(800)
                .playOn(findViewById(target));
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Create an instance of LOST location engine
            initializeLocationEngine();

            locationPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
            locationPlugin.setLocationLayerEnabled(true);
            locationPlugin.setRenderMode(RenderMode.COMPASS);
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

    @SuppressWarnings({"MissingPermission"})
    public void enableLocation() {
        if (locationPlugin != null) {
            locationPlugin.setLocationLayerEnabled(true);
            locationEngine.requestLocationUpdates();
        } else {
            this.enableLocationPlugin();
        }

        if (locationEngine != null) {
            locationEngine.addLocationEngineListener(updateStartPointWithLocationEnabledListener);
        }

    }

    @SuppressWarnings({"MissingPermission"})
    public void disableLocation() {
        if (locationPlugin != null) {
            locationPlugin.setLocationLayerEnabled(false);
            locationEngine.removeLocationUpdates();
        }

        if (locationEngine != null) {
            locationEngine.removeLocationEngineListener(updateStartPointWithLocationEnabledListener);
        }
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
        if (start1 != null && start2 != null) {
            StartPoint = Point.fromLngLat(Double.parseDouble(start2), Double.parseDouble(start1));
        }
    }

    public void setEndPoint(String end1, String end2) {
        if (end1 != null && end2 != null) {
            EndPoint = Point.fromLngLat(Double.parseDouble(end2), Double.parseDouble(end1));
        }
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
        NavigationRoute.builder(context)
                .accessToken(getResources().getString(R.string.access_token))
                .origin(origin)
                .destination(destination)
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, retrofit2.Response<DirectionsResponse> response) {
                        Timber.tag(TAG).d("Response code: %s", response.code());
                        if (response.body() == null) {
                            Timber.tag(TAG).e("No routes found, make sure you set the right user and access token.");
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
                        navigationButton.setEnabled(true);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Timber.tag(TAG).e("Error: %s", throwable.getMessage());
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

    private void addCerts() throws KeyManagementException, NoSuchAlgorithmException {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

                    @SuppressLint("TrustAllX509TrustManager")
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {  }

                }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = (hostname, session) -> true;
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

    }

}