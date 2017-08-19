package com.pwr.routing;

import java.util.HashMap;
import java.util.Map;

public class ListBuildings {
    public Map<String,String> get(String building) {
        Map<String,String> listBuildings = new HashMap<>();

        switch (building) {
            case "A":
                listBuildings.put("A-1","5691");
                listBuildings.put("A-2","41274");
                listBuildings.put("A-3","101446");
                listBuildings.put("A-4","10518");
                listBuildings.put("A-5","60440");
                listBuildings.put("A-6","133272");
                listBuildings.put("A-7","73021");
                listBuildings.put("A-8","145121");
                listBuildings.put("A-9","101446");
                listBuildings.put("A-10","73335");
                listBuildings.put("A-11","20547");
                break;
            case "B":
                listBuildings.put("B-1","155619");
                listBuildings.put("B-2","145397");
                listBuildings.put("B-3","103691");
                listBuildings.put("B-4","385");
                listBuildings.put("B-9","54164");
                listBuildings.put("B-11","69193");
                break;
            case "C":
                listBuildings.put("C-1","19749");
                listBuildings.put("C-2","90455");
                listBuildings.put("C-3","47688");
                listBuildings.put("C-4","47688");
                listBuildings.put("C-5","121805;91517;160025;160028;160029");
                listBuildings.put("C-6","32411;74248");
                listBuildings.put("C-7","33261");
                listBuildings.put("C-8","55106");
                listBuildings.put("C-11","124217");
                listBuildings.put("C-13","38857");
                listBuildings.put("C-14","121118");
                listBuildings.put("C-15","149523");
                listBuildings.put("C-16","3627124");
                listBuildings.put("C-18 (SKS)","242422");
                break;
            case "D":
                listBuildings.put("D-1","44418;15762;125311");
                listBuildings.put("D-2","66532");
                listBuildings.put("D-3","95293");
                listBuildings.put("D-21","5352352");
                listBuildings.put("D-20","138233");
                break;
            case "E":
                listBuildings.put("E-1","54629");
                listBuildings.put("E-3","81533");
                listBuildings.put("E-5","13929");
                break;
            case "H":
                listBuildings.put("H-4","25210");
                listBuildings.put("H-3","87399");
                listBuildings.put("H-6","104748");
                listBuildings.put("H-5","107066");
                listBuildings.put("H-14","3408");
                break;
            case "L":
                listBuildings.put("L-1","9492942");
                break;
            case "T":
                listBuildings.put("T-4","136260");
                listBuildings.put("T-7","22208");
                listBuildings.put("T-14","27758");
                listBuildings.put("T-2","26227");
                listBuildings.put("T-3","56423");
                listBuildings.put("T-15","27200");
                listBuildings.put("T-16","78138");
                listBuildings.put("T-17","8031");
                listBuildings.put("T-19","87947");
                listBuildings.put("T-22","21300");
                break;
            case "M":
                listBuildings.put("M-3","58293");
                listBuildings.put("M-4","52440");
                listBuildings.put("M-6BIS","64811");
                listBuildings.put("M-11","523523523");
                break;
            case "P":
                listBuildings.put("P-2","103779");
                listBuildings.put("P-4","27404");
                listBuildings.put("P-20","90646");
                listBuildings.put("P-14","78553");
                listBuildings.put("P-18","26600");
                break;
            case "F":
                listBuildings.put("F-1","47378");
                listBuildings.put("F-2","2749124");
                listBuildings.put("F-3","125339");
                listBuildings.put("F-4","18105");
                listBuildings.put("F-6","26446");
                break;
        }

        return listBuildings;
    }
}
