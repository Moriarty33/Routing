package com.pwr.routing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.Gravity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.valdesekamdem.library.mdtoast.MDToast;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.net.URLConnection;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import timber.log.Timber;


public class Lessons {
    private Context context;

    Lessons(Context ctx){
        context = ctx;
    }

    public ArrayList<String[]> getrooms(final String s) throws Exception {
        final ArrayList<String[]> lisRooms = new ArrayList<>();

        Thread q = new Thread(() -> {
            try {
                URL url = new URL("https://overpass-api.de/api/interpreter?data=%5Bout%3Axml%5D%5Btimeout%3A25%5D%3B%28area%5B\"WroclawGIS%3Abuilding%3AID\"%20%3D%20\"" + s + "\"%5D->.a%3B%2F%2F5691%0Away%5B\"buildingpart\"%5D%28area.a%29%3B%29%3Bout%20body%3B>%3Bout%20skel%20qt%3B");
                URLConnection conn = url.openConnection();

                conn.setConnectTimeout(3000);

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(conn.getInputStream());
                Timber.tag("XML").i(doc.toString());

                NodeList nodes = doc.getElementsByTagName("way");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element element = (Element) nodes.item(i);
                    if (element.getElementsByTagName("tag").item(1) != null) {
                        String tag = "";
                        for(int n=0; n< element.getElementsByTagName("tag").getLength();n++){
                            if(!Objects.equals(element.getElementsByTagName("tag").item(n).getAttributes().getNamedItem("v").getNodeValue(), "room")){
                                tag = element.getElementsByTagName("tag").item(n).getAttributes().getNamedItem("v").getNodeValue();
                            }
                        }

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
                notAnswer();
                e.printStackTrace();
            }


        });

        q.start();
        q.join();
        if (lisRooms.size() == 0) {
            Thread w = new Thread(() -> {
                try {
                    URL url = new URL("https://overpass-api.de/api/interpreter?data=%5Bout%3Axml%5D%5Btimeout%3A25%5D%3B%0A%28way%5B%22WroclawGIS%3Abuilding%3AID%22%20%3D%20%22" + s + "%22%5D%3B%29%3B%0Aout%20body%3B%3E%3B%0Aout%20skel%20qt%3B%0A");
                    URLConnection conn = url.openConnection();
                    conn.setConnectTimeout(3000);

                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(conn.getInputStream());
                    Timber.tag("XML").i(doc.toString());

                    NodeList nodes = doc.getElementsByTagName("way");

                    Element element = (Element) nodes.item(0);
                    if (element.getElementsByTagName("tag").item(1) != null) {
                        String tag = "Budynek";
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
                    notAnswer();
                    e.printStackTrace();
                }


            });

            w.start();
            w.join();
        }


        return lisRooms;


    }


    public LessonsDao getlistlessons(final String name) {

        final LessonsDao[] zajecia = {new LessonsDao()};

        Thread q = new Thread(() -> {
            try {
                URL url = new URL("https://prowadzacy.eka.pwr.edu.pl/json.php?name=" + name.replaceAll(" ","%20"));
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(3000);
                JSONObject json = new JSONObject(IOUtils.toString(conn.getInputStream(), "UTF-8"));

                Timber.tag("JSON").i(json.toString());
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                zajecia[0] = gson.fromJson(json.toString(), LessonsDao.class);
                for (int i = 0; i < zajecia[0].getResponse().length; i++) {
                    Timber.tag("JSON Z").i(zajecia[0].getResponse()[i].getNumer());

                }


            } catch (Exception e) {
                notAnswer();
                e.printStackTrace();
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

    public String[] getlessonGps(final String s, final String r) throws Exception {
        final String[] lisRooms = new String[2];

        Thread q = new Thread(() -> {
            try {
                URL url = new URL("https://overpass-api.de/api/interpreter?data=%5Bout%3Axml%5D%5Btimeout%3A25%5D%3B%28area%5B%22WroclawGIS%3Abuilding%3AID%22%20%3D%20%22" + s + "%22%5D-%3E.a%3Bway%5B%22buildingpart%22%5D%5B%20%22name%22%3D%22" + r + "%22%5D%28area.a%29%3B%29%3Bout%20body%3B%3E%3Bout%20skel%20qt%3B%0A");
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(4000);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(conn.getInputStream());
                Timber.tag("XML").i(doc.toString());

                NodeList nodes = doc.getElementsByTagName("way");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element element = (Element) nodes.item(i);
                    if (element.getElementsByTagName("tag").item(1) != null) {
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
                notAnswer();
                e.printStackTrace();
            }


        });
        q.start();
        q.join();
        return lisRooms;
    }

    private void notAnswer(){
        ((Activity) context).runOnUiThread(() -> {
            MDToast mdToast = MDToast.makeText(context, "Nie ma polączenia z serwerem", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR);
            mdToast.setGravity(Gravity.BOTTOM,0,400);
            mdToast.show();
        });
    }

}
