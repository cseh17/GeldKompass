package com.atm_search.cseh_17.geld_kompass;

import com.atm_search.cseh_17.geld_kompass.Remote.GoogleAPIService;
import com.atm_search.cseh_17.geld_kompass.Remote.RetrofitClient;

/**
 * Created by cseh_17 on 25.03.2018.
 */

public class Common {

    private static final String GOOGLE_API_URL = "https://maps.googleapis.com/";
    public static GoogleAPIService getGooglePIService(){
        return RetrofitClient.getClient(GOOGLE_API_URL).create(GoogleAPIService.class);
    }
}
