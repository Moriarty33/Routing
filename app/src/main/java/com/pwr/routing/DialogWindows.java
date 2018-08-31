package com.pwr.routing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.google.firebase.crash.FirebaseCrash;
import com.valdesekamdem.library.mdtoast.MDToast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import timber.log.Timber;

public class DialogWindows {
    private MainActivity m;
    private Teachers t;
    private Lessons l;
    private Context context;
    private Map<String, String> listBuildings = new HashMap<>();
    private Map<String, String> listBuildingsLessons = new HashMap<>();

    DialogWindows(MainActivity ctx, MainActivity M) {
        context = ctx;
        m = M;
        l = new Lessons(ctx);
        t = new Teachers(ctx);
    }

    public int dialogFirst(final int s) {
        final CharSequence[] items = {m.getString(R.string.my_location), m.getString(R.string.teachers),
                m.getString(R.string.house_name) + "A", m.getString(R.string.house_name) + "B", m.getString(R.string.house_name) + "C",
                m.getString(R.string.house_name) +  "D", m.getString(R.string.house_name) + "E", m.getString(R.string.house_name) + "H",
                m.getString(R.string.house_name) + "L", m.getString(R.string.house_name) + "T", m.getString(R.string.house_name) + "M",
                m.getString(R.string.house_name) + "P", m.getString(R.string.house_name) + "F"};
        final int[] number = new int[1];
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.select_search_source);

        builder.setItems(items, (dialog, item) -> {
            Timber.tag("SET:  ").i((String) items[item]);
            switchSelected(item, s);
            number[0] = item;
        });
        AlertDialog alert = builder.create();
        alert.show();
        return number[0];
    }

    private void dialogListTeachers(final int s) {
        TreeSet<String> list = t.getlistTecher();
        if(list.size() == 0){
            return;
        }
        final String[] items = list.toArray(new String[list.size()]);

        final ArrayAdapter<String> adapter = new ArrayAdapter<>
                (context, android.R.layout.simple_dropdown_item_1line, items);


        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams") final View dialogView = inflater.inflate(R.layout.dialog_teacher, null);
        dialogBuilder.setView(dialogView);

        final AutoCompleteTextView textView = dialogView.findViewById(R.id.input);
        textView.setAdapter(adapter);

        dialogBuilder.setTitle(R.string.select_teacher);
        dialogBuilder.setMessage(R.string.select_name_and_surname_teacher);

        final AlertDialog b = dialogBuilder.create();
        b.show();
        textView.setOnItemClickListener((parent, view, position, id) -> {
            Timber.tag("INPUT").i(adapter.getItem(position));
            b.hide();
            InputMethodManager imm = (InputMethodManager) m.getSystemService(Context.INPUT_METHOD_SERVICE);
            Objects.requireNonNull(imm).hideSoftInputFromWindow(textView.getWindowToken(), 0);
            dialogListTeacherLessons(adapter.getItem(position), s);
        });

    }

    private void dialogListBuilding(final int s) {
        final CharSequence[] items = listBuildings.keySet().toArray(new CharSequence[listBuildings.keySet().size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.select_house);
        m.showLoading();
        builder.setItems(items, (dialog, item) -> {
            dialog.dismiss();
            Timber.tag("SET:  ").i(listBuildings.get(items[item]));
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

        }).setOnCancelListener(dialog -> {
            m.message(m.getString(R.string.no_house_selected_error), MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR);
            m.hideLoading();
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void dialogListTeacherLessons(final String name, final int s) {
        ArrayList<String> listlesson = new ArrayList<>();
        final LessonsDao[] les = l.getlistlessons(name).getResponse();
        for (LessonsDao le : les) {
            listlesson.add(le.getBudynek() + " / " + le.getNumer() + " - " + le.getNazwa() + " [" + le.getPoczatek() + "]" + m.getString(R.string.week_short) + le.getParzystosc());
        }

        final CharSequence[] items = listlesson.toArray(new CharSequence[listlesson.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        if (listlesson.size() == 0) {
            m.message(m.getString(R.string.search_teacher_lessons_error) + name, MDToast.LENGTH_LONG, MDToast.TYPE_ERROR);
        } else {
            builder.setTitle(R.string.select_lessons);
        }

        builder.setItems(items, (dialog, item) -> {
            try {
                String lesson[] = l.getlessonGps(listBuildingsLessons.get(les[item].getBudynek()), les[item].getNumer().toLowerCase());
                if(lesson[0] == null) {
                    lesson = l.getlessonGps(listBuildingsLessons.get(les[item].getBudynek()), les[item].getNumer());
                }

                if(lesson[0] != null) {
                    if (s == 0) {
                        Timber.tag("SET ZAJECIa ").i(lesson[0]);
                        Timber.tag("SET ZAJECIa ").i(lesson[1]);
                        m.setStartPoint(lesson[0], lesson[1]);
                        m.setStarting(les[item].getBudynek() + " / " + les[item].getNumer());
                    } else {
                        Timber.tag("SET ZAJECIa ").i(lesson[0]);
                        Timber.tag("SET ZAJECIa ").i(lesson[1]);
                        m.setEndPoint(lesson[0], lesson[1]);
                        m.setDestination(les[item].getBudynek() + " / " + les[item].getNumer());
                    }
                }else  {
                    m.message(m.getString(R.string.search_room_after_select_teacher_error), MDToast.LENGTH_LONG, MDToast.TYPE_ERROR);
                    FirebaseCrash.report(new Exception("Sala not found: " + les[item].getBudynek() + " " + les[item].getNumer()));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        });
        if (listlesson.size() != 0) {
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void dialogRooms(final ArrayList<String[]> ListRooms, final int s) {
        String[] rooms = new String[ListRooms.size()];
        for (int i = 0; i < ListRooms.size(); i++) {
            rooms[i] = ListRooms.get(i)[0];
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.select_room);

        builder.setItems(rooms, (dialog, item) -> {
            String Start = ListRooms.get(item)[1];
            String End = ListRooms.get(item)[2];
            Timber.tag("SET:  ").i(Start);
            Timber.tag("SET:  ").i(End);
            if (s == 0) {
                m.setStartPoint(Start, End);
                m.setStarting(m.getStarting() + " / " + ListRooms.get(item)[0]);
            } else {
                m.setEndPoint(Start, End);
                m.setDestination(m.getDestination() + " / " + ListRooms.get(item)[0]);
            }

        }).setOnCancelListener(dialog -> {
            if(s == 0){
                m.setStarting(null);
                m.setStartPoint(null,null);
            }
            if(s == 1){
                m.setDestination(null);
                m.setEndPoint(null,null);
            }
            m.message(m.getString(R.string.room_not_selected), MDToast.LENGTH_SHORT, MDToast.TYPE_ERROR);
        });
        m.hideLoading();
        AlertDialog alert = builder.create();
        alert.show();

    }

    private void switchSelected(int selected, int s) {
        listBuildings.clear();
        if (s == 0 && selected != 0) {
          m.disableLocation();
          m.setStarting(null);
        }
        ListBuildings listAvaibleBildings = new ListBuildings();
        listBuildingsLessons.putAll(listAvaibleBildings.get("C"));
        switch (selected) {
            case 0:
                m.enableLocation();
                m.setStarting(m.getString(R.string.my_location));
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
