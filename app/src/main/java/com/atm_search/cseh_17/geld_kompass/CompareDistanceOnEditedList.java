package com.atm_search.cseh_17.geld_kompass;

import com.atm_search.cseh_17.geld_kompass.ModelOSM.Elements;

import java.util.Comparator;

public class CompareDistanceOnEditedList implements Comparator<Elements>{

    @Override
    public int compare(Elements atmdas1, Elements atmdas2) {
        Double comp1, comp2;
        comp1 = atmdas1.getDistance();
        comp2 = atmdas2.getDistance();
        return comp1.compareTo(comp2);
    }

}

