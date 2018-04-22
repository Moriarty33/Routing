package com.pwr.routing;

import android.util.Log;


import java.util.ArrayList;

/**
 * Created by jamin on 26.03.2017.
 */

class LngLat {
    public Double latitude;
    public Double longitude;

    LngLat(Double longitude, Double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;

    }
}

public class DecodePolyline {

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
}
