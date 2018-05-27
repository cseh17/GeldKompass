package com.atm_search.cseh_17.geld_kompass;

import java.util.Comparator;

public class CompareDistanceOnCacheList implements Comparator<AtmDataStructure>{

    @Override
    public int compare(AtmDataStructure atmdas1, AtmDataStructure atmdas2) {
        Double comp1, comp2;
        comp1 = Double.parseDouble(atmdas1.currentAtm.rowSubtitle);
        comp2 = Double.parseDouble(atmdas2.currentAtm.rowSubtitle);
        return comp1.compareTo(comp2);
    }

}

