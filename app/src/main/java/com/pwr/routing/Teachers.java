package com.pwr.routing;

import android.content.Context;
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

import static com.google.android.gms.internal.zzagz.runOnUiThread;

public class Teachers {
    Context context;
    public Teachers(Context ctx){
        context = ctx;
    }

    public TreeSet<String> getlistTecher() {

        final TreeSet<String> lisTeacher = new TreeSet<>();

        Thread q = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://prowadzacy.eka.pwr.edu.pl/json.php?prowadzacy");
                    URLConnection conn = url.openConnection();
                    conn.setConnectTimeout(3000);
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
                    notAnswer();
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
    private void notAnswer(){
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                MDToast mdToast = MDToast.makeText(context, "Nie ma polączenia z serwerem", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR);
                mdToast.setGravity(Gravity.BOTTOM,0,400);
                mdToast.show();
            }
        });
    }
}
