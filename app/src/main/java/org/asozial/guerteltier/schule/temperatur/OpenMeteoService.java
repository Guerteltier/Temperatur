package org.asozial.guerteltier.schule.temperatur;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenMeteoService {
    @GET("forecast?current=apparent_temperature&daily=apparent_temperature_max,apparent_temperature_min&forecast_days=1")
    Call<OpenMeteoForecast> getForecast(@Query("latitude") double latitude, @Query("longitude") double longitude);
}
