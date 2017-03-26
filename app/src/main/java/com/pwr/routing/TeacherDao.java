package com.pwr.routing;

/**
 * Created by jamin on 18.01.2017.
 */

public class TeacherDao {
    private TeacherDao[] response;
    private String prowadzacy_id;
    private String imie_nazwisko;

    public String getProwadzacy_id() {
        return prowadzacy_id;
    }

    public void setProwadzacy_id(String prowadzacy_id) {
        this.prowadzacy_id = prowadzacy_id;
    }

    public TeacherDao[] getResponse() {
        return response;
    }

    public void setResponse(TeacherDao[] response) {
        this.response = response;
    }

    public String getImie_nazwisko() {
        return imie_nazwisko;
    }

    public void setImie_nazwisko(String imie_nazwisko) {
        this.imie_nazwisko = imie_nazwisko;
    }


}
