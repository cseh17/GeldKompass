package com.atm_search.cseh_17.geld_kompass;

import java.util.Comparator;

public class CompareDistancesOnDisplayList implements Comparator<RVRowInformation> {

    @Override
    public int compare(RVRowInformation rvri1, RVRowInformation rvri2) {
        Double comp1, comp2;
        comp1 = Double.parseDouble(rvri1.rowSubtitle);
        comp2 = Double.parseDouble(rvri2.rowSubtitle);
        return comp1.compareTo(comp2);
    }
}
