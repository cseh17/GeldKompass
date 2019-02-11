package de.atm_search.cseh_17.geld_kompass;

class CheckIfGrossstadt {
    static boolean checkIfGrossstadt(String stadt) {

        return stadt.contains("Berlin")
                || stadt.contains("Hamburg")
                || stadt.contains("München")
                || stadt.contains("Köln")
                || stadt.contains("Frankfurt")
                || stadt.contains("Stuttgart")
                || stadt.contains("Düsseldorf")
                || stadt.contains("Dortmund")
                || stadt.contains("Essen")
                || stadt.contains("Leipzig")
                || stadt.contains("Bremen")
                || stadt.contains("Dresden")
                || stadt.contains("Hannover")
                || stadt.contains("Nürnberg")
                || stadt.contains("Duisburg")
                || stadt.contains("Bochum")
                || stadt.contains("Wuppertal")
                || stadt.contains("Bielefeld")
                || stadt.contains("Bonn")
                || stadt.contains("Münster");
    }
}
