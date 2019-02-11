package de.atm_search.cseh_17.geld_kompass;

class CheckIfNearGrossstadt {
    static boolean checkIfNearGrossstadt(double latitude, double longitude){

        boolean toReturn = false;

        // Check if near Berlin
        if (Distance.distance1(52.524840, latitude, 13.409519, longitude) < 30000 ||
                // Check if near Hamburg
                Distance.distance1(53.558173, latitude, 10.009519, longitude) < 20000 ||
                // Check if near München
                Distance.distance1(48.133333, longitude, 11.583333, longitude) < 25000 ||
                // Check if near Köln
                Distance.distance1(50.933333, latitude, 6.950000, longitude) < 20000 ||
                // Check if near Frankfurt
                Distance.distance1(50.116666, latitude, 8.683333, longitude) < 25000 ||
                // Check if near Stuttgart
                Distance.distance1(48.783333, latitude, 9.183333, longitude) < 20000 ||
                // Check if near Düsseldorf
                Distance.distance1(51.233333, latitude, 6.783333, longitude) < 15000 ||
                // Check if near Dortmund
                Distance.distance1(51.516666, latitude, 7.466666, longitude) < 10000 ||
                // Check if near Essen
                Distance.distance1(51.410000, latitude, 7.016666, longitude) < 10000 ||
                // Check if near Leipzig
                Distance.distance1(51.333333, latitude, 12.366666, longitude) < 20000 ||
                // Check if near Bremen
                Distance.distance1(53.083333, latitude, 8.800000, longitude) < 25000 ||
                // Check if near Dresden
                Distance.distance1(51.050000, latitude, 13.733333, longitude) < 20000 ||
                // Check if near Hannover
                Distance.distance1(52.366666, latitude, 9.733333, longitude) < 25000 ||
                // Check if near Nürnberg
                Distance.distance1(49.450000, latitude, 11.083333, longitude) < 25000 ||
                // Check if near Duisburg
                Distance.distance1(51.433333, latitude, 6.766666, longitude) < 13000 ||
                // Check if near Bochum
                Distance.distance1(51.483333, latitude, 7.216666, longitude) < 10000 ||
                // Check if near Wuppertal
                Distance.distance1(51.266666, latitude, 7.216666, longitude) < 20000 ||
                // Check if near Bielefeld
                Distance.distance1(52.016666, latitude, 8.533333, longitude) < 10000 ||
                // Check if near Bonn
                Distance.distance1(50.733333, latitude, 7.100000, longitude) < 13000 ||
                // Check if near Münster
                Distance.distance1(51.966666, latitude, 7.633333, longitude) < 10000){
            toReturn = true;
        }

        return toReturn;
    }
}
