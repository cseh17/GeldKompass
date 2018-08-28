package com.atm_search.cseh_17.geld_kompass;

public class CheckIfVolksbank {

    public static boolean checkIfVolksbank(String toCheck){

        return toCheck.toLowerCase().contains("volks")
                || (toCheck.toLowerCase().contains("aachener"))
                || (toCheck.toLowerCase().contains("bopfing"))
                || (toCheck.toLowerCase().contains("brühl"))
                || (toCheck.toLowerCase().contains("donau"))
                || (toCheck.toLowerCase().contains("erfurter"))
                || (toCheck.toLowerCase().contains("federsee bank"))
                || (toCheck.toLowerCase().contains("frankenberger bank"))
                || (toCheck.toLowerCase().contains("geno"))
                || (toCheck.toLowerCase().contains("genossenschafts bank münchen"))
                || (toCheck.toLowerCase().contains("gls"))
                || (toCheck.toLowerCase().contains("unterlegäu"))
                || (toCheck.toLowerCase().contains("kölner"))
                || (toCheck.toLowerCase().contains("ievo"))
                || (toCheck.toLowerCase().contains("liga"))
                || (toCheck.toLowerCase().contains("märki"))
                || (toCheck.toLowerCase().contains("münchener bank"))
                || (toCheck.toLowerCase().contains("raiffeisen"))
                || (toCheck.toLowerCase().contains("rv"))
                || (toCheck.toLowerCase().contains("darlehenkasse"))
                || (toCheck.toLowerCase().contains("spaar & kredit"))
                || (toCheck.toLowerCase().contains("spaar&kredit"))
                || (toCheck.toLowerCase().contains("spreewald"))
                || (toCheck.toLowerCase().contains("vr"))
                || (toCheck.toLowerCase().contains("waldecker"))
                || (toCheck.toLowerCase().contains("team"));
    }
}
