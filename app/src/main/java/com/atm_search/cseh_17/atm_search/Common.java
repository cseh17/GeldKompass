package com.atm_search.cseh_17.atm_search;

import com.atm_search.cseh_17.atm_search.Remote.GoogleAPIService;
import com.atm_search.cseh_17.atm_search.Remote.RetrofitClient;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by cseh_17 on 25.03.2018.
 */

public class Common {

    private static final String GOOGLE_API_URL = "https://maps.googleapis.com/";
    public static GoogleAPIService getGooglePIService(){
        return RetrofitClient.getClient(GOOGLE_API_URL).create(GoogleAPIService.class);
    }
}
