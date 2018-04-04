package com.atm_search.cseh_17.atm_search.Remote;

import com.atm_search.cseh_17.atm_search.Model.MyAtms;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by cseh_17 on 25.03.2018.
 */

public interface GoogleAPIService {

    @GET
    Call<MyAtms> getNearByPoi(@Url String url);
}
