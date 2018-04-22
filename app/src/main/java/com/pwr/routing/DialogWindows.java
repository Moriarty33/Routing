package com.pwr.routing;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.firebase.crash.FirebaseCrash;
import com.valdesekamdem.library.mdtoast.MDToast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class DialogWindows {
    private MainActivity m;
    private Teachers t;
    private Lessons l;
    private Context context;
    private Map<String, String> listBuildings = new HashMap<>();
    private Map<String, String> listBuildingsLessons = new HashMap<>();

    public DialogWindows(Context ctx, MainActivity M) {
        context = ctx;
        m = M;
        l = new Lessons(ctx);
        t = new Teachers(ctx);
    }

    public int dialogFirst(final int s) {
        final CharSequence[] items = {"Moja lokalizacja", "Prowadzący", "Budynki A", "Budynki B", "Budynki C", "Budynki D", "Budynki E"
                , "Budynki H", "Budynki L", "Akademiki T", "Budynki M", "Budynki P", "Budynki F"};
        final int[] number = new int[1];
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Wybiesz źródło wyszukiwania");

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Log.i("SET:  ", (String) items[item]);
                switchSelected(item, s);
                number[0] = item;
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
        return number[0];
    }

    public void dialogListTeachers(final int s) {
        TreeSet<String> list = t.getlistTecher();
        if(list.size() == 0){
            return;
        }
        final String[] items = list.toArray(new String[list.size()]);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (context, android.R.layout.simple_dropdown_item_1line, items);


        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        final View dialogView = inflater.inflate(R.layout.dialog_teacher, null);
        dialogBuilder.setView(dialogView);

        final AutoCompleteTextView textView = (AutoCompleteTextView) dialogView.findViewById(R.id.input);
        textView.setAdapter(adapter);

        dialogBuilder.setTitle("Wybór prowadzącego");
        dialogBuilder.setMessage("Wpisz nazwisko i imię prowadzącego");

        final AlertDialog b = dialogBuilder.create();
        b.show();
        textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @TargetApi(Build.VERSION_CODES.CUPCAKE)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("INPUT", adapter.getItem(position).toString());
                b.hide();
                InputMethodManager imm = (InputMethodManager) m.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                dialogListTeacherLessons(adapter.getItem(position).toString(), s);
            }
        });

    }

    public void dialogListBuilding(final int s) {
        final CharSequence[] items = listBuildings.keySet().toArray(new CharSequence[listBuildings.keySet().size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Wybierz budynek");
        m.showLoading();
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
                Log.i("SET:  ", listBuildings.get(items[item]));
                try {
                    if (s == 0) {
                        m.setStarting(String.valueOf(items[item]));
                    } else {
                        m.setDestination(String.valueOf(items[item]));
                    }
                    dialogRooms(l.getrooms(listBuildings.get(items[item])), s);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                MDToast mdToast = MDToast.makeText(context, "Żadny budynek nie został wybrany", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR);
                mdToast.setGravity(Gravity.BOTTOM,0,400);
                mdToast.show();
                m.hideLoading();
            }

        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void dialogListTeacherLessons(final String name, final int s) {
        ArrayList<String> listlesson = new ArrayList<>();
        final LessonsDao[] les = l.getlistlessons(name).getResponse();
        for (int i = 0; i < les.length; i++) {
            listlesson.add(les[i].getBudynek() + " / " + les[i].getNumer() + " - " + les[i].getNazwa() + " [" + les[i].getPoczatek() + "] Tyg." + les[i].getParzystosc());
        }

        final CharSequence[] items = listlesson.toArray(new CharSequence[listlesson.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        if (listlesson.size() == 0) {
            MDToast mdToast = MDToast.makeText(context, "Niestety w systemie niema zajęć dla " + name, MDToast.LENGTH_LONG, MDToast.TYPE_ERROR);
            mdToast.setGravity(Gravity.BOTTOM,0,400);
            mdToast.show();
        } else {
            builder.setTitle("Wybierz Zajęcia");
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                try {
                    String lesson[] = l.getlessonGps(listBuildingsLessons.get(les[item].getBudynek()), les[item].getNumer().toLowerCase());
                    if(lesson[0] == null) {
                        lesson = l.getlessonGps(listBuildingsLessons.get(les[item].getBudynek()), les[item].getNumer());
                    }

                    if(lesson[0] != null) {
                        if (s == 0) {
                            Log.i("SET ZAJECIa ", lesson[0]);
                            Log.i("SET ZAJECIa ", lesson[1]);
                            m.setStartPoint(lesson[0], lesson[1]);
                            m.setStarting(les[item].getBudynek() + " / " + les[item].getNumer());
                        } else {
                            Log.i("SET ZAJECIa ", lesson[0]);
                            Log.i("SET ZAJECIa ", lesson[1]);
                            m.setEndPoint(lesson[0], lesson[1]);
                            m.setDestination(les[item].getBudynek() + " / " + les[item].getNumer());
                        }
                    }else  {
                        MDToast mdToast = MDToast.makeText(context, "Nie udało się wyszukać sali :(\nale już pracuje nad tym!", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR);
                        mdToast.setGravity(Gravity.BOTTOM,0,400);
                        mdToast.show();
                        FirebaseCrash.report(new Exception("Sala not found: " + les[item].getBudynek() + " " + les[item].getNumer()));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        if (listlesson.size() != 0) {
            AlertDialog alert = builder.create();
            alert.show();
        }
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
                    m.setStartPoint(Start, End);
                    m.setStarting(m.getStarting() + " / " + ListRooms.get(item)[0]);
                } else {
                    m.setEndPoint(Start, End);
                    m.setDestination(m.getDestination() + " / " + ListRooms.get(item)[0]);
                }

            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if(s == 0){
                    m.setStarting(null);
                    m.setStartPoint(null,null);
                }
                if(s == 1){
                    m.setDestination(null);
                    m.setEndPoint(null,null);
                }
                MDToast mdToast = MDToast.makeText(context, "Żadna sala nie została wybrana", MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR);
                mdToast.setGravity(Gravity.BOTTOM,0,400);
                mdToast.show();
            }

        });
        m.hideLoading();
        AlertDialog alert = builder.create();
        alert.show();

    }

    public void switchSelected(int selected, int s) {
        listBuildings.clear();
        if (s == 0 && selected != 0) {
            m.setlokalization(false);
        }
        ListBuildings listAvaibleBildings = new ListBuildings();
        listBuildingsLessons.putAll(listAvaibleBildings.get("C"));
        switch (selected) {
            case 0:
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    m.myLokalizaction();
                } else {
                    ActivityCompat.requestPermissions(m, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 255);
                }

                break;
            case 1:
                dialogListTeachers(s);
                break;
            case 2:
                listBuildings.putAll(listAvaibleBildings.get("A"));
                dialogListBuilding(s);
                break;
            case 3:
                listBuildings.putAll(listAvaibleBildings.get("B"));
                dialogListBuilding(s);
                break;
            case 4:
                listBuildings.putAll(listAvaibleBildings.get("C"));
                dialogListBuilding(s);
                break;
            case 5:
                listBuildings.putAll(listAvaibleBildings.get("D"));
                dialogListBuilding(s);
                break;
            case 6:
                listBuildings.putAll(listAvaibleBildings.get("E"));
                dialogListBuilding(s);
                break;
            case 7:
                listBuildings.putAll(listAvaibleBildings.get("H"));
                dialogListBuilding(s);
                break;
            case 8:
                listBuildings.putAll(listAvaibleBildings.get("L"));
                dialogListBuilding(s);
                break;
            case 9:
                listBuildings.putAll(listAvaibleBildings.get("T"));
                dialogListBuilding(s);
                break;
            case 10:
                listBuildings.putAll(listAvaibleBildings.get("M"));
                dialogListBuilding(s);
                break;
            case 11:
                listBuildings.putAll(listAvaibleBildings.get("P"));
                dialogListBuilding(s);
                break;
            case 12:
                listBuildings.putAll(listAvaibleBildings.get("F"));
                dialogListBuilding(s);
                break;
        }

    }
}
