package com.pwr.routing;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Created by jamin on 26.03.2017.
 */

public class Teachers {
    public TreeSet<String> getlistTecher() {

        final TreeSet<String> lisTeacher = new TreeSet<>();

        Thread q = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://bronn.iiar.pwr.wroc.pl/json.php?prowadzacy");
                    URLConnection conn = url.openConnection();
                    JSONObject json = new JSONObject(IOUtils.toString(conn.getInputStream(), String.valueOf(Charset.forName("UTF-8"))));

                    Log.i("JSON", json.toString());
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    TeacherDao teachers = new TeacherDao();
                    teachers = gson.fromJson(json.toString(), TeacherDao.class);
                    for (int i = 0; i < teachers.getResponse().length; i++) {
                        if (!Objects.equals(teachers.getResponse()[i].getImie_nazwisko().trim(), "null") && !Objects.equals(teachers.getResponse()[i].getImie_nazwisko().trim(), "")) {
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
}
