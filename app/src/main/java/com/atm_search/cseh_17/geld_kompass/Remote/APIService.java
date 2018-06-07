package com.atm_search.cseh_17.geld_kompass.Remote;

import com.atm_search.cseh_17.geld_kompass.Model.MyAtms;
import com.atm_search.cseh_17.geld_kompass.ModelOSM.MyOsmAtms;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by cseh_17 on 25.03.2018.
 */

public interface APIService {

    @GET
    Call<MyAtms> getNearByPoi(@Url String url);

    @GET
    Call<MyOsmAtms> getNearByBank(@Url String url);

    @GET
    Call<MyOsmAtms> getNearByAtm(@Url String url);
}