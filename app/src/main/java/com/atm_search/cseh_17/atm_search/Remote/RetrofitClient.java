package com.atm_search.cseh_17.atm_search.Remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by cseh_17 on 25.03.2018.
 */

public class RetrofitClient {

    private static Retrofit retrofit = null;
    public static Retrofit getClient(String baseUrl){
        if (retrofit == null){
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
