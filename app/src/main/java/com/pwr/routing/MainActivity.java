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

import java.lang.reflect.Array;
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
    protected Boolean mRequestingLocationUpdates;
    protected String mLastUpdateTime;

    MapzenMap map;
    private boolean enableLocationOnResume = false;
    LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    @InjectView(R.id.start)
    AutoCompleteTextView starting;
    @InjectView(R.id.destination)
    AutoCompleteTextView destination;
    @InjectView(R.id.send)
    ImageView send;
    Map<String,String> listBuildings = new HashMap<>();
    Map<String,String> listBuildingslessons = new HashMap<>();
    final Context context = this;
    String [] StartPoint;
    String [] EndPoint;
    boolean myLokalization = false;
    boolean tasks = false;

    private GoogleApiClient client;
    @Override
    protected void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";
        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();
        StartPoint = new String[2];
        EndPoint= new String[2];
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
                        if(StartPoint[0] != null || EndPoint[0] != null){
                        Log.i("mLastUpdateTime: ", mLastUpdateTime);

                        startLocationUpdates();


                        final MapzenRouter router = new MapzenRouter(MainActivity.this);
                        router.setWalking();
                        router.setCallback(new RouteCallback() {
                            @Override
                            public void success(final Route route) {
                                Log.i("Route", route.getStartCoordinates().getLatitude() + "");
                                //map.clearRouteLocationMarker();
                                map.removeMarker();
                                map.removePolyline();
                                map.setPosition(new LngLat(Double.parseDouble(StartPoint[1]),Double.parseDouble(StartPoint[0])));
                                map.addMarker(new Marker(Double.parseDouble(EndPoint[1]),Double.parseDouble(EndPoint[0])));
                                map.setRotation(0f);
                                map.setZoom(18f);
                                map.setTilt(0f);
                                List<LngLat> coordinates = new ArrayList<>();
                                for (ValhallaLocation location : route.getGeometry()) {
                                    coordinates.add(new LngLat(location.getLongitude(), location.getLatitude()));
                                }
                                Polyline polyline = new Polyline(coordinates);
                                map.addPolyline(polyline);
                                map.drawRouteLocationMarker(new LngLat(Double.parseDouble(StartPoint[1]),Double.parseDouble(StartPoint[0])));

                                final RouteEngine engine = new RouteEngine();
                                RouteListener routeListener = new RouteListener() {

                                    @Override public void onRouteStart() {

                                    }

                                    @Override public void onRecalculate(ValhallaLocation location) {
                                        router.clearLocations();
                                        StartPoint[0] = String.valueOf(location.getLatitude());
                                        StartPoint[1] = String.valueOf(location.getLongitude());
                                        double[] start = {Double.parseDouble(StartPoint[0]), Double.parseDouble(StartPoint[1])};
                                        router.setLocation(start);
                                        double[] end = {Double.parseDouble(EndPoint[0]), Double.parseDouble(EndPoint[1])};
                                        router.setLocation(end);

                                        router.fetch();
                                        Log.i("RECALCULATE","Recalculate");
                                    }

                                    @Override public void onSnapLocation(ValhallaLocation originalLocation,
                                                                         ValhallaLocation snapLocation) {
                                        //Center map on snapLocation
                                        Log.i("RECALCULATE","Recalculate1");
                                    }

                                    @Override
                                    public void onMilestoneReached(int index, RouteEngine.Milestone milestone) {
                                        Log.i("RECALCULATE","Recalculate2");
                                    }

                                    @Override public void onApproachInstruction(int index) {
                                        //String instruction  = route.getRouteInstructions().get(index).getVerbalPreTransitionInstruction();
                                        //Speak pre transition instruction
                                        Log.i("RECALCULATE","Recalculate3");
                                    }

                                    @Override public void onInstructionComplete(int index) {
                                        //String instruction  = route.getRouteInstructions().get(index).getVerbalPostTransitionInstruction();
                                        //Speak post transition instruction
                                        Log.i("RECALCULATE","Recalculate4");
                                    }

                                    @Override
                                    public void onUpdateDistance(int distanceToNextInstruction, int distanceToDestination) {
                                        //Update trip summary UI to reflect distance away from destination
                                        Log.i("RECALCULATE","Recalculate5");
                                    }

                                    @Override public void onRouteComplete() {
                                        //Show 'you have arrived' UI
                                        Log.i("RECALCULATE","Recalculate6");
                                    }
                                };


                                engine.setListener(routeListener);
                                engine.setRoute(route);


                                TimerTask timerTask = new TimerTask() {
                                    public void run() {

                                        //use a handler to run a toast that shows the current timestamp
                                        handler.post(new Runnable() {
                                            public void run() {
                                                if(myLokalization == true) {
                                                    Log.d("Handlers", "Called on main thread");
                                                    ValhallaLocation valhallaLocation = new ValhallaLocation();
                                                    valhallaLocation.setBearing(mCurrentLocation.getBearing());
                                                    valhallaLocation.setLatitude(mCurrentLocation.getLatitude());
                                                    valhallaLocation.setLongitude(mCurrentLocation.getLongitude());
                                                    engine.onLocationChanged(valhallaLocation);
                                                    map.clearRouteLocationMarker();
                                                    map.drawRouteLocationMarker(new LngLat(mCurrentLocation.getLongitude(),mCurrentLocation.getLatitude()));

                                                }
                                            }
                                        });
                                    }
                                };
                                if(!tasks){
                                    timer.schedule(timerTask, 5000, 10000);
                                }

                            }

                            @Override
                            public void failure(int i) {
                                Log.e("Eror", i + "");
                            }
                        });

                        Log.i("Loc", String.valueOf(myLokalization));
                        //double[] start = {mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()};
                        double[] start = {Double.parseDouble(StartPoint[0]), Double.parseDouble(StartPoint[1])};
                        router.setLocation(start);
                        double[] end = {Double.parseDouble(EndPoint[0]), Double.parseDouble(EndPoint[1])};
                        router.setLocation(end);

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
                dialogfirst(0);
            }
        });
        destination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogfirst(1);
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }
    public int dialogfirst(final int s){
        final CharSequence[] items =  {"Moja lokalizacja", "Prowadzący","Budynki A","Budynki B","Budynki C","Budynki D","Budynki E"
                ,"Budynki F","Budynki H","Budynki L","Budynki M","Budynki P"};
        final int[] number = new int[1];
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Wybiesz źródło wyszukiwania");

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Log.i("SET:  ", (String) items[item]);
                switchSelected(item,s);
                number[0] = item;
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
        return number[0];
    }
    public void switchSelected(int selected,int s){
        listBuildings.clear();
        if(s == 0){
            myLokalization = false;
        }
        listBuildingslessons.put("A-1","5691");
        listBuildingslessons.put("C-3","47688");
        listBuildingslessons.put("C-13","38857");
        switch (selected){
            case 0:
                myLokalizaction();
                myLokalization = true;
                break;
            case 1:
                dialoglisttecher(s);
                break;
            case 2:
                listBuildings.put("A-1","5691");///A-1
                dialoglistbulding(s);
                break;
            case 3:
                //
                break;
            case 4:
                listBuildings.put("C-1","19749");
                //listBuildings.put("C-2","47688");///C-3
                listBuildings.put("C-3","47688");///C-3
                listBuildings.put("C-4","47688");
                listBuildings.put("C-5","121805");
                //listBuildings.put("C-6","47688");
                listBuildings.put("C-7","33261");
                listBuildings.put("C-8","55106");
                listBuildings.put("C-11","124217");
                listBuildings.put("C-13","38857");///C-13
                listBuildings.put("C-14","121118");
                listBuildings.put("C-15","149523");
                listBuildings.put("C-16","3627124");
                //listBuildings.put("C-18","47688");
                dialoglistbulding(s);
                break;
            case 5:
                //
                break;
        }

    }
    public void dialoglisttecher(final int s){

        final CharSequence[] items =  getlisttecher().toArray(new CharSequence[getlisttecher().size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Wybierz Prowadzącego");

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                dialoglisttecherlessons(items[item].toString(),s);

            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public TreeSet <String> getlisttecher(){

            final TreeSet <String> lisTeacher = new TreeSet<>();

            Thread q =  new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL("http://bronn.iiar.pwr.wroc.pl/json.php?prowadzacy");
                        URLConnection conn = url.openConnection();
                        JSONObject json = new JSONObject(IOUtils.toString(conn.getInputStream(), String.valueOf(Charset.forName("UTF-8"))));

                        Log.i("JSON", json.toString());
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        prowadzonce teachers = new prowadzonce();
                        teachers  = gson.fromJson(json.toString(),prowadzonce.class);
                        for(int i = 0;i<teachers.getResponse().length;i++){
                            if(!Objects.equals(teachers.getResponse()[i].getImie_nazwisko().trim(), "null") && !Objects.equals(teachers.getResponse()[i].getImie_nazwisko().trim(), "")){
                                lisTeacher.add(teachers.getResponse()[i].getImie_nazwisko());
                            }

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }

            });
            q.start();
        try {
            q.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return lisTeacher;
    }
    public void dialoglisttecherlessons(final String name, final int s){
        ArrayList<String> listlesson = new ArrayList<>();
        final lessons[] les = getlistlessons(name).getResponse();
        for (int i=0;i<les.length;i++){
            listlesson.add(les[i].getBudynek()+" / "+ les[i].getNumer() +" - "+les[i].getNazwa()+" ["+les[i].getPoczatek()+"] Tyg."+les[i].getParzystosc());
        }

        final CharSequence[] items =  listlesson.toArray(new CharSequence[listlesson.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Wybierz Zajęcia");

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                try {
                    String lesson[] = getlessongps(listBuildingslessons.get(les[item].getBudynek()),les[item].getNumer());
                    if(s == 0){
                        Log.i("SET ZAJECIa ", lesson[0]);
                        Log.i("SET ZAJECIa ", lesson[1]);
                        StartPoint[0]=lesson[0];
                        StartPoint[1]=lesson[1];

                        starting.setText(les[item].getBudynek()+" / "+ les[item].getNumer());
                    }else{
                        Log.i("SET ZAJECIa ", lesson[0]);
                        Log.i("SET ZAJECIa ", lesson[1]);
                        EndPoint[0]= lesson[0];
                        EndPoint[1]= lesson[1];
                        destination.setText(les[item].getBudynek()+" / "+ les[item].getNumer());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    public lessons getlistlessons(final String name){

        final lessons[] zajecia = {new lessons()};

        Thread q =  new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://bronn.iiar.pwr.wroc.pl/json.php?name="+name);
                    URLConnection conn = url.openConnection();
                    JSONObject json = new JSONObject(IOUtils.toString(conn.getInputStream(), String.valueOf(Charset.forName("UTF-8"))));

                    Log.i("JSON", json.toString());
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();

                    zajecia[0] = gson.fromJson(json.toString(),lessons.class);
                    for(int i = 0; i< zajecia[0].getResponse().length; i++){
                             Log.i("JSON Z", zajecia[0].getResponse()[i].getNumer());

                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

        });
        q.start();
        try {
            q.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zajecia[0];
    }
    public String[] getlessongps(final String s,final String r) throws Exception {
        final String[] lisRooms = new String[2];

        Thread q =  new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://overpass-api.de/api/interpreter?data=%5Bout%3Axml%5D%5Btimeout%3A25%5D%3B%28area%5B%22WroclawGIS%3Abuilding%3AID%22%20%3D%20%22"+s+"%22%5D-%3E.a%3Bway%5B%22buildingpart%22%5D%5B%20%22name%22%3D%22"+r+"%22%5D%28area.a%29%3B%29%3Bout%20body%3B%3E%3Bout%20skel%20qt%3B%0A");
                    URLConnection conn = url.openConnection();

                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(conn.getInputStream());
                    Log.i("XML", doc.toString());

                    NodeList nodes = doc.getElementsByTagName("way");
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element element = (Element) nodes.item(i);
                        if (element.getElementsByTagName("tag").item(1) != null) {
                            String tag = element.getElementsByTagName("tag").item(1).getAttributes().getNamedItem("v").getNodeValue();
                            String node = element.getElementsByTagName("nd").item(2).getAttributes().getNamedItem("ref").getNodeValue();
                            String lat = null;
                            String lon = null;

                            NodeList nodesnode = doc.getElementsByTagName("node");
                            for (int j = 0; j < nodesnode.getLength(); j++) {
                                Element elemNode = (Element) nodesnode.item(j);
                                if (Objects.equals(node, elemNode.getAttribute("id"))) {
                                    lat = elemNode.getAttribute("lat");
                                    lon = elemNode.getAttribute("lon");
                                }
                            }
                            lisRooms[0] = lat;
                            lisRooms[1] = lon;
                            // Log.i("Elem", tag +"  node: "+node + " lat  "+ lat +"  "+ lon);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

        });
        q.start();
        q.join();
        return lisRooms;


    }
    public void dialoglistbulding(final int s){
        final CharSequence[] items =  listBuildings.keySet().toArray(new CharSequence[listBuildings.keySet().size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Wybierz budynek");

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Log.i("SET:  ",listBuildings.get(items[item]));
                try {
                    if(s == 0){
                        starting.setText(items[item]);
                    }
                    else {
                        destination.setText(items[item]);
                    }
                    dialogrooms(getrooms(listBuildings.get(items[item])),s);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    public ArrayList<String[]> getrooms(final String s) throws Exception {
        final ArrayList<String[]> lisRooms = new ArrayList<String[]>();

       Thread q =  new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://overpass-api.de/api/interpreter?data=%5Bout%3Axml%5D%5Btimeout%3A25%5D%3B%28area%5B\"WroclawGIS%3Abuilding%3AID\"%20%3D%20\""+s+"\"%5D->.a%3B%2F%2F5691%0Away%5B\"buildingpart\"%5D%28area.a%29%3B%29%3Bout%20body%3B>%3Bout%20skel%20qt%3B");
                    URLConnection conn = url.openConnection();

                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(conn.getInputStream());
                    Log.i("XML", doc.toString());

                    NodeList nodes = doc.getElementsByTagName("way");
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element element = (Element) nodes.item(i);
                        if (element.getElementsByTagName("tag").item(1) != null) {
                            String tag = element.getElementsByTagName("tag").item(1).getAttributes().getNamedItem("v").getNodeValue();
                            String node = element.getElementsByTagName("nd").item(2).getAttributes().getNamedItem("ref").getNodeValue();
                            String lat = null;
                            String lon = null;

                            NodeList nodesnode = doc.getElementsByTagName("node");
                            for (int j = 0; j < nodesnode.getLength(); j++) {
                                Element elemNode = (Element) nodesnode.item(j);
                                if (Objects.equals(node, elemNode.getAttribute("id"))) {
                                    lat = elemNode.getAttribute("lat");
                                    lon = elemNode.getAttribute("lon");
                                }
                            }
                            lisRooms.add(new String[]{tag, lat, lon});
                            // Log.i("Elem", tag +"  node: "+node + " lat  "+ lat +"  "+ lon);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

        });

        q.start();
        q.join();
        if(lisRooms.size()==0){
            Thread w =  new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL("https://overpass-api.de/api/interpreter?data=%5Bout%3Axml%5D%5Btimeout%3A25%5D%3B%0A%28way%5B%22WroclawGIS%3Abuilding%3AID%22%20%3D%20%22"+s+"%22%5D%3B%29%3B%0Aout%20body%3B%3E%3B%0Aout%20skel%20qt%3B%0A");
                        URLConnection conn = url.openConnection();

                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document doc = builder.parse(conn.getInputStream());
                        Log.i("XML", doc.toString());

                        NodeList nodes = doc.getElementsByTagName("way");

                            Element element = (Element) nodes.item(0);
                            if (element.getElementsByTagName("tag").item(1) != null) {
                                String tag = "Do budynku";
                                String node = element.getElementsByTagName("nd").item(2).getAttributes().getNamedItem("ref").getNodeValue();
                                String lat = null;
                                String lon = null;

                                NodeList nodesnode = doc.getElementsByTagName("node");
                                for (int j = 0; j < nodesnode.getLength(); j++) {
                                    Element elemNode = (Element) nodesnode.item(j);
                                    if (Objects.equals(node, elemNode.getAttribute("id"))) {
                                        lat = elemNode.getAttribute("lat");
                                        lon = elemNode.getAttribute("lon");
                                    }
                                }
                                lisRooms.add(new String[]{tag, lat, lon});
                                // Log.i("Elem", tag +"  node: "+node + " lat  "+ lat +"  "+ lon);
                            }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }

            });

            w.start();
            w.join();
        }


        return lisRooms;


    }
public void dialogrooms(final ArrayList<String[]> ListRooms, final int s){
                String[] rooms = new String[ListRooms.size()];
            for (int i=0;i<ListRooms.size();i++) {
                rooms[i] = ListRooms.get(i)[0];
            }
            final CharSequence[] items = rooms;

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Wybierz sale");

            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    String Start =  ListRooms.get(item)[1];
                    String End =  ListRooms.get(item)[2];
                    Log.i("SET:  ", Start);
                    Log.i("SET:  ", End);
                    if(s == 0){
                        StartPoint[0]=Start;
                        StartPoint[1]=End;

                        starting.setText(starting.getText()+" / "+ ListRooms.get(item)[0]);
                    }else{
                        EndPoint[0]= Start;
                        EndPoint[1]= End;
                        destination.setText(destination.getText()+" / "+ ListRooms.get(item)[0]);
                    }

                }
            });
            AlertDialog alert = builder.create();
            alert.show();

}
public void myLokalizaction(){
    StartPoint[0] = String.valueOf(mCurrentLocation.getLatitude());
    StartPoint[1] = String.valueOf(mCurrentLocation.getLongitude());
    starting.setText("Moja Lokalizacja");
}
    private ArrayList<LngLat> decodePoly(String encoded) {
        Log.i("Location", "String received: " + encoded);
        ArrayList<LngLat> poly = new ArrayList<LngLat>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            //LatLng p = new LatLng((((double) lat / 1E6)),(((double) lng / 1E6)));
            LngLat p = new LngLat((((double) lng / 1E6)), (((double) lat / 1E6)));
            poly.add(p);
        }

        for (int i = 0; i < poly.size(); i++) {
            Log.i("Location", "Point sent: Latitude: " + poly.get(i).latitude + " Longitude: " + poly.get(i).longitude);
        }
        return poly;
    }


    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);

            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);

            }

        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
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

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Handles the Start Updates button and requests start of location updates. Does nothing if
     * updates have already been requested.
     */
    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;

            startLocationUpdates();
        }
    }

    /**
     * Handles the Stop Updates button, and requests removal of location updates. Does nothing if
     * updates were not previously requested.
     */
    public void stopUpdatesButtonHandler(View view) {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            stopLocationUpdates();
        }
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }


    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        mGoogleApiClient.connect();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.

        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();

        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        Log.i("YeS:", "OK");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
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


}
