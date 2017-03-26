package com.pwr.routing;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DialogWindows {
    private MainActivity m;
    private Teachers t = new Teachers();
    private Lessons l = new Lessons();
    Context context;
    Map<String,String> listBuildings = new HashMap<>();
    Map<String,String> listBuildingsLessons = new HashMap<>();

    public DialogWindows(Context ctx,MainActivity M){
         context = ctx;
         m = M;
    }

     public int dialogFirst(final int s){
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

    public void dialoglistTecher(final int s){

        final CharSequence[] items =  t.getlistTecher().toArray(new CharSequence[t.getlistTecher().size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = new View(context).findViewById(R.id.dialog_layout);

        builder.setView(view);
        builder.setTitle("Wybierz Prowadzącego");

            builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                dialoglisttecherlessons(items[item].toString(),s);

            }
        });
        AlertDialog alert = builder.create();
        alert.show();
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
                        m.setStarting(String.valueOf(items[item]));
                    }
                    else {
                        m.setDestination(String.valueOf(items[item]));
                    }
                    dialogRooms(l.getrooms(listBuildings.get(items[item])),s);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    public void dialoglisttecherlessons(final String name, final int s) {
        ArrayList<String> listlesson = new ArrayList<>();
        final LessonsDao[] les = l.getlistlessons(name).getResponse();
        for (int i = 0; i < les.length; i++) {
            listlesson.add(les[i].getBudynek() + " / " + les[i].getNumer() + " - " + les[i].getNazwa() + " [" + les[i].getPoczatek() + "] Tyg." + les[i].getParzystosc());
        }

        final CharSequence[] items = listlesson.toArray(new CharSequence[listlesson.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Wybierz Zajęcia");

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                try {
                    String lesson[] = l.getlessonGps(listBuildingsLessons.get(les[item].getBudynek()), les[item].getNumer());
                    if (s == 0) {
                        Log.i("SET ZAJECIa ", lesson[0]);
                        Log.i("SET ZAJECIa ", lesson[1]);
                        m.setStartPoint(lesson[0],lesson[1]);
                        m.setStarting(les[item].getBudynek() + " / " + les[item].getNumer());
                    } else {
                        Log.i("SET ZAJECIa ", lesson[0]);
                        Log.i("SET ZAJECIa ", lesson[1]);
                        m.setEndPoint(lesson[0],lesson[1]);
                        m.setDestination(les[item].getBudynek() + " / " + les[item].getNumer());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    public void dialogRooms(final ArrayList<String[]> ListRooms, final int s) {
        String[] rooms = new String[ListRooms.size()];
        for (int i = 0; i < ListRooms.size(); i++) {
            rooms[i] = ListRooms.get(i)[0];
        }
        final CharSequence[] items = rooms;

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Wybierz sale");

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                String Start = ListRooms.get(item)[1];
                String End = ListRooms.get(item)[2];
                Log.i("SET:  ", Start);
                Log.i("SET:  ", End);
                if (s == 0) {
                    m.setStartPoint(Start,End);
                    m.setStarting(m.getStarting() + " / " + ListRooms.get(item)[0]);
                } else {
                    m.setEndPoint(Start,End);
                    m.setDestination(m.getDestination() + " / " + ListRooms.get(item)[0]);
                }

            }
        });
        AlertDialog alert = builder.create();
        alert.show();

    }
    public void switchSelected(int selected,int s){
        listBuildings.clear();
        if(s == 0){
            //m.setlokalization(false);
        }
        listBuildingsLessons.put("A-1","5691");
        listBuildingsLessons.put("C-3","47688");
        listBuildingsLessons.put("C-13","38857");
        switch (selected){
            case 0:
                m.myLokalizaction();
                m.setlokalization(true);
                break;
            case 1:
                dialoglistTecher(s);
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
}
