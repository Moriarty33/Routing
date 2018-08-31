package com.pwr.routing;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.valdesekamdem.library.mdtoast.MDToast;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.TreeSet;

import timber.log.Timber;


public class Teachers {
    private Context context;
    Teachers(Context ctx){
        context = ctx;
    }

    public TreeSet<String> getlistTecher() {

        final TreeSet<String> lisTeacher = new TreeSet<>();

        Thread q = new Thread(() -> {
            try {
                URL url = new URL("https://prowadzacy.eka.pwr.edu.pl/json.php?prowadzacy");
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(3000);
                JSONObject json = new JSONObject(IOUtils.toString(conn.getInputStream(), "UTF-8"));

                Timber.tag("JSON").i(json.toString());
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
                notAnswer();
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
    private void notAnswer(){
        ((Activity) context).runOnUiThread(() -> {
            MDToast mdToast = MDToast.makeText(context, context.getString(R.string.connect_server_error), MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR);
            mdToast.setGravity(Gravity.BOTTOM,0,400);
            mdToast.show();
        });
    }
}
